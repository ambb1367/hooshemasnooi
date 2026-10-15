package ir.hoshemasnooi.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.hoshemasnooi.app.data.FeedRepository
import ir.hoshemasnooi.app.data.RefreshResult
import ir.hoshemasnooi.app.model.Category
import ir.hoshemasnooi.app.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val visiblePosts: List<Post> = emptyList(),
    val selectedCategory: Category? = null,
    val query: String = "",
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val message: String? = null
)

class FeedViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FeedRepository(app)

    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    val settings: FeedRepository get() = repo

    init {
        viewModelScope.launch {
            val posts = repo.initialLoad()
            _state.update { it.copy(posts = posts, loading = false).applied() }
            refresh(silentIfEmpty = posts.isNotEmpty())
        }
    }

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q).applied() }
    }

    fun onCategorySelected(category: Category?) {
        _state.update {
            val next = if (it.selectedCategory == category) null else category
            it.copy(selectedCategory = next).applied()
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun refresh(silentIfEmpty: Boolean = false) {
        if (_state.value.refreshing) return
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }
            val result: RefreshResult = repo.refresh()
            val posts = repo.initialLoad()
            val message = when {
                result.added > 0 -> "این بار $result.added مطلب تازه افزوده شد"
                !result.networkOk -> "اتصال برقرار نشد؛ محتوای ذخیره‌شده نمایش داده می‌شود"
                else -> "مطلب تازه‌ای یافت نشد"
            }
            _state.update {
                it.copy(
                    posts = posts,
                    refreshing = false,
                    message = if (silentIfEmpty && result.added <= 0 && result.networkOk) null else message
                ).applied()
            }
        }
    }

    fun toggleTranslation(enabled: Boolean) {
        repo.translateEnabled = enabled
    }

    fun isTranslationEnabled(): Boolean = repo.translateEnabled

    fun setRemoteUrl(value: String) {
        repo.remoteFeedUrl = value
    }

    fun remoteUrl(): String = repo.remoteFeedUrl

    private fun FeedUiState.applied(): FeedUiState {
        val filtered = posts.filter { post ->
            (selectedCategory == null || post.category == selectedCategory) &&
                (query.isBlank() ||
                    post.displayText.contains(query, ignoreCase = true) ||
                    post.textOriginal.contains(query, ignoreCase = true) ||
                    post.account.contains(query, ignoreCase = true))
        }
        return copy(visiblePosts = filtered)
    }
}
