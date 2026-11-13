@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package hu.gyerekbillentyuzet

import android.graphics.Typeface
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.gyerekbillentyuzet.feature.keypad.Burst
import hu.gyerekbillentyuzet.feature.keypad.KeypadUiState
import hu.gyerekbillentyuzet.feature.keypad.KeypadViewModel
import hu.gyerekbillentyuzet.feature.keypad.Typed
import hu.gyerekbillentyuzet.feature.lock.ChildLockController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private val Background = Color(0xFF131428)
private val BackgroundCenter = Color(0xFF262445)
private val Foreground = Color(0xFFF0F1F9)
private val Primary = Color(0xFF5FD37F)
private val Secondary = Color(0xFF2E304C)
private val MutedForeground = Color(0xFFA0A3B8)
private val Destructive = Color(0xFFE7000B)
private val DestructiveForeground = Color(0xFFF8FAFC)
private val KidColors = listOf(
    Color(0xFFFF706A), Color(0xFFFFC31A), Color(0xFF68DB70),
    Color(0xFF00BEFA), Color(0xFFDA74F1), Color(0xFFFF9F5F),
)

private const val K_TYPE = 0
private const val K_SPACE = 1
private const val K_SEND = 2
private const val K_GAP = 3

private data class KeySpec(val label: String, val weight: Float = 1f, val kind: Int = K_TYPE, val funShape: Boolean = false)

private fun letters(s: String) = s.map { KeySpec(it.toString()) }
private fun funKey(label: String, weight: Float = 1f, kind: Int = K_TYPE) = KeySpec(label, weight, kind, true)

private val FunTop = listOf("🦄", "🐸", "🌈", "🍭", "👻", "🐙").map { funKey(it) }

private val KeyRows: List<List<KeySpec>> = listOf(
    letters("1234567890"),
    letters("QWERTYUIOP"),
    listOf(KeySpec("", 0.5f, K_GAP)) + letters("ASDFGHJKL") + KeySpec("", 0.5f, K_GAP),
    listOf(funKey("⭐", 1.5f)) + letters("ZXCVBNM") + funKey("🎈", 1.5f),
    listOf(funKey("🍩", 1.5f), funKey("♥"), funKey("~ ~ ~", 5f, K_SPACE), funKey("★"), funKey("🚀", 1.5f, K_SEND)),
)

private val KidFont: FontFamily = try {
    FontFamily(Typeface.create("sans-serif-rounded", Typeface.BOLD))
} catch (_: Throwable) {
    FontFamily.SansSerif
}

class MainActivity : ComponentActivity() {
    private val keypadVm: KeypadViewModel by viewModels()

