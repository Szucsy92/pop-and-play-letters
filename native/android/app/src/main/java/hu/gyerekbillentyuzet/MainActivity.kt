@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package hu.gyerekbillentyuzet

import android.graphics.Typeface
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.gyerekbillentyuzet.core.audio.NativeSoundEngine
import hu.gyerekbillentyuzet.feature.keypad.Bit
import hu.gyerekbillentyuzet.feature.keypad.KeypadViewModel
import hu.gyerekbillentyuzet.feature.keypad.Shown
import hu.gyerekbillentyuzet.feature.lock.ChildLockController
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

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
private val Rows = listOf(
    "1234567890".map { it.toString() },
    "QWERTZUIOP".map { it.toString() },
    "ASDFGHJKLÉ".map { it.toString() },
    "YXCVBNMÖÜÁ".map { it.toString() },
    listOf("?", "!", "+", "-", "★", "♥", "☺", "."),
)

// A web Fredoka betűjéhez legközelebbi kerekített rendszerbetű.
private val KidFont: FontFamily = try {
    FontFamily(Typeface.create("sans-serif-rounded", Typeface.BOLD))
} catch (_: Throwable) {
    FontFamily.SansSerif
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

// ---- Animációs segédek (CSS keyframes portja) ----
private val KidEase = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.2f)
private val FlyEase = CubicBezierEasing(0f, 0f, 0.58f, 1f)

private fun track(t: Float, vararg stops: Pair<Float, Float>): Float {
    if (t <= stops.first().first) return stops.first().second
    for (i in 1 until stops.size) {
        val (t1, v1) = stops[i]
        if (t <= t1) {
            val (t0, v0) = stops[i - 1]
            val local = (t - t0) / (t1 - t0)
            return v0 + (v1 - v0) * KidEase.transform(local)
        }
    }
    return stops.last().second
}

