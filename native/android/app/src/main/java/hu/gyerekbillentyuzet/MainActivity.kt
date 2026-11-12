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
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// ---- Téma: a webes styles.css oklch-színei sRGB-ben ----
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

// ---- Billentyűzet: a telefon billentyűzetéhez hasonló kiosztás, de a felesleges kapcsolók helyén vicces formák ----
private const val K_TYPE = 0
private const val K_SPACE = 1
private const val K_SEND = 2
private const val K_GAP = 3

private data class KeySpec(val label: String, val weight: Float = 1f, val kind: Int = K_TYPE, val funShape: Boolean = false)

private fun letters(s: String) = s.map { KeySpec(it.toString()) }
private fun funKey(label: String, weight: Float = 1f, kind: Int = K_TYPE) = KeySpec(label, weight, kind, true)

// Felső "kezelősáv": kerek, hintázó emoji gombok (beírják magukat)
private val FunTop = listOf("🦄", "🐸", "🌈", "🍭", "👻", "🐙").map { funKey(it) }

// QWERTY (mint a képen). Ha QWERTZ kell: "QWERTYUIOP" -> "QWERTZUIOP" és "ZXCVBNM" -> "YXCVBNM".
private val KeyRows: List<List<KeySpec>> = listOf(
    letters("1234567890"),
    letters("QWERTYUIOP"),
    listOf(KeySpec("", 0.5f, K_GAP)) + letters("ASDFGHJKL") + KeySpec("", 0.5f, K_GAP),
    listOf(funKey("⭐", 1.5f)) + letters("ZXCVBNM") + funKey("🎈", 1.5f), // shift és törlés helyén
    listOf(funKey("🍩", 1.5f), funKey("♥"), funKey("~ ~ ~", 5f, K_SPACE), funKey("★"), funKey("🚀", 1.5f, K_SEND)),
)

// A web Fredoka betűjéhez legközelebbi kerekített rendszerbetű.
private val KidFont: FontFamily = try {
    FontFamily(Typeface.create("sans-serif-rounded", Typeface.BOLD))
} catch (_: Throwable) {
    FontFamily.SansSerif
}

class MainActivity : ComponentActivity() {
    private val keypadVm: KeypadViewModel by viewModels()

    override fun onStop() {
        keypadVm.wipe() // háttérbe kerüléskor / kilépéskor a puffer törlődik
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false) // a billentyűzet-magasság (ime inset) így jut el a Compose-hoz
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()
        pinApp() // Gyerekzár: induláskor azonnal rögzítjük a képernyőt (Lock Task Mode)
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

    // Telefon saját billentyűzete: fókusz + billentyűzet megjelenítése
    LaunchedEffect(st.mode, st.parentMenu, st.exited, st.loaded) {
        if (st.loaded && st.mode == "native" && !st.parentMenu && !st.exited) {
            delay(150)
            try { focus.requestFocus() } catch (_: Exception) {}
            keyboard?.show()
        }
    }

    // Vissza gomb tiltva (mint a webes history.pushState trükk)
    BackHandler(enabled = true) { }

    // A háttér a teljes képernyőt kitölti; a tartalom a rendszer-billentyűzet fölötti részre szorul (imePadding).
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

        // Színpad: begépelt szöveg + felsorakozó nagy karakterek + konfetti
        Stage(
            st,
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .then(if (inner) Modifier.fillMaxHeight(0.47f) else Modifier.fillMaxHeight()),
        )

        // Telefon billentyűzete: rejtett beviteli mező
        if (st.mode == "native" && !st.parentMenu) {
            BasicTextField(
                value = input,
                onValueChange = { v ->
                    if (v.isNotEmpty()) {
                        val cp = v.codePointBefore(v.length)
                        vm.press(String(Character.toChars(cp)))
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

        // Nagy színes gombok (a telefon billentyűzetéhez hasonló kiosztás)
        if (inner) {
            val keySp = (screenW.value * 0.055f).coerceIn(16f, 30f)
            FunKeyboard(
                fontSp = keySp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.53f)
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
            ) { k ->
                when (k.kind) {
                    K_SPACE -> vm.pressSpace()
                    K_SEND -> vm.sendNow()
                    else -> vm.press(k.label)
                }
            }
        }

        // Lakat: 5 gyors koppintás (2 mp-en belül) nyitja a szülői menüt
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

        // Billentyűzet-választó
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

        // Szülői menü
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
                    activity?.unpinApp() // gyerekzár feloldása
                    vm.exit()
                }
            }
        }
    }
    }
}