    override fun onStop() {
        keypadVm.wipe()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()
        pinApp()
        setContent { KeypadApp() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    fun pinApp() {
        try { startLockTask() } catch (e: Exception) { e.printStackTrace() }
    }

    fun unpinApp() {
        try { stopLockTask() } catch (e: Exception) { e.printStackTrace() }
    }
}

@Composable
fun KeypadApp(vm: KeypadViewModel = viewModel()) {
    val st by vm.state.collectAsState()
    val activity = androidx.compose.ui.platform.LocalContext.current as? MainActivity
    val lock = remember { ChildLockController { vm.showMenu() } }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var input by remember { mutableStateOf("") }

    LaunchedEffect(st.mode, st.parentMenu, st.exited, st.loaded) {
        if (st.loaded && st.mode == "native" && !st.parentMenu && !st.exited) {
            delay(150)
            try { focus.requestFocus() } catch (_: Exception) {}
            keyboard?.show()
        }
    }

    BackHandler(enabled = true) { }

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val c = Offset(size.width * 0.5f, size.height * 0.35f)
                val r = hypot(max(c.x, size.width - c.x), max(c.y, size.height - c.y))
                drawRect(
                    Brush.radialGradient(
                        0f to BackgroundCenter, 0.7f to Background, 1f to Background,
                        center = c, radius = r,
                    )
                )
            }
    ) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .imePadding()
            .pointerInput(st.mode, st.parentMenu) {
                detectTapGestures(onTap = {
                    if (st.mode == "native" && !st.parentMenu) {
                        try { focus.requestFocus() } catch (_: Exception) {}
                        keyboard?.show()
                    }
                })
            }
    ) {
        val screenW = maxWidth
        val btnW = minOf(screenW * 0.9f, 360.dp)

        if (!st.loaded) return@BoxWithConstraints

        if (st.exited) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                KidText("Viszlát! 👋", 30.sp, Foreground)
                KidButton("Újra indítás", Primary, Background, btnW) {
                    vm.restart()
                    activity?.pinApp()
                    activity?.hideSystemBars()
                }
            }
            return@BoxWithConstraints
        }

        val inner = st.mode == "inner"

        Stage(
            st,
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .then(if (inner) Modifier.fillMaxHeight(0.47f) else Modifier.fillMaxHeight()),
        )

        if (st.mode == "native" && !st.parentMenu) {
            BasicTextField(
                value = input,
                onValueChange = { v ->
                    // Egyszerre érkezhet több karakter is (gyors, egyidejű nyomás). Egy hosszú
                    // beillesztés / szójavaslat esetén csak az utolsót vesszük.
                    if (v.isNotEmpty()) {
                        if (v.length <= 4) {
                            var i = 0
                            while (i < v.length) {
                                val cp = v.codePointAt(i)
                                vm.press(String(Character.toChars(cp)))
                                i += Character.charCount(cp)
                            }
                        } else {
                            val cp = v.codePointBefore(v.length)
                            vm.press(String(Character.toChars(cp)))
                        }
                    }
                    input = ""
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(1.dp)
                    .alpha(0f)
                    .focusRequester(focus),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                ),
            )
        }

        if (inner) {
            val keySp = (screenW.value * 0.055f).coerceIn(16f, 30f)
            FunKeyboard(
                fontSp = keySp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.477f),
            ) { k ->
                when (k.kind) {
                    K_SPACE -> vm.pressSpace()
                    K_SEND -> vm.sendNow()
                    else -> vm.press(k.label)
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
                .size(48.dp)
                .alpha(0.4f)
                .pointerInput(Unit) { detectTapGestures(onPress = { lock.tap() }) },
            contentAlignment = Alignment.Center,
        ) {
            Text("🔒", fontSize = 24.sp)
        }

        if (st.mode == null) {
            Overlay {
                KidText("Melyik billentyűzet legyen?", 30.sp, Foreground, center = true)
                KidButton("🎨 Nagy színes gombok", Primary, Background, btnW) {
                    vm.choose("inner"); activity?.hideSystemBars()
                }
                KidButton("📱 Telefon billentyűzete", Secondary, Foreground, btnW) {
                    vm.choose("native"); activity?.hideSystemBars()
                }
                Text(
                    "Kilépés és beállítások: 5× gyors koppintás a jobb felső lakatra.",
                    modifier = Modifier.widthIn(max = 320.dp),
                    color = MutedForeground,
                    fontSize = 14.sp,
                    fontFamily = KidFont,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (st.parentMenu) {
            Overlay {
                KidText("Szülői menü", 30.sp, Foreground)
                KidButton(
                    "Billentyűzet: ${if (inner) "nagy gombok" else "telefoné"} ↔",
                    Secondary, Foreground, btnW,
                ) { vm.toggleMode() }
                KidButton("Hang: ${if (st.soundEnabled) "be 🔊" else "ki 🔇"}", Secondary, Foreground, btnW) {
                    vm.setSound(!st.soundEnabled)
                }
                KidButton("Begépelt szöveg törlése 🗑", Secondary, Foreground, btnW) { vm.clearText() }
                KidButton("Vissza a játékhoz", Primary, Background, btnW) {
                    vm.closeMenu(); activity?.hideSystemBars()
                }
                KidButton("Kilépés", Destructive, DestructiveForeground, btnW) {
                    activity?.unpinApp()
                    vm.exit()
                }
            }
        }
    }
    }
}

// ---- Karakter-sor elrendezés ----
private const val GLOW_R = 0.80f   // a glow sugara a cella méretében (a doboz szélén túl is ér)
private const val EDGE_DP = 8f     // biztonsági margó a képernyő szélétől (a glow is belefér)
private const val JUMP = 1.2f      // ennyi cellányit ugrik be felülről a karakter

/** Egy karakter vízszintes "helye" a sorban cellában mérve: a keskeny betűk közelebb kerülnek egymáshoz. */
private fun advance(ch: String): Float {
    val letter = ch.isNotEmpty() && ch.all { it.isLetterOrDigit() }
    return when {
        !letter -> 0.95f
        ch == "W" || ch == "M" -> 0.84f
        ch == "I" || ch == "1" -> 0.52f
        else -> 0.68f
    }
}

/** Ha a sor (glow-val együtt) nem férne el a képernyőn, ennyiszeresére zsugorítjuk. */
private fun rowFit(chars: List<String>, sw: Float, cell: Float): Float {
    if (chars.isEmpty()) return 1f
    val adv = chars.map { advance(it) }
    val over = max(GLOW_R - adv.first() / 2f, GLOW_R - adv.last() / 2f)
    val need = (adv.sum() + 2f * over) * cell
    return min(1f, (sw - 2f * EDGE_DP) / need)
}

/** A karakterek középpontjai (dp), a sor a képernyő közepére igazítva. */
private fun rowCenters(chars: List<String>, sw: Float, cell: Float): List<Float> {
    val adv = chars.map { advance(it) }
    var x = (sw - adv.sum() * cell) / 2f
    return adv.map { a ->
        val c = x + a * cell / 2f
        x += a * cell
        c
    }
}

@Composable
private fun Stage(st: KeypadUiState, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val sw = maxWidth.value
        val sh = maxHeight.value
        val topReserve = 64f + 134f + 6f // előzmény-panel helye
        // 5 betűs sor + glow pont elfér a szélességben; magasságban is marad hely a glow-nak és az ugrásnak.
        val wCell = (sw - 2f * EDGE_DP) / 4.32f
        val vCell = (sh - topReserve - 12f) / (GLOW_R + 0.55f)
        val cell0 = min(wCell, max(vCell, wCell * 0.75f))
        val chars = st.pending.map { it.ch }
        val fit = rowFit(chars, sw, cell0)
        val cell by animateFloatAsState(cell0 * fit, spring(dampingRatio = 0.7f, stiffness = 350f), label = "cell")
        val cy = min(max(sh * 0.60f, topReserve + cell0 * 0.55f), sh - GLOW_R * cell0 - 12f)
        val centers = rowCenters(chars, sw, cell)
        val bob = rememberInfiniteTransition(label = "bob").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
            label = "bobT",
        )

        HistoryPanel(
            st.history, st.bump,
            Modifier.align(Alignment.TopCenter).padding(top = 64.dp, start = 20.dp, end = 20.dp),
        )
        if (st.flying.isNotEmpty()) {
            key(st.flyId) {
                val fc = cell0 * rowFit(st.flying.map { it.ch }, sw, cell0)
                FlyingRow(st.flying, sw, fc, cy)
            }
        }
        st.pending.forEachIndexed { i, t ->
            key(t.id) { TypedChar(t, i, centers[i], cell, cy, bob) }
        }
        st.bursts.forEach { b -> key(b.id) { BurstView(b, sw, cy, centers) } }
    }
}

