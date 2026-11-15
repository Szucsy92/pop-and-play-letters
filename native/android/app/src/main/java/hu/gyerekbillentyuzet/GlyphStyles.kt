package hu.gyerekbillentyuzet

import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import kotlin.math.max
import kotlin.math.min

/** A karakterek megjelenési stílusa (szülői menü → betűtípus). Az `id` kerül a beállításokba. */
enum class GlyphStyle(val id: String) {
    DEFAULT("default"), // az eredeti, lekerekített rendszerbetű
    MODAK("modak"),     // Modak betűtípus (OFL)
    JELLY("jelly");     // gumicukor betűk + szörnyes számok (képek)

    companion object {
        fun from(id: String?): GlyphStyle = values().firstOrNull { it.id == id } ?: DEFAULT
    }
}

/**
 * Egy kép-karakter méretei a cella (a karakter doboza) arányában: szélesség, magasság, és az alsó él
 * függőleges helye (0 = a doboz teteje). Az értékek úgy vannak kiszámolva, hogy a kép sosem lóg ki a dobozból.
 */
internal class JellyGlyph(@DrawableRes val res: Int, val w: Float, val h: Float, val bottom: Float)

internal val JellyGlyphs: Map<Char, JellyGlyph> = mapOf(
    'A' to JellyGlyph(R.drawable.g_a, 0.6226f, 0.6247f, 0.8124f),
    'B' to JellyGlyph(R.drawable.g_b, 0.4664f, 0.6226f, 0.8113f),
    'C' to JellyGlyph(R.drawable.g_c, 0.5862f, 0.6376f, 0.8188f),
    'D' to JellyGlyph(R.drawable.g_d, 0.5113f, 0.6397f, 0.8199f),
    'E' to JellyGlyph(R.drawable.g_e, 0.4579f, 0.6269f, 0.8134f),
    'F' to JellyGlyph(R.drawable.g_f, 0.4279f, 0.6119f, 0.8060f),
    'G' to JellyGlyph(R.drawable.g_g, 0.6076f, 0.6397f, 0.8199f),
    'H' to JellyGlyph(R.drawable.g_h, 0.5199f, 0.6205f, 0.8102f),
    'I' to JellyGlyph(R.drawable.g_i, 0.1904f, 0.6205f, 0.8102f),
    'J' to JellyGlyph(R.drawable.g_j, 0.4557f, 0.6247f, 0.8124f),
    'K' to JellyGlyph(R.drawable.g_k, 0.5285f, 0.6397f, 0.8199f),
    'L' to JellyGlyph(R.drawable.g_l, 0.4151f, 0.6183f, 0.8092f),
    'M' to JellyGlyph(R.drawable.g_m, 0.6953f, 0.6226f, 0.8113f),
    'N' to JellyGlyph(R.drawable.g_n, 0.5905f, 0.6226f, 0.8113f),
    'O' to JellyGlyph(R.drawable.g_o, 0.6440f, 0.6397f, 0.8199f),
    'P' to JellyGlyph(R.drawable.g_p, 0.4857f, 0.6226f, 0.8113f),
    'Q' to JellyGlyph(R.drawable.g_q, 0.6440f, 0.7082f, 0.8541f),
    'R' to JellyGlyph(R.drawable.g_r, 0.4707f, 0.6611f, 0.8306f),
    'S' to JellyGlyph(R.drawable.g_s, 0.4707f, 0.6205f, 0.8102f),
    'T' to JellyGlyph(R.drawable.g_t, 0.5156f, 0.6290f, 0.8145f),
    'U' to JellyGlyph(R.drawable.g_u, 0.5392f, 0.6269f, 0.8134f),
    'V' to JellyGlyph(R.drawable.g_v, 0.5691f, 0.6419f, 0.8209f),
    'W' to JellyGlyph(R.drawable.g_w, 0.9200f, 0.6461f, 0.8231f),
    'X' to JellyGlyph(R.drawable.g_x, 0.5563f, 0.6183f, 0.8092f),
    'Y' to JellyGlyph(R.drawable.g_y, 0.5798f, 0.6162f, 0.8081f),
    'Z' to JellyGlyph(R.drawable.g_z, 0.5263f, 0.6226f, 0.8113f),
    '0' to JellyGlyph(R.drawable.g_0, 0.6782f, 0.6740f, 0.9161f),
    '1' to JellyGlyph(R.drawable.g_1, 0.2610f, 0.6483f, 0.9161f),
    '2' to JellyGlyph(R.drawable.g_2, 0.4964f, 0.6761f, 0.9161f),
    '3' to JellyGlyph(R.drawable.g_3, 0.4022f, 0.6718f, 0.9161f),
    '4' to JellyGlyph(R.drawable.g_4, 0.5434f, 0.7039f, 0.9161f),
    '5' to JellyGlyph(R.drawable.g_5, 0.5841f, 0.7167f, 0.9161f),
    '6' to JellyGlyph(R.drawable.g_6, 0.4450f, 0.8323f, 0.9161f),
    '7' to JellyGlyph(R.drawable.g_7, 0.6012f, 0.7553f, 0.9161f),
    '8' to JellyGlyph(R.drawable.g_8, 0.5178f, 0.7060f, 0.9161f),
    '9' to JellyGlyph(R.drawable.g_9, 0.4429f, 0.7125f, 0.9161f),
)

