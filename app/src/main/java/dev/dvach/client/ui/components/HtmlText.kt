package dev.dvach.client.ui.components

import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import dev.dvach.client.BuildConfig
import dev.dvach.client.util.decodeHtmlEntities
import dev.dvach.client.util.decodeLinkHref

/**
 * Мини-рендерер HTML из `comment`-поля Makaba.
 *
 * WebView осознанно не используем: он тяжёлый, ломает выделение текста и даёт
 * лишнюю поверхность атаки. Поддерживаем ровно тот поднабор, который встречается
 * в постах: `<br>`, `<b>`, `<i>`, `<u>`, `<s>`, `<a href>`, `<span style="color">`,
 * спойлеры, `<code>`, `<sub>/<sup>` и кликабельные цитаты `>>123456`.
 */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    color: Color = Color.Unspecified,
    /**
     * В списках (превью треда) ссылки должны быть обычным текстом, иначе тап
     * по тексту уводит в браузер вместо открытия треда.
     */
    linksEnabled: Boolean = true,
    /** Клик по цитате: номер поста и тред, в котором он лежит (null — текущий). */
    onPostClick: ((postNum: Int, threadNum: Int?) -> Unit)? = null,
    /**
     * Скрытый текст (спойлеры) сразу раскрыт. Тап по такому спойлеру его
     * прячет — состояние в [revealedSpoilers] при этом работает «наоборот».
     */
    autoRevealSpoilers: Boolean = false,
) {
    var revealedSpoilers by remember(html) { mutableStateOf<Set<Int>>(emptySet()) }
    val scheme = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current

    val annotated: AnnotatedString = remember(
        html,
        revealedSpoilers,
        autoRevealSpoilers,
        scheme.primary,
        color,
        linksEnabled,
        uriHandler,
    ) {
        renderDvachHtml(
            html = html,
            linksEnabled = linksEnabled,
            linkColor = scheme.primary,
            baseColor = color,
            revealedSpoilers = revealedSpoilers,
            autoRevealSpoilers = autoRevealSpoilers,
            spoilerBackground = scheme.onSurface,
            onSpoilerClick = { index ->
                revealedSpoilers = if (revealedSpoilers.contains(index)) {
                    revealedSpoilers - index
                } else {
                    revealedSpoilers + index
                }
            },
            onPostClick = onPostClick,
            onLinkClick = { url ->
                if (BuildConfig.DEBUG) Log.i("Links", "tap: $url")
                runCatching { uriHandler.openUri(url) }
                    .onFailure {
                        if (BuildConfig.DEBUG) Log.w("Links", "Ссылка не открылась: $url", it)
                    }
            },
        )
    }

    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
    )
}

private class Run(
    val text: String,
    val style: SpanStyle,
    val href: String? = null,
    val postRef: Int? = null,
    val postThread: Int? = null,
    val spoilerIndex: Int? = null,
)

private class OpenTag(
    val name: String,
    val style: SpanStyle = SpanStyle(),
    val href: String? = null,
    val spoilerIndex: Int? = null,
    /** Для `<a class="post-reply-link" data-num="...">` — ссылка на пост внутри треда. */
    val postRef: Int? = null,
    val postThread: Int? = null,
)

@Suppress("LongParameterList")
private fun renderDvachHtml(
    html: String,
    linksEnabled: Boolean,
    linkColor: Color,
    baseColor: Color,
    revealedSpoilers: Set<Int>,
    autoRevealSpoilers: Boolean,
    spoilerBackground: Color,
    onSpoilerClick: (Int) -> Unit,
    onPostClick: ((postNum: Int, threadNum: Int?) -> Unit)?,
    onLinkClick: ((String) -> Unit)?,
): AnnotatedString {
    val runs = tokenizeDvachHtml(
        html = html,
        linksEnabled = linksEnabled,
        linkColor = linkColor,
        baseColor = baseColor,
        revealedSpoilers = revealedSpoilers,
        autoRevealSpoilers = autoRevealSpoilers,
        spoilerBackground = spoilerBackground,
    )

    val linkStyles = TextLinkStyles(
        style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
    )
    val quoteStyles = TextLinkStyles(style = SpanStyle(color = linkColor))

    return buildAnnotatedString {
        for (run in runs) {
            val start = length
            withStyle(run.style) { append(run.text) }
            val end = length
            if (end <= start) continue

            val href = run.href
            val postRef = run.postRef
            val postThread = run.postThread
            val spoiler = run.spoilerIndex
            when {
                href != null -> addLink(
                    LinkAnnotation.Url(
                        href,
                        linkStyles,
                        linkInteractionListener = { onLinkClick?.invoke(href) },
                    ),
                    start,
                    end,
                )
                postRef != null && onPostClick != null -> addLink(
                    LinkAnnotation.Clickable(
                        tag = "post:$postRef",
                        styles = quoteStyles,
                        linkInteractionListener = { onPostClick.invoke(postRef, postThread) },
                    ),
                    start,
                    end,
                )
                spoiler != null -> addLink(
                    LinkAnnotation.Clickable(
                        tag = "spoiler:$spoiler",
                        styles = TextLinkStyles(),
                        linkInteractionListener = { onSpoilerClick.invoke(spoiler) },
                    ),
                    start,
                    end,
                )
            }
        }
    }
}