@Composable
private fun HistoryPanel(history: String, bump: Long, modifier: Modifier) {
    if (history.isEmpty()) return
    val s = remember { Animatable(1f) }
    val scroll = rememberScrollState()
    LaunchedEffect(bump) {
        if (bump > 0) {
            s.snapTo(1.1f)
            s.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 260f))
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { scroll.maxValue }.collectLatest { scroll.animateScrollTo(it) }
    }
    val text = remember(history) {
        buildAnnotatedString {
            var i = 0
            while (i < history.length) {
                val cp = history.codePointAt(i)
                val len = Character.charCount(cp)
                withStyle(SpanStyle(color = KidColors[cp % KidColors.size])) { append(history.substring(i, i + len)) }
                i += len
            }
        }
    }
    Box(
        modifier
            .graphicsLayer { scaleX = s.value; scaleY = s.value }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .heightIn(max = 132.dp),
    ) {
        Text(
            text,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 30.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = KidFont,
            textAlign = TextAlign.Center,
            letterSpacing = 2.sp,
        )
    }
}

private fun sparklePath(r: Float): Path {
    val path = Path()
    val pts = listOf(0f to -1f, 0.22f to -0.22f, 1f to 0f, 0.22f to 0.22f, 0f to 1f, -0.22f to 0.22f, -1f to 0f, -0.22f to -0.22f)
    pts.forEachIndexed { i, (px, py) -> if (i == 0) path.moveTo(px * r, py * r) else path.lineTo(px * r, py * r) }
    path.close()
    return path
}

