package ir.hoshemasnooi.app.data

import ir.hoshemasnooi.app.model.Category

/**
 * دسته‌بندی خودکار مطالب بر پایه کلیدواژه‌ها.
 */
object Classifier {

    fun classify(text: String): Category {
        if (text.isBlank()) return Category.OTHER
        val lower = text.lowercase()

        var best: Category = Category.OTHER
        var bestScore = 0

        for (category in Category.all) {
            if (category == Category.OTHER) continue
            var score = 0
            for (keyword in category.keywords) {
                val k = keyword.lowercase()
                var index = lower.indexOf(k)
                while (index >= 0) {
                    score++
                    index = lower.indexOf(k, index + k.length)
                }
            }
            if (score > bestScore) {
                bestScore = score
                best = category
            }
        }
        return best
    }
}