@Suppress("LongParameterList", "CyclomaticComplexMethod")
private fun tokenizeDvachHtml(
    html: String,
    linksEnabled: Boolean,
    linkColor: Color,
    baseColor: Color,
    revealedSpoilers: Set<Int>,
    autoRevealSpoilers: Boolean,
    spoilerBackground: Color,
): List<Run> {
    val runs = mutableListOf<Run>()
    val stack = mutableListOf<OpenTag>()
    var spoilerCounter = 0
    var cursor = 0

    fun mergedStyle(): SpanStyle = stack.fold(SpanStyle()) { acc, tag -> acc.merge(tag.style) }

    fun currentHref(): String? = stack.lastOrNull { it.href != null }?.href

    fun currentSpoiler(): Int? = stack.lastOrNull { it.spoilerIndex != null }?.spoilerIndex

    fun currentRef(): OpenTag? = stack.lastOrNull { it.postRef != null }

    fun appendText(raw: String) {
        val text = decodeHtmlEntities(raw)
        if (text.isEmpty()) return

        // Цвет по умолчанию подмешиваем первым: явные цвета из HTML (и цвет
        // скрытого спойлера) должны его перекрывать.
        val style = if (baseColor == Color.Unspecified) {
            mergedStyle()
        } else {
            SpanStyle(color = baseColor).merge(mergedStyle())
        }
        val href = currentHref()
        val spoiler = currentSpoiler()
        val ref = currentRef()

        if (href != null) {
            // внутри ссылки цитаты не размечаем
            runs += Run(text, style, href = href)
            return
        }
        if (ref != null) {
            // цитата вида <a data-num="123">>>123</a> — ведём на пост внутри приложения
            runs += Run(text, style, postRef = ref.postRef, postThread = ref.postThread, spoilerIndex = spoiler)
            return
        }

        var index = 0
        for (match in QUOTE_REGEX.findAll(text)) {
            if (match.range.first > index) {
                runs += Run(text.substring(index, match.range.first), style, spoilerIndex = spoiler)
            }
            val ref = match.groupValues[1].toIntOrNull()
            runs += Run(match.value, style, postRef = ref, spoilerIndex = spoiler)
            index = match.range.last + 1
        }
        if (index < text.length) {
            runs += Run(text.substring(index), style, spoilerIndex = spoiler)
        }
    }

    while (cursor < html.length) {
        val match = TAG_REGEX.find(html, cursor)
        val textEnd = match?.range?.first ?: html.length
        if (textEnd > cursor) appendText(html.substring(cursor, textEnd))
        if (match == null) break

        val isClosing = match.groupValues[1] == "/"
        val name = match.groupValues[2].lowercase()
        val attrs = match.groupValues[3]
        cursor = match.range.last + 1

        if (isClosing) {
            val index = stack.indexOfLast { it.name == name }
            if (index >= 0) stack.removeAt(index)
            continue
        }

        when (name) {
            "br", "hr" -> runs += Run("\n", mergedStyle())
            "p" -> if (runs.isNotEmpty() && !runs.last().text.endsWith("\n")) {
                runs += Run("\n", mergedStyle())
            }
            "b", "strong" -> stack += OpenTag(name, SpanStyle(fontWeight = FontWeight.Bold))
            "i", "em" -> stack += OpenTag(name, SpanStyle(fontStyle = FontStyle.Italic))
            "u" -> stack += OpenTag(name, SpanStyle(textDecoration = TextDecoration.Underline))
            "s", "strike", "del" -> stack += OpenTag(name, SpanStyle(textDecoration = TextDecoration.LineThrough))
            "code", "pre" -> stack += OpenTag(name, SpanStyle(fontFamily = FontFamily.Monospace))
            "sub" -> stack += OpenTag(name, SpanStyle(baselineShift = BaselineShift.Subscript))
            "sup" -> stack += OpenTag(name, SpanStyle(baselineShift = BaselineShift.Superscript))
            "a" -> {
                val postRef = POST_NUM_ATTR.find(attrs)?.groupValues?.get(1)?.toIntOrNull()
                val postThread = THREAD_NUM_ATTR.find(attrs)?.groupValues?.get(1)?.toIntOrNull()
                val isPostLink = postRef != null && attrs.contains("post-reply", ignoreCase = true)
                val href = decodeLinkHref(HREF_ATTR.find(attrs)?.groupValues?.get(1))
                stack += when {
                    isPostLink -> OpenTag(
                        name = name,
                        style = SpanStyle(color = if (linksEnabled) linkColor else Color.Unspecified),
                        postRef = postRef,
                        postThread = postThread,
                    )
                    else -> OpenTag(
                        name = name,
                        style = SpanStyle(
                            color = if (linksEnabled) linkColor else Color.Unspecified,
                            textDecoration = if (linksEnabled) TextDecoration.Underline else null,
                        ),
                        href = if (linksEnabled) href else null,
                    )
                }
            }
            "span", "font" -> {
                val isSpoiler = attrs.contains("spoiler", ignoreCase = true) ||
                    attrs.contains("color: transparent", ignoreCase = true)
                if (isSpoiler) {
                    val index = spoilerCounter++
                    // По умолчанию спойлер скрыт; при авто-раскрытии tapped index
                    // работает в обратную сторону — тап прячет текст обратно.
                    val revealed = revealedSpoilers.contains(index) != autoRevealSpoilers
                    stack += OpenTag(
                        name = SPOILER_TAG,
                        style = if (revealed) {
                            // Раскрытый спойлер — обычный текст, но с лёгкой
                            // заливкой: видно, что это был скрытый текст, а не
                            // обычная строка.
                            SpanStyle(background = spoilerBackground.copy(alpha = REVEALED_SPOILER_ALPHA))
                        } else {
                            SpanStyle(
                                background = spoilerBackground,
                                color = spoilerBackground,
                            )
                        },
                        spoilerIndex = index,
                    )
                } else {
                    val color = COLOR_ATTR.find(attrs)?.groupValues?.get(1)?.let(::parseColor)
                    if (color != null) stack += OpenTag(name, SpanStyle(color = color))
                }
            }
            "img" -> {
                // смайлы и вложенные картинки в постах: показываем как пометку
                runs += Run(" ", mergedStyle())
            }
        }
    }
    return runs
}