/** Lágy, a széle felé átlátszóvá simuló glow-korong. A korong négyzete pontosan körbeveszi, így semmi sem vágódik el. */
@Composable
private fun GlowDisc(c: Color, cell: Float, glow: () -> Float, layer: GraphicsLayerScope.() -> Unit = {}) {
    Box(
        Modifier
            .requiredSize((cell * GLOW_R * 2f).dp)
            .graphicsLayer { layer() }
            .drawBehind {
                val g = glow()
                val r = size.minDimension / 2f
                drawCircle(
                    Brush.radialGradient(
                        0f to c.copy(alpha = 0.75f * g),
                        0.45f to c.copy(alpha = 0.45f * g),
                        0.75f to c.copy(alpha = 0.15f * g),
                        1f to Color.Transparent,
                        center = center, radius = r,
                    ),
                    radius = r, center = center,
                )
            }
    )
}

/// Meseszerű betű (glow nélkül).
@Composable
private fun Glyph(ch: String, colorIdx: Int, cell: Float) {
    val c = KidColors[colorIdx]
    val density = LocalDensity.current
    val outlinePx = with(density) { (cell * 0.045f).dp.toPx() }
    val story = ch.isNotEmpty() && ch.all { it.isLetterOrDigit() }
    // dp-ből számolunk sp-t, hogy a rendszer betűméret-beállítása ne nyomja szét az elrendezést
    val fs = with(density) { (cell * if (story) 0.88f else 0.80f).dp.toSp() }
    val light = lerp(c, Color.White, 0.50f)
    val deep = lerp(c, Color.Black, 0.38f)
    val fillBrush = Brush.verticalGradient(listOf(light, c, lerp(c, deep, 0.45f)))

    Box(Modifier.size(cell.dp), contentAlignment = Alignment.Center) {
        if (story) {
            Text(
                ch,
                style = TextStyle(
                    color = deep, fontSize = fs, lineHeight = fs,
                    fontWeight = FontWeight.Bold, fontFamily = KidFont,
                    drawStyle = Stroke(width = outlinePx * 2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                ),
            )
            Text(
                ch,
                style = TextStyle(
                    brush = fillBrush, fontSize = fs, lineHeight = fs,
                    fontWeight = FontWeight.Bold, fontFamily = KidFont,
                ),
            )
        } else {
            Text(
                ch,
                style = TextStyle(
                    color = c, fontSize = fs, lineHeight = fs,
                    fontWeight = FontWeight.Bold, fontFamily = KidFont,
                ),
            )
        }
    }
}

/// Betű + háttérfény együtt (a szövegbe repülő sorhoz).
@Composable
private fun BigChar(ch: String, colorIdx: Int, cell: Float, glow: State<Float>? = null) {
    Box(Modifier.size(cell.dp), contentAlignment = Alignment.Center) {
        GlowDisc(KidColors[colorIdx], cell, { glow?.value ?: 0.4f })
        Glyph(ch, colorIdx, cell)
    }
}

@Composable
private fun TypedChar(t: Typed, slot: Int, cx: Float, cell: Float, cy: Float, bob: State<Float>) {
    val x by animateFloatAsState(cx - cell / 2f, spring(dampingRatio = 0.55f, stiffness = 300f), label = "x")
    val p = remember { Animatable(0f) }
    val glow = remember { Animatable(1f) }
    // Lazább, ruganyosabb rugó: nagyobbat lendül túl, mielőtt a helyére áll.
    LaunchedEffect(Unit) { p.animateTo(1f, spring(dampingRatio = 0.30f, stiffness = 210f)) }
    LaunchedEffect(Unit) { glow.animateTo(0.4f, tween(1100)) }
    val dir = if (t.id % 2L == 0L) -1f else 1f
    val c = KidColors[t.colorIdx]
    Box(
        Modifier
            .offset { IntOffset(x.dp.roundToPx(), (cy - cell / 2f).dp.roundToPx()) }
            .size(cell.dp),
        contentAlignment = Alignment.Center,
    ) {
        // A glow nem lő túl (nem nő a helyfoglalása), így a képernyő szélén sem vágódik el.
        GlowDisc(c, cell, { glow.value }) {
            val v = min(p.value, 1f)
            val ph = (bob.value + slot * 0.17f) * 2f * PI.toFloat()
            val s = 0.6f + 0.4f * v
            scaleX = s
            scaleY = s
            translationY = (1f - v) * (-JUMP * cell.dp.toPx()) + sin(ph) * 4.dp.toPx()
            alpha = min(1f, v * 3f).coerceAtLeast(0f)
            compositingStrategy = CompositingStrategy.ModulateAlpha
        }
        Box(
            Modifier.graphicsLayer {
                val v = p.value
                val ph = (bob.value + slot * 0.17f) * 2f * PI.toFloat()
                scaleX = v
                scaleY = v * (1f + 0.30f * (1f - v))
                rotationZ = (1f - v) * 42f * dir + sin(ph) * 2f
                translationY = (1f - v) * (-JUMP * cell.dp.toPx()) + sin(ph) * 4.dp.toPx()
                alpha = min(1f, v * 3f).coerceAtLeast(0f)
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
            contentAlignment = Alignment.Center,
        ) { Glyph(t.ch, t.colorIdx, cell) }
    }
}

@Composable
private fun FlyingRow(items: List<Typed>, sw: Float, cell: Float, cy: Float) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(640, easing = LinearEasing)) }
    val ease = remember { CubicBezierEasing(0.5f, -0.4f, 0.8f, 0.6f) }
    val centers = remember { rowCenters(items.map { it.ch }, sw, cell) }
    val hy = 104f
    items.forEachIndexed { i, t ->
        val x = centers[i] - cell / 2f
        Box(
            Modifier
                .offset { IntOffset(x.dp.roundToPx(), (cy - cell / 2f).dp.roundToPx()) }
                .size(cell.dp)
                .graphicsLayer {
                    val local = ((p.value * 640f - i * 45f) / 460f).coerceIn(0f, 1f)
                    val e = ease.transform(local)
                    translationY = -(cy - hy).dp.toPx() * e
                    val sc = 1f - 0.7f * e
                    scaleX = sc
                    scaleY = sc
                    rotationZ = e * (i - 2) * 18f
                    alpha = if (local > 0.7f) 1f - (local - 0.7f) / 0.3f else 1f
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                },
            contentAlignment = Alignment.Center,
        ) { BigChar(t.ch, t.colorIdx, cell) }
    }
}