// ---- Színpad ----
@Composable
private fun Stage(st: KeypadUiState, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val sw = maxWidth.value
        val sh = maxHeight.value
        val cell = min(sw * 0.19f, sh * 0.34f)
        // A sor középpontja: a szöveg-panel alatt, de a (billentyűzet miatt lecsökkent) színpadon belül marad.
        val minCy = 64f + 134f + cell / 2f + 6f
        val cy = min(max(sh * 0.60f, minCy), sh - cell / 2f - 8f)
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
            key(st.flyId) { FlyingRow(st.flying, sw, cell, cy) }
        }
        st.pending.forEachIndexed { i, t ->
            key(t.id) { TypedChar(t, i, st.pending.size, sw, cell, cy, bob) }
        }
        st.bursts.forEach { b -> key(b.id) { BurstView(b, sw, cell, cy) } }
    }
}

/** A már begépelt szöveg: görgethető, minden betű a saját színével, új betűnél lágyan megpattan és az aljára görget. */
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
    // Ha nő a szöveg, az alját mutatjuk; felfelé görgetve végigböngészhető.
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

@Composable
private fun BigChar(ch: String, colorIdx: Int, cell: Float, glow: State<Float>? = null) {
    val glowPx = with(LocalDensity.current) { (cell * 0.22f).dp.toPx() }
    val c = KidColors[colorIdx]
    Box(
        Modifier
            .size(cell.dp)
            .drawBehind {
                // lágy, színes fényudvar a betű mögött (az újonnan érkezett erősebb, aztán csillapodik)
                val g = glow?.value ?: 0.4f
                val r = size.minDimension * 0.9f
                drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.5f * g), Color.Transparent), center, r), r, center)
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            ch,
            style = TextStyle(
                color = c,
                fontSize = (cell * 0.9f).sp,
                lineHeight = (cell * 0.9f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = KidFont,
                shadow = Shadow(c.copy(alpha = 0.85f), Offset.Zero, glowPx),
            ),
        )
    }
}

/** Egy felugró karakter: rugós (túllendülő) belépés, utána finom lebegés; a sor többi tagja rugalmasan odébb csúszik. */
@Composable
private fun TypedChar(t: Typed, slot: Int, total: Int, sw: Float, cell: Float, cy: Float, bob: State<Float>) {
    val targetX = (sw - total * cell) / 2f + slot * cell
    val x by animateFloatAsState(targetX, spring(dampingRatio = 0.55f, stiffness = 300f), label = "x")
    val p = remember { Animatable(0f) }
    val glow = remember { Animatable(1f) }
    LaunchedEffect(Unit) { p.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = 220f)) } // puha, nagyot lendülő
    LaunchedEffect(Unit) { glow.animateTo(0.4f, tween(1100)) }
    val dir = if (t.id % 2L == 0L) -1f else 1f
    Box(
        Modifier
            .offset { IntOffset(x.dp.roundToPx(), (cy - cell / 2f).dp.roundToPx()) }
            .size(cell.dp)
            .graphicsLayer {
                val v = p.value
                val ph = (bob.value + slot * 0.17f) * 2f * PI.toFloat()
                scaleX = v
                scaleY = v * (1f + 0.24f * (1f - v)) // zselés összenyomódás a csúcson
                rotationZ = (1f - v) * 34f * dir + sin(ph) * 2f
                translationY = (1f - v) * (-0.75f * cell.dp.toPx()) + sin(ph) * 4.dp.toPx()
                alpha = min(1f, v * 3f).coerceAtLeast(0f)
            },
        contentAlignment = Alignment.Center,
    ) { BigChar(t.ch, t.colorIdx, cell, glow.asState()) }
}

