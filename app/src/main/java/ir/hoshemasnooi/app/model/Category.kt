package ir.hoshemasnooi.app.model

/**
 * دسته‌بندی موضوعی مطالب هوش مصنوعی.
 * هر دسته شامل کلیدواژه‌هایی است که برای دسته‌بندی خودکار مطالب استفاده می‌شود.
 */
enum class Category(
    val id: String,
    val title: String,
    val emoji: String,
    val keywords: List<String>
) {
    NEWS(
        id = "news",
        title = "اخبار",
        emoji = "\uD83D\uDCF0",
        keywords = listOf(
            "announce", "announced", "launch", "released", "release", "unveil",
            "مدل جدید", "رونمایی", "معرفی", "منتشر", "عرضه", "خبر", "breaking",
            "update", "version", "report"
        )
    ),
    LLM(
        id = "llm",
        title = "مدل‌های زبانی",
        emoji = "\uD83E\uDDE0",
        keywords = listOf(
            "llm", "language model", "gpt", "chatgpt", "claude", "gemini", "llama",
            "mistral", "qwen", "deepseek", "token", "prompt", "context window",
            "مدل زبانی", "چت‌بات", "پاسخ‌گو", "توکن", "پرامپت"
        )
    ),
    IMAGE_VIDEO(
        id = "imagevideo",
        title = "تصویر و ویدیو",
        emoji = "\uD83C\uDFA8",
        keywords = listOf(
            "image", "video", "diffusion", "midjourney", "stable diffusion", "dall",
            "sora", "render", "3d", "generative art", "text-to-image", "animation",
            "تصویر", "ویدیو", "نقاشی", "تولید تصویر", "انیمیشن", "سینمایی"
        )
    ),
    CODE(
        id = "code",
        title = "کدنویسی",
        emoji = "\uD83D\uDCBB",
        keywords = listOf(
            "code", "coding", "programming", "developer", "copilot", "cursor",
            "ide", "api", "sdk", "github", "framework", "python", "software",
            "کدنویسی", "برنامه‌نویسی", "توسعه‌دهنده", "کتابخانه", "نرم‌افزار"
        )
    ),
    RESEARCH(
        id = "research",
        title = "پژوهش و مقالات",
        emoji = "\uD83D\uDD2C",
        keywords = listOf(
            "paper", "research", "arxiv", "study", "benchmark", "sota", "dataset",
            "algorithm", "training", "neural network", "architecture",
            "پژوهش", "مقاله", "تحقیق", "پایان‌نامه", "الگوریتم", "آموزش مدل"
        )
    ),
    TOOLS(
        id = "tools",
        title = "ابزارها و محصولات",
        emoji = "\uD83D\uDEE0\uFE0F",
        keywords = listOf(
            "tool", "app", "product", "plugin", "extension", "feature", "platform",
            "service", "integrat", "workflow", "automation", "agent",
            "ابزار", "اپلیکیشن", "افزونه", "محصول", "سرویس", "اتوماسیون", "عامل"
        )
    ),
    BUSINESS(
        id = "business",
        title = "کسب‌وکار و سرمایه",
        emoji = "\uD83D\uDCB0",
        keywords = listOf(
            "funding", "raises", "investment", "venture", "acquisition", "ipo",
            "revenue", "startup", "valuation", "market", "company", "billion",
            "سرمایه‌گذاری", "استارتاپ", "بودجه", "بازار", "شرکت", "درآمد", "ارز"
        )
    ),
    ETHICS(
        id = "ethics",
        title = "اخلاق و ایمنی",
        emoji = "\u2696\uFE0F",
        keywords = listOf(
            "safety", "ethic", "alignment", "bias", "harm", "regulation", "risk",
            "privacy", "misinformation", "deepfake", "security",
            "اخلاق", "ایمنی", "سوگیری", "حریم خصوصی", "خطر", "دیپ‌فیک", "امنیت"
        )
    ),
    POLICY(
        id = "policy",
        title = "قوانین و سیاست",
        emoji = "\uD83C\uDFDB\uFE0F",
        keywords = listOf(
            "law", "government", "policy", "bill", "eu ai act", "copyright",
            "lawsuit", "court", "ban", "compliance", "senate", "congress",
            "قانون", "دولت", "سیاست", "دادگاه", "شکایت", "ممنوعیت", "مجوز"
        )
    ),
    TUTORIAL(
        id = "tutorial",
        title = "آموزش و راهنما",
        emoji = "\uD83C\uDF93",
        keywords = listOf(
            "how to", "tutorial", "guide", "tips", "learn", "course", "explained",
            "step by step", "beginner", "best practices", "thread",
            "آموزش", "راهنما", "یادگیری", "دوره", "نکته", "گام به گام"
        )
    ),
    OTHER(
        id = "other",
        title = "سایر",
        emoji = "\u2728",
        keywords = emptyList()
    );

    companion object {
        val all: List<Category> = entries.toList()

        fun fromId(id: String?): Category =
            entries.firstOrNull { it.id == id } ?: OTHER
    }
}