private class Particle(
    val vx: Float, val vy: Float, val spin: Float, val kind: Int,
    val color: Color, val size: Float, val phase: Float,
)

private fun makeParticles(b: Burst): List<Particle> {
    val rnd = Random(b.id.toInt() * 7919 + 13)
    val n = when (b.kind) { 0 -> 7; 1 -> 15; else -> 13 }
    val kinds = (0 until 10).shuffled(rnd)
    return List(n) { i ->
        val ang = if (b.kind == 0) -90.0 + (rnd.nextDouble() - 0.5) * 140.0 else rnd.nextDouble() * 360.0
        val speed = if (b.kind == 0) 280 + rnd.nextDouble() * 380 else 300 + rnd.nextDouble() * 460
        val rad = Math.toRadians(ang)
        var vy = (sin(rad) * speed).toFloat()
        if (b.kind != 0) vy -= 220f
        Particle(
            vx = (cos(rad) * speed).toFloat(),
            vy = vy,
            spin = ((rnd.nextDouble() - 0.5) * 2 * (300 + rnd.nextDouble() * 600)).toFloat(),
            kind = kinds[i % kinds.size],
            color = KidColors[(i + b.seed) % KidColors.size],
            size = (10 + rnd.nextDouble() * 12).toFloat(),
            phase = (rnd.nextDouble() * 6.28).toFloat(),
        )
    }
}