/** Az 5 betű hullámszerűen felrepül a begépelt szövegbe (kis "lendület-vétellel"), közben kicsinyedve elhalványul. */
@Composable
private fun FlyingRow(items: List<Typed>, sw: Float, cell: Float, cy: Float) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(640, easing = LinearEasing)) }
    val ease = remember { CubicBezierEasing(0.5f, -0.4f, 0.8f, 0.6f) }
    val hy = 104f
    items.forEachIndexed { i, t ->
        val x = (sw - items.size * cell) / 2f + i * cell
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
                },
            contentAlignment = Alignment.Center,
        ) { BigChar(t.ch, t.colorIdx, cell) }
    }
}

// ---- Mókás konfetti: kevesebb, de sokféle (10 alak) és izzó ----
private class Particle(
    val vx: Float, val vy: Float, val spin: Float, val kind: Int,
    val color: Color, val size: Float, val phase: Float,
)

private fun makeParticles(b: Burst): List<Particle> {
    val rnd = Random(b.id.toInt() * 7919 + 13)
    val n = when (b.kind) { 0 -> 7; 1 -> 15; else -> 13 }
    val kinds = (0 until 10).shuffled(rnd) // egy robbanáson belül minél több különböző alak
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
private fun BurstView(b: Burst, sw: Float, cell: Float, cy: Float) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(2000, easing = LinearEasing)) }
    val parts = remember { makeParticles(b) }
    Canvas(Modifier.fillMaxSize()) {
        val s = p.value * 2f // eltelt másodperc
        val ox = (if (b.kind == 2) sw / 2f else (sw - b.total * cell) / 2f + (b.slot + 0.5f) * cell).dp.toPx()
        val oy = cy.dp.toPx()
        if (b.kind < 2) {
            val rp = (s / 0.45f).coerceIn(0f, 1f)
            if (rp < 1f) {
                val c = Offset(ox, oy)
                val gr = (50f + 110f * rp).dp.toPx()
                drawCircle( // lágy fényvillanás
                    Brush.radialGradient(listOf(KidColors[b.seed % KidColors.size].copy(alpha = 0.5f * (1f - rp)), Color.Transparent), c, gr),
                    gr, c,
                )
                drawCircle(Color.White.copy(alpha = 0.5f * (1f - rp)), (20f + 80f * rp).dp.toPx(), c, style = Stroke(3.dp.toPx()))
            }
        }
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
                val gr = q.size.dp.toPx() * 2.4f
                drawCircle(Brush.radialGradient(listOf(q.color.copy(alpha = 0.45f * aa), Color.Transparent), Offset.Zero, gr), gr, Offset.Zero) // glow
                drawShape(q, aa)
            }
        }
    }
}

private fun DrawScope.drawShape(q: Particle, a: Float) {
    val s = q.size.dp.toPx()
    when (q.kind) {
        0 -> drawRoundRect(q.color, Offset(-s * 0.5f, -s * 0.75f), Size(s, s * 1.5f), CornerRadius(s * 0.2f), alpha = a)
        1 -> drawCircle(q.color, s * 0.55f, Offset.Zero, alpha = a)
        2 -> drawPath(polyPath(10) { i -> if (i % 2 == 0) s else s * 0.45f }, q.color, alpha = a) // csillag
        3 -> { // szív
            val path = Path()
            path.moveTo(0f, s * 0.9f)
            path.cubicTo(-s * 1.4f, -s * 0.1f, -s * 0.7f, -s * 1.1f, 0f, -s * 0.4f)
            path.cubicTo(s * 0.7f, -s * 1.1f, s * 1.4f, -s * 0.1f, 0f, s * 0.9f)
            path.close()
            drawPath(path, q.color, alpha = a)
        }
        4 -> drawArc(q.color, 135f, 180f, false, Offset(-s, -s), Size(s * 2f, s * 2f), alpha = a, style = Stroke(s * 0.3f, cap = StrokeCap.Round))
        5 -> { // kunkori vonal
            val w = s * 0.55f
            val path = Path()
            path.moveTo(-w * 2f, 0f)
            path.quadraticBezierTo(-w * 1.5f, -w, -w, 0f)
            path.quadraticBezierTo(-w * 0.5f, w, 0f, 0f)
            path.quadraticBezierTo(w * 0.5f, -w, w, 0f)
            path.quadraticBezierTo(w * 1.5f, w, w * 2f, 0f)
            drawPath(path, q.color, alpha = a, style = Stroke(s * 0.3f, cap = StrokeCap.Round))
        }
        6 -> drawCircle(q.color, s * 0.6f, Offset.Zero, alpha = a, style = Stroke(s * 0.28f)) // gyűrű
        7 -> drawPath(polyPath(4) { s * 0.9f }, q.color, alpha = a) // rombusz
        8 -> drawPath(polyPath(3) { s * 0.95f }, q.color, alpha = a) // háromszög
        else -> { // négyágú szikra
            val path = Path()
            val pts = listOf(0f to -1f, 0.25f to -0.25f, 1f to 0f, 0.25f to 0.25f, 0f to 1f, -0.25f to 0.25f, -1f to 0f, -0.25f to -0.25f)
            pts.forEachIndexed { i, (px, py) ->
                if (i == 0) path.moveTo(px * s, py * s) else path.lineTo(px * s, py * s)
            }
            path.close()
            drawPath(path, q.color, alpha = a)
        }
    }
}

