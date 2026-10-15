#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
هوش‌یار — گردآورنده محتوا
Aggregates AI-related items from public RSS/Atom feeds, classifies them,
translates the text to Persian, and writes docs/feed.json for the Android app.

Standard library only (no third-party dependencies).
"""

import gzip
import html
import io
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCS_DIR = os.path.join(ROOT, "docs")
OUT_PATH = os.path.join(DOCS_DIR, "feed.json")
GLOSSARY_PATH = os.path.join(ROOT, "app", "src", "main", "assets", "glossary.json")

USER_AGENT = "Mozilla/5.0 (compatible; HooshYarBot/1.0; +https://github.com/ambb1367/hooshemasnooi)"
TIMEOUT = 25
MAX_TOTAL = 200
MAX_NEW_TRANSLATIONS = 90
TRANSLATE_DELAY = 0.4

# (source name, url, max items, require_ai_keyword)
FEEDS = [
    ("Google News",
     "https://news.google.com/rss/search?q=artificial+intelligence+OR+AI+model+OR+LLM+OR+chatgpt+OR+agentic+AI&hl=en-US&gl=US&ceid=US:en", 45, False),
    ("Google News",
     "https://news.google.com/rss/search?q=OpenAI+OR+Anthropic+OR+DeepMind+OR+Gemini+OR+LLaMA+OR+HuggingFace&hl=en-US&gl=US&ceid=US:en", 45, False),
    ("Hugging Face", "https://huggingface.co/blog/feed.xml", 15, False),
    ("TechCrunch", "https://techcrunch.com/category/artificial-intelligence/feed/", 15, False),
    ("The Verge", "https://www.theverge.com/rss/ai-artificial-intelligence/index.xml", 15, False),
    ("Google AI", "https://blog.google/technology/ai/rss/", 10, False),
    ("OpenAI", "https://openai.com/news/rss.xml", 15, False),
    ("arXiv", "https://export.arxiv.org/rss/cs.AI", 12, False),
    ("Hacker News", "https://news.ycombinator.com/rss", 25, True),
    # X/educators via Google site:x.com RSS
    ("Andrej Karpathy (X)", "https://news.google.com/rss/search?q=site:x.com+Andrej+Karpathy+AI+OR+LLM&hl=en-US&gl=US&ceid=US:en", 15, False),
    ("Yann LeCun (X)", "https://news.google.com/rss/search?q=site:x.com+Yann+LeCun+AI&hl=en-US&gl=US&ceid=US:en", 10, False),
    ("Andrew Ng (X)", "https://news.google.com/rss/search?q=site:x.com+Andrew+Ng+AI&hl=en-US&gl=US&ceid=US:en", 10, False),
    ("Simon Willison (X)", "https://news.google.com/rss/search?q=site:x.com+simonwillison+AI+OR+LLM&hl=en-US&gl=US&ceid=US:en", 10, False),
    ("Ethan Mollick (X)", "https://news.google.com/rss/search?q=site:x.com+Ethan+Mollick+AI&hl=en-US&gl=US&ceid=US:en", 10, False),
    ("Sebastian Raschka (X)", "https://news.google.com/rss/search?q=site:x.com+Sebastian+Raschka+AI&hl=en-US&gl=US&ceid=US:en", 10, False),
    # RSSHub public instances (fallbacks) to get direct X posts
    ("RSSHub (x)", "https://rsshub.rssforever.com/x/karpathy/statuses", 15, False),
    ("RSSHub (x)", "https://rsshub.app/x/AndrewYNg/statuses", 15, False),
    ("RSSHub (x)", "https://rsshub.app/x/ylecun/statuses", 10, False),
    ("RSSHub (x)", "https://rsshub.app/x/simonw/statuses", 10, False),
    ("RSSHub (x)", "https://rsshub.app/x/emollick/statuses", 10, False),
]

AI_KEYWORDS = [
    "ai", "a.i.", "artificial intelligence", "machine learning", "deep learning",
    "neural", "llm", "gpt", "chatgpt", "claude", "gemini", "llama", "mistral",
    "openai", "anthropic", "deepmind", "hugging face", "transformer", "diffusion",
    "midjourney", "chatbot", "copilot", "model", "inference", "training", "gpu",
    "agent", "open source", "robotics", "autonomous", "nvidia",
    "rag", "reasoning", "fine-tuning", "fine tune", "eval", "token", "embedding",
    "vision", "multimodal", "agentic", "genai", "generative ai",
]


# category id -> keyword list (Persian + English)
CATEGORIES = {
    "news": ["announce", "launch", "released", "release", "unveil", "update", "version",
              "report", "news", "خبر", "رونمایی", "معرفی", "منتشر", "عرضه"],
    "llm": ["llm", "language model", "gpt", "chatgpt", "claude", "gemini", "llama",
            "mistral", "qwen", "deepseek", "token", "prompt", "context window", "model"],
    "imagevideo": ["image", "video", "diffusion", "midjourney", "dall", "sora", "render",
                   "3d", "art", "text-to-image", "animation", "تصویر", "ویدیو"],
    "code": ["code", "coding", "programming", "developer", "copilot", "cursor", "ide",
             "api", "sdk", "github", "framework", "python", "software", "کد"],
    "research": ["paper", "research", "arxiv", "study", "benchmark", "sota", "dataset",
                 "algorithm", "training", "neural network", "architecture", "پژوهش", "مقاله"],
    "tools": ["tool", "app", "product", "plugin", "extension", "feature", "platform",
              "service", "integrat", "workflow", "automation", "agent", "ابزار"],
    "business": ["funding", "raises", "investment", "venture", "acquisition", "ipo",
                 "revenue", "startup", "valuation", "market", "company", "billion",
                 "سرمایه", "استارتاپ", "بازار", "شرکت", "میلیارد"],
    "ethics": ["safety", "ethic", "alignment", "bias", "harm", "risk", "privacy",
               "misinformation", "deepfake", "security", "ایمنی", "اخلاق", "سوگیری"],
    "policy": ["law", "government", "policy", "bill", "ai act", "copyright", "lawsuit",
               "court", "ban", "compliance", "senate", "congress", "قانون", "دادگاه"],
    "tutorial": ["how to", "tutorial", "guide", "tips", "learn", "course", "explained",
                 "step by step", "beginner", "best practices", "آموزش", "راهنما"],
}

TAG_RE = re.compile(r"<[^>]+>")
WS_RE = re.compile(r"\s+")


def log(*a):
    print(*a, file=sys.stderr, flush=True)


def fetch(url):
    req = urllib.request.Request(url, headers={
        "User-Agent": USER_AGENT,
        "Accept": "application/rss+xml, application/atom+xml, application/xml, text/xml, */*",
        "Accept-Language": "en-US,en;q=0.9",
    })
    with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
        raw = resp.read()
    if raw[:2] == b"\x1f\x8b":
        raw = gzip.decompress(raw)
    return raw.decode("utf-8", errors="replace")


def strip_ns(tag):
    return tag.split("}", 1)[-1].lower()


def clean_html(text):
    if not text:
        return ""
    text = re.sub(r"<br\s*/?>", " ", text, flags=re.I)
    text = TAG_RE.sub(" ", text)
    text = html.unescape(text)
    return WS_RE.sub(" ", text).strip()


def parse_date(value):
    if not value:
        return None
    value = value.strip()
    try:
        dt = parsedate_to_datetime(value)
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return int(dt.timestamp() * 1000)
    except Exception:
        pass
    for fmt in ("%Y-%m-%dT%H:%M:%S%z", "%Y-%m-%dT%H:%M:%SZ", "%Y-%m-%dT%H:%M:%S.%f%z",
                "%Y-%m-%d"):
        try:
            v = value.replace("Z", "+0000") if fmt.endswith("%z") and value.endswith("Z") else value
            dt = datetime.strptime(v, fmt)
            if dt.tzinfo is None:
                dt = dt.replace(tzinfo=timezone.utc)
            return int(dt.timestamp() * 1000)
        except Exception:
            continue
    return None


def parse_feed(xml_text, source_name, limit):
    items = []
    try:
        root = ET.fromstring(xml_text.encode("utf-8"))
    except Exception as e:
        log("  ! xml parse error:", e)
        return items

    root_tag = strip_ns(root.tag)
    if root_tag == "feed":  # Atom
        nodes = [c for c in root if strip_ns(c.tag) == "entry"]
    else:  # RSS
        channel = root
        for c in root:
            if strip_ns(c.tag) == "channel":
                channel = c
                break
        nodes = [c for c in channel if strip_ns(c.tag) == "item"]

    for node in nodes:
        title = ""
        link = ""
        summary = ""
        pub = ""
        src = ""
        for child in node:
            name = strip_ns(child.tag)
            text = (child.text or "").strip()
            if name == "title":
                title = clean_html(text)
            elif name == "link":
                if child.get("href"):
                    link = child.get("href").strip()
                elif text:
                    link = text
            elif name in ("description", "summary", "content", "encoded"):
                if not summary:
                    summary = clean_html(text)
            elif name in ("pubdate", "published", "updated", "date"):
                if not pub:
                    pub = text
            elif name == "source":
                src = clean_html(text) or (child.get("url") or "")
        if not title:
            continue
        items.append({
            "title": title,
            "summary": summary,
            "link": link,
            "timestamp": parse_date(pub),
            "author": src or source_name,
            "feed_source": source_name,
        })
        if len(items) >= limit:
            break
    return items


def is_ai_related(text):
    low = " " + text.lower() + " "
    for kw in AI_KEYWORDS:
        if re.search(r"\b" + re.escape(kw) + r"\b", low):
            return True
    return False


def classify(text):
    low = text.lower()
    best = "news"
    best_score = 0
    for cid, kws in CATEGORIES.items():
        score = 0
        for kw in kws:
            score += low.count(kw.lower())
        if score > best_score:
            best_score = score
            best = cid
    return best


def norm_key(text):
    return WS_RE.sub(" ", text.lower()).strip()[:160]


# ---------------- translation ----------------

_glossary = []


def load_glossary():
    global _glossary
    try:
        with open(GLOSSARY_PATH, encoding="utf-8") as f:
            data = json.load(f)
        _glossary = sorted(data.items(), key=lambda kv: -len(kv[0]))
    except Exception as e:
        log("  ! glossary load failed:", e)


def apply_glossary(text):
    out = text
    for en, fa in _glossary:
        if en.lower() in out.lower():
            out = re.sub(re.escape(en), fa, out, flags=re.I)
    return out


def is_persian(text):
    fa = sum(1 for ch in text if "\u0600" <= ch <= "\u06FF")
    la = sum(1 for ch in text if ch.isascii() and ch.isalpha())
    return fa > 0 and fa >= la


def google_translate(text):
    try:
        q = urllib.parse.quote(text[:900])
        url = ("https://translate.googleapis.com/translate_a/single"
               "?client=gtx&sl=auto&tl=fa&dt=t&q=" + q)
        req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
            data = json.loads(resp.read().decode("utf-8", "replace"))
        segments = data[0] if data and isinstance(data, list) else None
        if not segments:
            return None
        out = "".join(seg[0] for seg in segments if seg and seg[0])
        return out.strip() or None
    except Exception:
        return None


def mymemory_translate(text):
    try:
        q = urllib.parse.quote(text[:480])
        url = "https://api.mymemory.translated.net/get?q=%s&langpair=en|fa" % q
        req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
            data = json.loads(resp.read().decode("utf-8", "replace"))
        if data.get("responseStatus") != 200:
            return None
        tr = (data.get("responseData") or {}).get("translatedText") or ""
        if not tr or "MYMEMORY WARNING" in tr.upper():
            return None
        return html.unescape(tr).strip() or None
    except Exception:
        return None


def translate(text, cache, budget):
    key = norm_key(text)
    if key in cache:
        return cache[key], budget
    if is_persian(text):
        cache[key] = text
        return text, budget
    result = None
    if budget[0] > 0:
        result = google_translate(text) or mymemory_translate(text)
        if result:
            budget[0] -= 1
            time.sleep(TRANSLATE_DELAY)
    if not result:
        result = apply_glossary(text)
    cache[key] = result
    return result, budget


# ---------------- main ----------------

def load_existing():
    try:
        with open(OUT_PATH, encoding="utf-8") as f:
            items = json.load(f)
        cache = {}
        for it in items:
            if it.get("textOriginal") and it.get("textPersian"):
                cache[norm_key(it["textOriginal"])] = it["textPersian"]
        return items, cache
    except Exception:
        return [], {}


def main():
    os.makedirs(DOCS_DIR, exist_ok=True)
    load_glossary()
    existing, cache = load_existing()
    log("existing items:", len(existing), "cached translations:", len(cache))

    collected = []
    seen = set()
    for (name, url, limit, require_ai) in FEEDS:
        log("fetching:", name, url)
        try:
            xml_text = fetch(url)
        except Exception as e:
            log("  ! fetch failed:", e)
            continue
        parsed = parse_feed(xml_text, name, limit)
        log("  items:", len(parsed))
        for it in parsed:
            title = it["title"]
            if not title or len(title) < 12:
                continue
            if require_ai and not is_ai_related(title + " " + it["summary"]):
                continue
            key = norm_key(title)
            if key in seen:
                continue
            seen.add(key)
            collected.append(it)

    collected.sort(key=lambda x: x["timestamp"] or 0, reverse=True)
    collected = collected[:MAX_TOTAL]
    log("collected unique:", len(collected))

    budget = [MAX_NEW_TRANSLATIONS]
    out = []
    for i, it in enumerate(collected):
        original = it["title"]
        account = it["author"] or it["feed_source"]
        # Google News titles end with " - Source"; drop the redundant suffix.
        suffix = " - " + account
        if original.endswith(suffix):
            original = original[: -len(suffix)].strip()
        persian, budget = translate(original, cache, budget)
        ts = it["timestamp"] or int(time.time() * 1000)
        out.append({
            "id": str(abs(hash(norm_key(original) + it["link"])) % (10 ** 15)),
            "account": account,
            "authorName": account,
            "category": classify(original + " " + it["summary"]),
            "textOriginal": original,
            "textPersian": persian,
            "timestamp": ts,
            "url": it["link"] or "https://news.google.com/",
            "sourceName": it["feed_source"],
            "translatedFrom": True,
        })

    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, indent=2)
    log("wrote", OUT_PATH, "items:", len(out), "remaining translation budget:", budget[0])

    meta = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "count": len(out),
    }
    with open(os.path.join(DOCS_DIR, "meta.json"), "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)


if __name__ == "__main__":
    main()