@Composable
private fun BurstView(b: Burst, sw: Float, cy: Float, centers: List<Float>) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(2000, easing = LinearEasing)) }
    val parts = remember { makeParticles(b) }
    // A kiindulópont a létrejöttekor rögzül (a karakter helyén), akkor is, ha a sor később átrendeződik.
    val originX = remember { if (b.kind == 2) sw / 2f else centers.getOrElse(b.slot) { sw / 2f } }
    Canvas(Modifier.fillMaxSize()) {
        val s = p.value * 2f
        val ox = originX.dp.toPx()
        val oy = cy.dp.toPx()
        for (q in parts) drawParticle(q, s, ox, oy)
    }
}

private fun DrawScope.drawParticle(q: Particle, s: Float, ox: Float, oy: Float) {
    val life = 1.7f
    if (s < 0f || s > life) return
    val k = 2.2f
    val d = (1f - exp(-k * s)) / k
    val x = ox + (q.vx * d + sin(q.phase + s * 7f) * 6f * min(s, 1f)).dp.toPx()
    val y = oy + (q.vy * d + 0.5f * 900f * s * s).dp.toPx()
    val a = if (s > life * 0.65f) 1f - (s - life * 0.65f) / (life * 0.35f) else 1f
    val sc = when {
        s < 0.12f -> s / 0.12f * 1.25f
        s < 0.2f -> 1.25f - (s - 0.12f) / 0.08f * 0.25f
        else -> 1f
    }
    translate(x, y) {
        rotate(q.spin * s, Offset.Zero) {
            scale(sc, Offset.Zero) {
                val aa = a.coerceIn(0f, 1f)
                val u = q.size.dp.toPx()
                drawShape(q, aa * 0.10f, u * 1.4f)
                drawShape(q, aa * 0.17f, u * 0.8f)
                drawShape(q, aa * 0.28f, u * 0.38f)
                drawShape(q, aa)
            }
        }
    }
}

private fun DrawScope.drawShape(q: Particle, a: Float, grow: Float = 0f) {
    val s = q.size.dp.toPx()
    val fs = if (grow > 0f) Stroke(grow, cap = StrokeCap.Round, join = StrokeJoin.Round) else Fill
    when (q.kind) {
        0 -> drawRoundRect(q.color, Offset(-s * 0.5f, -s * 0.75f), Size(s, s * 1.5f), CornerRadius(s * 0.2f), alpha = a, style = fs)
        1 -> drawCircle(q.color, s * 0.55f, Offset.Zero, alpha = a, style = fs)
        2 -> drawPath(polyPath(10) { i -> if (i % 2 == 0) s else s * 0.45f }, q.color, alpha = a, style = fs)
        3 -> {
            val path = Path()
            path.moveTo(0f, s * 0.9f)
            path.cubicTo(-s * 1.4f, -s * 0.1f, -s * 0.7f, -s * 1.1f, 0f, -s * 0.4f)
            path.cubicTo(s * 0.7f, -s * 1.1f, s * 1.4f, -s * 0.1f, 0f, s * 0.9f)
            path.close()
            drawPath(path, q.color, alpha = a, style = fs)
        }
        4 -> drawArc(q.color, 135f, 180f, false, Offset(-s, -s), Size(s * 2f, s * 2f), alpha = a, style = Stroke(s * 0.3f + grow, cap = StrokeCap.Round))
        5 -> {
            val w = s * 0.55f
            val path = Path()
            path.moveTo(-w * 2f, 0f)
            path.quadraticBezierTo(-w * 1.5f, -w, -w, 0f)
            path.quadraticBezierTo(-w * 0.5f, w, 0f, 0f)
            path.quadraticBezierTo(w * 0.5f, -w, w, 0f)
            path.quadraticBezierTo(w * 1.5f, w, w * 2f, 0f)
            drawPath(path, q.color, alpha = a, style = Stroke(s * 0.3f + grow, cap = StrokeCap.Round))
        }
        6 -> drawCircle(q.color, s * 0.6f, Offset.Zero, alpha = a, style = Stroke(s * 0.28f + grow))
        7 -> drawPath(polyPath(4) { s * 0.9f }, q.color, alpha = a, style = fs)
        8 -> drawPath(polyPath(3) { s * 0.95f }, q.color, alpha = a, style = fs)
        else -> drawPath(sparklePath(s), q.color, alpha = a, style = fs)
    }
}