private val TAG_REGEX = Regex("""<\s*(/?)\s*([a-zA-Z][a-zA-Z0-9]*)([^>]*?)/?\s*>""")
private val QUOTE_REGEX = Regex(""">>(\d+)""")
private val COLOR_ATTR = Regex("""color\s*:\s*(rgb\([^)]*\)|#[0-9a-fA-F]{3,8}|[a-zA-Z]+)""")
private val HREF_ATTR = Regex("""href\s*=\s*["']([^"']*)["']""")
private val POST_NUM_ATTR = Regex("""data-num\s*=\s*["'](\d+)["']""")
private val THREAD_NUM_ATTR = Regex("""data-thread\s*=\s*["'](\d+)["']""")
private const val SPOILER_TAG = "__spoiler__"

/**
 * Прозрачность плашки раскрытого спойлера. Сплошная плашка делала текст
 * нечитаемым, а без заливки спойлер было не отличить от обычного текста.
 */
private const val REVEALED_SPOILER_ALPHA = 0.16f

private fun parseColor(value: String): Color? {
    val v = value.trim()
    return runCatching {
        if (v.startsWith("rgb")) {
            val parts = v.substringAfter('(').substringBefore(')').split(',').map { it.trim().toInt() }
            Color(parts[0], parts[1], parts[2])
        } else {
            Color(android.graphics.Color.parseColor(v))
        }
    }.getOrNull()
}