private const val GAP = 0.10f       // a karakterek közti rés a sorban (cellában)
private const val MIN_ADV = 0.5f    // a legkeskenyebb karakter (I, 1) is legalább ennyi helyet kap
private const val INK_FIT = 0.88f   // a legszélesebb betű (W) szélessége a cella arányában (Modak)
private const val SAFE_LO = 0.03f   // a karakter tintája ennél közelebb nem érhet a doboz széléhez
private const val SAFE_HI = 0.97f

/** Az eredeti (alap betűtípusos) elrendezés: a keskeny betűk közelebb kerülnek egymáshoz. */
internal fun legacyAdvance(ch: String): Float {
    val letter = ch.isNotEmpty() && ch.all { it.isLetterOrDigit() }
    return when {
        !letter -> 0.95f
        ch == "W" || ch == "M" -> 0.84f
        ch == "I" || ch == "1" -> 0.52f
        else -> 0.68f
    }
}

/** Egy betűtípus karakter-körvonalai (Path), a doboz-illesztéshez szükséges mérésekkel. */
internal class TextOutlines(typeface: Typeface) {
    class Outline(val path: Path, val l: Float, val t: Float, val r: Float, val b: Float)
    class Place(val s: Float, val tx: Float, val ty: Float)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.typeface = typeface; textSize = 100f }
    private val cache = HashMap<String, Outline?>()
    val maxInk: Float
    val refTop: Float
    val refBottom: Float

    init {
        var m = 1f
        for (c in "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789") {
            outline(c.toString())?.let { m = max(m, max(it.r - it.l, it.b - it.t)) }
        }
        maxInk = m
        val h = outline("H")
        refTop = h?.t ?: -70f
        refBottom = h?.b ?: 0f
    }

    /** null, ha a betűtípusban nincs kontúrja (pl. emoji): ilyenkor az eredeti szövegrajzolás marad. */
    fun outline(ch: String): Outline? {
        if (cache.containsKey(ch)) return cache[ch]
        val raw = android.graphics.Path()
        paint.getTextPath(ch, 0, ch.length, 0f, 0f, raw)
        val rf = RectF()
        raw.computeBounds(rf, true)
        val o = if (raw.isEmpty || rf.width() < 1f || rf.height() < 1f) null
        else Outline(raw.asComposePath(), rf.left, rf.top, rf.right, rf.bottom)
        cache[ch] = o
        return o
    }

    /** A karakter helye a dobozban (doboz = 1): állandó betűméret, középre igazítva, a doboz szélét sosem éri el. */
    fun place(o: Outline): Place {
        var s = INK_FIT / maxInk
        val w = (o.r - o.l) * s
        val h = (o.b - o.t) * s
        val lim = SAFE_HI - SAFE_LO
        if (w > lim || h > lim) s *= min(lim / w, lim / h)
        val tx = 0.5f - (o.l + o.r) / 2f * s
        var ty = 0.5f - (refTop + refBottom) / 2f * s
        val top = o.t * s + ty
        val bottom = o.b * s + ty
        if (top < SAFE_LO) ty += SAFE_LO - top else if (bottom > SAFE_HI) ty -= bottom - SAFE_HI
        return Place(s, tx, ty)
    }

    fun width(o: Outline): Float = (o.r - o.l) * place(o).s
}