private fun polyPath(n: Int, radius: (Int) -> Float): Path {
    val path = Path()
    for (i in 0 until n) {
        val r = radius(i)
        val ang = (-90.0 + i * 360.0 / n) * PI / 180.0
        val px = (cos(ang) * r).toFloat()
        val py = (sin(ang) * r).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    return path
}

@Composable
private fun Overlay(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Background.copy(alpha = 0.95f))
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

@Composable
private fun KidText(text: String, size: TextUnit, color: Color, center: Boolean = false) {
    Text(
        text,
        color = color,
        fontSize = size,
        fontWeight = FontWeight.Bold,
        fontFamily = KidFont,
        textAlign = if (center) TextAlign.Center else null,
    )
}

@Composable
private fun KidButton(text: String, bg: Color, fg: Color, width: Dp, onClick: () -> Unit) {
    Box(
        Modifier
            .width(width)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontSize = 21.6.sp, fontWeight = FontWeight.Bold, fontFamily = KidFont, textAlign = TextAlign.Center)
    }
}

/**
 * A billentyűzet egyetlen közös érintéskezelővel dolgozik:
 *  - több ujj egyszerre is nyomhat gombokat (mindegyik külön hangot / karaktert ad),
 *  - nem kell pontosan a gombra nyomni: a gombok közti résre vagy a szélére tett ujj a legközelebbi gombot találja el,
 *  - de egy bizonyos távolságon túl (16 dp) már nem ír ki semmit.
 */