@Composable
fun KeypadApp(vm: KeypadViewModel = viewModel()) {
    val st by vm.state.collectAsState()
    val activity = androidx.compose.ui.platform.LocalContext.current as? MainActivity
    val sound = remember { NativeSoundEngine() }
    val lock = remember { ChildLockController { vm.showMenu() } }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var input by remember { mutableStateOf("") }

    val onPress: (String) -> Unit = { ch ->
        if (ch.isNotEmpty() && ch != " ") {
            vm.press(ch)
            if (vm.state.value.soundEnabled) {
                val seed = ch.first().code
                sound.play(seed, seed % 3 == 0)
            }
        }
    }

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

    BoxWithConstraints(
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
        val screenH = maxHeight
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

        // Színpad (a nagy betű és a konfetti)
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .then(if (inner) Modifier.fillMaxHeight(0.54f) else Modifier.fillMaxHeight()),
            contentAlignment = Alignment.Center,
        ) {
            val shown = st.shown
            if (shown != null) {
                BitsCanvas(st.bits, shown.id)
                StageChar(shown, min(screenW.value * 0.55f, screenH.value * 0.38f), screenH.value)
            }
        }

        // Telefon billentyűzete: rejtett beviteli mező
        if (st.mode == "native" && !st.parentMenu) {
            BasicTextField(
                value = input,
                onValueChange = { v ->
                    if (v.isNotEmpty()) onPress(v.last().toString())
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

        // Nagy színes gombok
        if (inner) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.46f)
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val keySp = (screenW.value * 0.05f).coerceIn(16f, 32f)
                Rows.forEachIndexed { ri, row ->
                    Row(
                        Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        row.forEachIndexed { ki, k ->
                            KidKey(
                                label = k,
                                color = KidColors[(ri * 3 + ki) % KidColors.size],
                                fontSp = keySp,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            ) { onPress(k) }
                        }
                    }
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
private fun KidText(text: String, size: androidx.compose.ui.unit.TextUnit, color: Color, center: Boolean = false) {
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
private fun KidKey(label: String, color: Color, fontSp: Float, modifier: Modifier, onDown: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
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
                val dy = (if (pressed) 1.dp else 4.dp).toPx()
                drawRoundRect(
                    Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(0f, dy),
                    size = size,
                    cornerRadius = CornerRadius(14.4.dp.toPx()),
                )
            }
            .graphicsLayer {
                translationY = if (pressed) 3.dp.toPx() else 0f
                scaleX = if (pressed) 0.96f else 1f
                scaleY = if (pressed) 0.96f else 1f
            }
            .clip(RoundedCornerShape(14.4.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Background, fontSize = fontSp.sp, fontWeight = FontWeight.Bold, fontFamily = KidFont)
    }
}

@Composable
private fun StageChar(shown: Shown, sizeDp: Float, screenHDp: Float) {
    val p = remember(shown.id) { Animatable(0f) }
    LaunchedEffect(shown.id) { p.animateTo(1f, tween(1600, easing = LinearEasing)) }
    val density = LocalDensity.current
    val shadowPx = with(density) { (sizeDp * 0.06f).dp.toPx() }
    Text(
        shown.ch,
        modifier = Modifier.graphicsLayer {
            val t = p.value
            val h = sizeDp.dp.toPx()
            var scale = 1f
            var scaleY = 1f
            var rot = 0f
            var ty = 0f
            val alpha: Float
            when (shown.anim) {
                0 -> {
                    scale = track(t, 0f to 0f, 0.2f to 1.15f, 0.35f to 1f, 1f to 0.6f)
                    alpha = track(t, 0f to 0f, 0.2f to 1f, 0.75f to 1f, 1f to 0f)
                }
                1 -> {
                    scale = track(t, 0f to 0f, 0.2f to 1f, 0.75f to 1f, 1f to 0.7f)
                    rot = track(t, 0f to 0f, 0.2f to -10f, 0.35f to 8f, 0.5f to -4f, 0.75f to 0f, 1f to 0f)
                    alpha = track(t, 0f to 1f, 0.75f to 1f, 1f to 0f)
                }
                2 -> {
                    val fall = track(t, 0f to -0.6f * screenHDp.dp.toPx(), 0.25f to 0f)
                    val bounce = track(t, 0.25f to 0f, 0.35f to -0.08f, 0.45f to 0f, 1f to 0.10f) * h
                    ty = fall + bounce
                    scaleY = track(t, 0f to 1f, 0.25f to 0.85f, 0.35f to 1f, 1f to 1f)
                    alpha = track(t, 0f to 1f, 0.75f to 1f, 1f to 0f)
                }
                else -> {
                    rot = track(t, 0f to -200f, 0.3f to 0f, 1f to 0f)
                    scale = track(t, 0f to 0f, 0.3f to 1f, 1f to 1.3f)
                    alpha = track(t, 0f to 1f, 0.75f to 1f, 1f to 0f)
                }
            }
            scaleX = scale
            this.scaleY = scale * scaleY
            rotationZ = rot
            translationY = ty
            this.alpha = alpha.coerceIn(0f, 1f)
        },
        style = TextStyle(
            color = KidColors[shown.colorIdx],
            fontSize = sizeDp.sp,
            lineHeight = sizeDp.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = KidFont,
            shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, shadowPx), 0f),
        ),
    )
}

@Composable
private fun BitsCanvas(bits: List<Bit>, token: Long) {
    val p = remember(token) { Animatable(0f) }
    LaunchedEffect(token) { p.animateTo(1f, tween(1400, easing = FlyEase)) }
    Canvas(Modifier.fillMaxSize()) {
        val e = p.value
        val center = Offset(size.width / 2f, size.height / 2f)
        val a = (1f - e).coerceIn(0f, 1f)
        for (b in bits) {
            val pos = center + Offset(b.dx.dp.toPx() * e, (b.dy + 80f).dp.toPx() * e)
            val color = KidColors[b.colorIdx]
            translate(pos.x, pos.y) {
                rotate(b.r * e, Offset.Zero) {
                    if (b.curl) {
                        val d = 35.dp.toPx()
                        drawArc(
                            color = color,
                            startAngle = 135f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(-d / 2f, -d / 2f),
                            size = Size(d, d),
                            alpha = a,
                            style = Stroke(width = 5.dp.toPx()),
                        )
                    } else {
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(-6.dp.toPx(), -9.dp.toPx()),
                            size = Size(12.dp.toPx(), 18.dp.toPx()),
                            cornerRadius = CornerRadius(3.dp.toPx()),
                            alpha = a,
                        )
                    }
                }
            }
        }
    }
}