/** Egy stílus összes mérése és rajzolási adata. */
internal class GlyphKit(val style: GlyphStyle, private val outlines: TextOutlines?) {
    fun jelly(ch: String): JellyGlyph? =
        if (style == GlyphStyle.JELLY && ch.length == 1) JellyGlyphs[ch[0].uppercaseChar()] else null

    fun outlineFor(ch: String): TextOutlines.Outline? = when (style) {
        GlyphStyle.DEFAULT -> null
        GlyphStyle.MODAK -> outlines?.outline(ch)
        GlyphStyle.JELLY -> if (jelly(ch) != null) null else outlines?.outline(ch)
    }

    fun place(o: TextOutlines.Outline): TextOutlines.Place = outlines!!.place(o)

    /** Van-e ehhez a karakterhez saját rajz ebben a stílusban (különben az eredeti szöveges rajz marad). */
    fun hasGlyph(ch: String): Boolean = style != GlyphStyle.DEFAULT && (jelly(ch) != null || outlineFor(ch) != null)

    /** Egy karakter vízszintes "helye" a sorban cellában mérve. */
    fun advance(ch: String): Float {
        jelly(ch)?.let { return max(MIN_ADV, it.w + GAP) }
        outlineFor(ch)?.let { return max(MIN_ADV, outlines!!.width(it) + GAP) }
        return legacyAdvance(ch)
    }
}

internal object GlyphKits {
    private val cache = HashMap<GlyphStyle, GlyphKit>()

    fun get(ctx: Context, style: GlyphStyle): GlyphKit = synchronized(cache) {
        cache.getOrPut(style) {
            val outlines = if (style == GlyphStyle.DEFAULT) null else {
                val tf = try { ResourcesCompat.getFont(ctx.applicationContext, R.font.modak_regular) } catch (_: Throwable) { null }
                tf?.let { TextOutlines(it) }
            }
            GlyphKit(style, outlines)
        }
    }
}

@Composable
internal fun rememberGlyphKit(style: GlyphStyle): GlyphKit {
    val ctx = LocalContext.current
    return remember(style) { GlyphKits.get(ctx, style) }
}

/**
 * A karakter rajza az adott stílusban. Ha a stílusban nincs hozzá rajz (pl. emoji), az `legacy` rajzol
 * (az eredeti szöveges megjelenés). A rajz mindig a `cell` méretű dobozon belül marad.
 */
@Composable
internal fun StyledGlyph(
    ch: String,
    colorIdx: Int,
    cell: Float,
    kit: GlyphKit,
    legacy: @Composable (String, Int, Float) -> Unit,
) {
    val jg = kit.jelly(ch)
    if (jg != null) {
        Box(Modifier.requiredSize(cell.dp)) {
            Image(
                painter = painterResource(jg.res),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .offset((cell * (0.5f - jg.w / 2f)).dp, (cell * (jg.bottom - jg.h)).dp)
                    .requiredSize((cell * jg.w).dp, (cell * jg.h).dp),
            )
        }
        return
    }
    val o = kit.outlineFor(ch)
    if (o != null) {
        OutlineGlyph(o, kit.place(o), ch, colorIdx, cell)
        return
    }
    legacy(ch, colorIdx, cell)
}