@Composable
private fun FunKeyboard(fontSp: Float, modifier: Modifier, onKey: (KeySpec) -> Unit) {
    val tolerancePx = with(LocalDensity.current) { 16.dp.toPx() }
    val bounds = remember { HashMap<Int, Rect>() }          // gomb-azonosító -> terület (root koordináta)
    val specs = remember { HashMap<Int, KeySpec>() }
    val pressCount = remember { mutableStateMapOf<Int, Int>() } // hány ujj nyomja épp
    val ripples = remember { mutableStateMapOf<Int, Int>() }    // hullám-számláló gombonként
    val origin = remember { floatArrayOf(0f, 0f) }
    val keyCb by rememberUpdatedState(onKey)

    fun hit(x: Float, y: Float): Int? {
        var best: Int? = null
        var bestD = Float.MAX_VALUE
        var bestC = Float.MAX_VALUE
        for ((id, r) in bounds) {
            val dx = max(max(r.left - x, 0f), x - r.right)
            val dy = max(max(r.top - y, 0f), y - r.bottom)
            val d = hypot(dx, dy)
            if (d > tolerancePx) continue
            val c = hypot(x - r.center.x, y - r.center.y)
            if (d < bestD - 0.5f || (abs(d - bestD) <= 0.5f && c < bestC)) {
                best = id
                bestD = d
                bestC = c
            }
        }
        return best
    }

    fun keyModifier(id: Int, k: KeySpec): Modifier = Modifier.onGloballyPositioned { c ->
        val pos = c.positionInRoot()
        bounds[id] = Rect(pos.x, pos.y, pos.x + c.size.width, pos.y + c.size.height)
        specs[id] = k
    }

    Box(
        modifier
            .onGloballyPositioned { c ->
                val pos = c.positionInRoot()
                origin[0] = pos.x
                origin[1] = pos.y
            }
            .pointerInput(Unit) {
                val active = HashMap<Long, Int>() // ujj azonosító -> gomb
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        for (c in event.changes) {
                            val pid = c.id.value
                            if (c.pressed && !c.previousPressed) {
                                val k = hit(c.position.x + origin[0], c.position.y + origin[1])
                                if (k != null) {
                                    active[pid] = k
                                    pressCount[k] = (pressCount[k] ?: 0) + 1
                                    ripples[k] = (ripples[k] ?: 0) + 1
                                    specs[k]?.let { keyCb(it) }
                                }
                                c.consume()
                            } else if (!c.pressed && c.previousPressed) {
                                val k = active.remove(pid)
                                if (k != null) {
                                    val n = (pressCount[k] ?: 1) - 1
                                    if (n <= 0) pressCount.remove(k) else pressCount[k] = n
                                }
                                c.consume()
                            }
                        }
                    }
                }
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(Modifier.weight(0.85f).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                FunTop.forEachIndexed { i, k ->
                    KidKey(
                        label = k.label,
                        color = KidColors[(i + 2) % KidColors.size],
                        fontSp = fontSp,
                        funShape = true,
                        pressed = (pressCount[i] ?: 0) > 0,
                        rippleSeq = ripples[i] ?: 0,
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .then(keyModifier(i, k)),
                    )
                }
            }
            KeyRows.forEachIndexed { ri, row ->
                Row(Modifier.weight(if (ri == KeyRows.lastIndex) 1f else 0.9f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEachIndexed { ki, k ->
                        if (k.kind == K_GAP) {
                            Spacer(Modifier.weight(k.weight))
                        } else {
                            val id = 100 + ri * 20 + ki
                            KidKey(
                                label = k.label,
                                color = KidColors[(ri * 3 + ki) % KidColors.size],
                                fontSp = if (k.funShape) fontSp * 0.9f else fontSp,
                                funShape = k.funShape,
                                pressed = (pressCount[id] ?: 0) > 0,
                                rippleSeq = ripples[id] ?: 0,
                                modifier = Modifier.weight(k.weight).fillMaxHeight().then(keyModifier(id, k)),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Csak megjelenítés: az érintést a FunKeyboard kezeli. */
@Composable
private fun KidKey(
    label: String,
    color: Color,
    fontSp: Float,
    modifier: Modifier,
    funShape: Boolean = false,
    pressed: Boolean,
    rippleSeq: Int,
) {
    var hold by remember { mutableStateOf(false) }
    val rippleProgress = remember { Animatable(1f) }
    // Egy nagyon gyors koppintás se maradjon láthatatlan: a lenyomott állapot legalább ~90 ms-ig látszik.
    LaunchedEffect(rippleSeq) {
        if (rippleSeq > 0) {
            hold = true
            delay(90)
            hold = false
        }
    }
    LaunchedEffect(rippleSeq) {
        if (rippleSeq > 0) {
            rippleProgress.snapTo(0f)
            rippleProgress.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
        }
    }
    val down = pressed || hold
    val squish by animateFloatAsState(
        if (down) 1f else 0f,
        spring(dampingRatio = 0.4f, stiffness = 550f),
        label = "squish",
    )
    val corner = if (funShape) 40.dp else 14.4.dp
    val bg = if (funShape) color.copy(alpha = 0.3f) else color
    val fg = if (funShape) color else Background
    Box(
        modifier
            .drawBehind {
                val dy = (4.dp - 3.dp * squish).toPx()
                drawRoundRect(
                    Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(0f, dy),
                    size = size,
                    cornerRadius = CornerRadius(corner.toPx()),
                )
            }
            .graphicsLayer {
                translationY = 3.dp.toPx() * squish
                scaleX = 1f - 0.07f * squish
                scaleY = 1f - 0.07f * squish
            }
            .clip(RoundedCornerShape(corner))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, fontSize = fontSp.sp, fontWeight = FontWeight.Bold, fontFamily = KidFont, maxLines = 1)
        if (rippleSeq > 0 && rippleProgress.value < 1f) {
            Canvas(Modifier.matchParentSize().clip(RoundedCornerShape(corner))) {
                val pr = rippleProgress.value
                val radius = size.maxDimension * (0.12f + 0.95f * pr)
                drawCircle(
                    Brush.radialGradient(
                        0f to Color.White.copy(alpha = 0.24f * (1f - pr)),
                        0.65f to color.copy(alpha = 0.2f * (1f - pr)),
                        1f to Color.Transparent,
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            }
        }
    }
}