/** Szabályos sokszög / csillag útvonala a nullpont körül; a sugarat az index adja. */
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

// ---- Közös elemek ----
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

@Composable
private fun FunKeyboard(fontSp: Float, modifier: Modifier, onKey: (KeySpec) -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FunToolbar(Modifier.weight(0.85f).fillMaxWidth(), fontSp, onKey)
        KeyRows.forEachIndexed { ri, row ->
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { ki, k ->
                    if (k.kind == K_GAP) {
                        Spacer(Modifier.weight(k.weight))
                    } else {
                        KidKey(
                            label = k.label,
                            color = KidColors[(ri * 3 + ki) % KidColors.size],
                            fontSp = if (k.funShape) fontSp * 0.9f else fontSp,
                            funShape = k.funShape,
                            modifier = Modifier.weight(k.weight).fillMaxHeight(),
                        ) { onKey(k) }
                    }
                }
            }
        }
    }
}

/** A felső sáv: kerek emoji gombok, amik finoman hintáznak. */
@Composable
private fun FunToolbar(modifier: Modifier, fontSp: Float, onKey: (KeySpec) -> Unit) {
    val t by rememberInfiniteTransition(label = "toolbar").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "toolbarT",
    )
    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        FunTop.forEachIndexed { i, k ->
            KidKey(
                label = k.label,
                color = KidColors[(i + 2) % KidColors.size],
                fontSp = fontSp,
                funShape = true,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .graphicsLayer {
                        val ph = (t + i * 0.17f) * 2f * PI.toFloat()
                        rotationZ = sin(ph) * 7f
                        translationY = cos(ph) * 2.dp.toPx()
                    },
            ) { onKey(k) }
        }
    }
}

@Composable
private fun KidKey(label: String, color: Color, fontSp: Float, modifier: Modifier, funShape: Boolean = false, onDown: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    // Puha, rugalmas "lenyomódás": túllendül, majd visszarugózik
    val squish by animateFloatAsState(
        if (pressed) 1f else 0f,
        spring(dampingRatio = 0.4f, stiffness = 550f),
        label = "squish",
    )
    val corner = if (funShape) 40.dp else 14.4.dp
    val bg = if (funShape) color.copy(alpha = 0.3f) else color
    val fg = if (funShape) color else Background
    Box(
        modifier
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true
                    onDown()
                    tryAwaitRelease()
                    pressed = false
                })
            }
            .drawBehind {
                val dy = (4.dp - 3.dp * squish).toPx()
                drawRoundRect(
                    Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(0f, dy),
                    size = size,
                    cornerRadius = CornerRadius(corner.toPx()),
                )
                if (squish > 0.01f) { // lenyomáskor a gomb színe felizzik
                    val r = size.maxDimension * 0.8f
                    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.6f * squish), Color.Transparent), center, r), r, center)
                }
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
    }
}