@Composable
private fun OutlineGlyph(o: TextOutlines.Outline, pl: TextOutlines.Place, ch: String, colorIdx: Int, cell: Float) {
    val c = KidColors[colorIdx]
    val story = ch.isNotEmpty() && ch.all { it.isLetterOrDigit() }
    val light = lerp(c, Color.White, 0.50f)
    val deep = lerp(c, Color.Black, 0.38f)
    val fill = Brush.verticalGradient(listOf(light, c, lerp(c, deep, 0.45f)), startY = o.t, endY = o.b)
    Canvas(Modifier.requiredSize(cell.dp)) {
        val box = size.minDimension
        val s = pl.s * box
        val outlinePx = (cell * 0.045f).dp.toPx()
        translate(pl.tx * box, pl.ty * box) {
            scale(s, s, Offset.Zero) {
                if (story) {
                    drawPath(o.path, deep, style = Stroke(width = outlinePx * 2f / s, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(o.path, fill)
                } else {
                    drawPath(o.path, c)
                }
            }
        }
    }
}

/** A begépelt szöveg az adott stílusban: a karakterek beágyazott rajzok, a többi (pl. emoji) sima szöveg. */
internal fun buildStyledHistory(history: String, kit: GlyphKit, hSp: Float): Pair<AnnotatedString, Map<String, InlineTextContent>> {
    val map = LinkedHashMap<String, InlineTextContent>()
    val legacyNone: @Composable (String, Int, Float) -> Unit = { _, _, _ -> }
    val text = buildAnnotatedString {
        var i = 0
        while (i < history.length) {
            val cp = history.codePointAt(i)
            val len = Character.charCount(cp)
            val s = history.substring(i, i + len)
            val colorIdx = cp % KidColors.size
            if (s != " " && kit.hasGlyph(s)) {
                val id = "g$cp"
                if (!map.containsKey(id)) {
                    map[id] = InlineTextContent(
                        Placeholder((kit.advance(s) * hSp).sp, hSp.sp, PlaceholderVerticalAlign.Center),
                    ) { _ ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            StyledGlyph(s, colorIdx, hSp, kit, legacyNone)
                        }
                    }
                }
                appendInlineContent(id, s)
            } else {
                withStyle(SpanStyle(color = KidColors[colorIdx])) { append(s) }
            }
            i += len
        }
    }
    return text to map
}

/** "ABC123" minta egy stílusban (a lenyíló menühöz): ugyanazzal a rajzolással, mint a játékban. */
@Composable
private fun GlyphPreview(style: GlyphStyle, size: Dp, legacy: @Composable (String, Int, Float) -> Unit) {
    val kit = rememberGlyphKit(style)
    Row(verticalAlignment = Alignment.CenterVertically) {
        "ABC123".forEachIndexed { i, c ->
            val s = c.toString()
            Box(
                Modifier.width((kit.advance(s) * size.value).dp).height(size),
                contentAlignment = Alignment.Center,
            ) { StyledGlyph(s, i, size.value, kit, legacy) }
        }
    }
}

/** Lenyíló betűtípus-választó: a fejléc az aktuális stílust mutatja, lenyitva mindhárom minta látszik. */
@Composable
internal fun GlyphStyleDropdown(
    current: GlyphStyle,
    width: Dp,
    bg: Color,
    fg: Color,
    accent: Color,
    onSelect: (GlyphStyle) -> Unit,
    legacy: @Composable (String, Int, Float) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    Column(Modifier.width(width), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.fillMaxWidth().clip(shape).background(bg).clickable { open = !open }.padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            GlyphPreview(current, 52.dp, legacy)
            Text(
                if (open) "▴" else "▾",
                color = fg,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
        if (open) {
            GlyphStyle.values().forEach { st ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(bg)
                        .then(if (st == current) Modifier.border(3.dp, accent, shape) else Modifier)
                        .clickable { onSelect(st); open = false }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) { GlyphPreview(st, 52.dp, legacy) }
            }
        }
    }
}
