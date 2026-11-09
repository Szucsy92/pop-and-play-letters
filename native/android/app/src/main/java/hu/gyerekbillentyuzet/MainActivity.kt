package hu.gyerekbillentyuzet

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.gyerekbillentyuzet.core.audio.NativeSoundEngine
import hu.gyerekbillentyuzet.feature.keypad.KeypadViewModel
import hu.gyerekbillentyuzet.feature.lock.ChildLockController
import kotlinx.coroutines.delay
import kotlin.random.Random

private val palette = listOf(Color(0xFFFF6B6B), Color(0xFFFFC857), Color(0xFF4ECDC4), Color(0xFF5B8DEF), Color(0xFFB084CC), Color(0xFFFF8FAB))
private val rows = listOf("1234567890", "QWERTZUIOP", "ASDFGHJKLÉ", "YXCVBNMÖÜÁ", "?!+-★♥☺.")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        setContent { MaterialTheme { KeypadApp() } }
    }
    fun pinApp() { try { startLockTask() } catch (_: Exception) {} }
    fun unpinApp() { try { stopLockTask() } catch (_: Exception) {} }
}

@Composable
fun KeypadApp(vm: KeypadViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? MainActivity
    val sound = remember { NativeSoundEngine() }
    val lock = remember { ChildLockController { vm.showMenu() } }
    var input by remember { mutableStateOf("") }
    val keyboardFocus = remember { FocusRequester() }
    LaunchedEffect(state.mode, state.parentMenu) { if (state.mode == "native" && !state.parentMenu) { delay(250); keyboardFocus.requestFocus() } }
    BackHandler(enabled = !state.parentMenu && !state.exited) { /* prevent accidental exit */ }
    Box(Modifier.fillMaxSize().background(Color(0xFF171923))) {
        if (state.exited) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment=Alignment.CenterHorizontally) {
                Text("Viszlát! 👋", color=Color.White, fontSize=32.sp, fontWeight=FontWeight.Bold)
                Button(onClick=vm::restart) { Text("Újra indítás") }
            }
        } else {
            Box(Modifier.fillMaxWidth().fillMaxHeight(if (state.mode=="inner") .54f else 1f).align(if(state.mode=="inner") Alignment.TopCenter else Alignment.Center)
                .clickable(enabled=state.mode=="native") { /* text field receives keyboard focus below */ }, contentAlignment=Alignment.Center) {
                ConfettiCanvas(state.characterId)
                if (state.character.isNotEmpty()) {
                    key(state.characterId) {
                        var scale by remember { mutableFloatStateOf(.25f) }
                        LaunchedEffect(Unit) { scale=1.15f; delay(250); scale=1f }
                        Text(state.character, fontSize=(130*scale).sp, color=palette[state.character.first().code%palette.size], fontWeight=FontWeight.Black)
                    }
                }
            }
            if (state.mode=="inner") {
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.46f).padding(7.dp), verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    rows.forEachIndexed { ri, row ->
                        Row(Modifier.weight(1f), horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            row.forEachIndexed { ki, ch ->
                                Button(onClick={ vm.press(ch.toString()); if(state.soundEnabled) sound.play(ch.code, ch.code%3==0) },
                                    modifier=Modifier.weight(1f).fillMaxHeight(), contentPadding=PaddingValues(0.dp),
                                    colors=ButtonDefaults.buttonColors(containerColor=palette[(ri*3+ki)%palette.size])) {
                                    Text(ch.toString(), color=Color(0xFF171923), fontSize=if(row.length>9) 18.sp else 20.sp, fontWeight=FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                BasicTextField(value=input, onValueChange={ value -> if(value.isNotEmpty()) { val c=value.last().toString(); vm.press(c); if(state.soundEnabled) sound.play(c.first().code,c.first().code%3==0); input="" } },
                    modifier=Modifier.size(1.dp).align(Alignment.BottomStart).focusRequester(keyboardFocus), keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Characters))
            }
            TextButton(onClick={ lock.tap() }, modifier=Modifier.align(Alignment.TopEnd).padding(8.dp)) { Text("🔒", fontSize=28.sp, color=Color.White.copy(alpha=.55f)) }
        }
        if(state.parentMenu) {
            Surface(Modifier.fillMaxSize(), color=Color(0xF2171923)) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center) {
                    Text("Szülői menü", fontSize=30.sp, color=Color.White, fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(18.dp))
                    Button(onClick={ vm.setMode(if(state.mode=="inner") "native" else "inner") }) { Text("Billentyűzet: ${if(state.mode=="inner") "nagy gombok" else "telefoné"} ↔") }
                    Button(onClick={ vm.setSound(!state.soundEnabled) }) { Text("Hang: ${if(state.soundEnabled) "be 🔊" else "ki 🔇"}") }
                    Button(onClick={ activity?.pinApp() }) { Text("Android képernyőrögzítés") }
                    Button(onClick=vm::closeMenu) { Text("Vissza a játékhoz") }
                    Button(onClick={ activity?.unpinApp(); vm.exit() }, colors=ButtonDefaults.buttonColors(containerColor=Color.Red)) { Text("Kilépés") }
                }
            }
        }
    }
}

@Composable
private fun ConfettiCanvas(seed: Long) {
    Canvas(Modifier.fillMaxSize()) {
        if (seed == 0L) return@Canvas
        val rnd = Random(seed)
        repeat(28) { i ->
            val x = size.width * (.2f + rnd.nextFloat()*.6f)
            val y = size.height * (.2f + rnd.nextFloat()*.6f)
            val c = palette[i%palette.size]
            drawCircle(c, radius=2.5f+rnd.nextFloat()*5f, center=androidx.compose.ui.geometry.Offset(x,y))
        }
    }
}
