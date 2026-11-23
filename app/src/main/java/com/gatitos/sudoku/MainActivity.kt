package com.gatitos.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag

private fun vibrate(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 60, 50, 90), -1))
}

private val Fondo = Color(0xFFFFF6EE)
private val Rosa = Color(0xFFF48FB1)
private val RosaOscuro = Color(0xFFC2185B)
private val Cafe = Color(0xFF5A4A42)
private val Linea = Color(0xFF8D6E63)

private val colores = lightColorScheme(
    primary = RosaOscuro,
    onPrimary = Color.White,
    secondary = Rosa,
    background = Fondo,
    surface = Color.White,
    onSurface = Cafe,
    onBackground = Cafe,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = colores) {
                val vm: GameViewModel = viewModel()
                Surface(Modifier.fillMaxSize(), color = Fondo) {
                    Box(Modifier.safeDrawingPadding()) {
                        when (vm.screen) {
                            Screen.MENU -> MenuScreen(vm)
                            Screen.GAME -> GameScreen(vm)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Menú

@Composable
fun MenuScreen(vm: GameViewModel) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-14).dp)) {
            listOf(3, 1, 5).forEach { CatFace(it, Modifier.size(84.dp)) }
        }
        Spacer(Modifier.height(12.dp))
        Text("Sudoku Gatitos", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = RosaOscuro)
        Text("Sin anuncios, solo michis 🐾", color = Cafe)
        Spacer(Modifier.height(28.dp))

        if (vm.hasGame && vm.status == Status.PLAYING) {
            Button(
                onClick = vm::continueGame,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
            ) { Text("Continuar (${vm.difficulty.label})", fontSize = 18.sp) }
            Spacer(Modifier.height(18.dp))
        }

        Text("Elige la dificultad", fontWeight = FontWeight.SemiBold, color = Cafe)
        Spacer(Modifier.height(8.dp))
        Difficulty.entries.forEach { d ->
            val best = vm.bestTime(d)
            OutlinedButton(
                onClick = { vm.newGame(d) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
            ) {
                Text("${d.emoji}  ${d.label}", fontSize = 18.sp, color = Cafe, modifier = Modifier.weight(1f))
                if (best >= 0) Text("🏆 ${formatTime(best)}", color = Linea, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(20.dp))
        Option("🔢  Mostrar números en los gatitos", vm.showNumbers, "opcion_numeros", vm::toggleNumbers)
        Option("🔊  Sonidos (ronroneo, gruñido, maullido)", vm.soundOn, "opcion_sonido", vm::toggleSound)
        Option("📳  Vibrar al equivocarse", vm.vibrationOn, "opcion_vibracion", vm::toggleVibration)
    }
}

@Composable
private fun Option(label: String, checked: Boolean, tag: String, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Cafe, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onToggle() }, modifier = Modifier.testTag(tag))
    }
}

// ---------------------------------------------------------------- Juego

@Composable
fun GameScreen(vm: GameViewModel) {
    BackHandler { vm.goMenu() }

    // Cronómetro: solo corre con la app en primer plano
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val running = lifecycle.isAtLeast(Lifecycle.State.RESUMED) && vm.status == Status.PLAYING && !vm.loading
    LaunchedEffect(running) {
        while (running) {
            delay(1000)
            vm.tick()
        }
    }

    // Al equivocarse: vibra el teléfono y se sacude la celda
    val context = LocalContext.current
    val shake = remember { Animatable(0f) }
    var handledErrors by remember { mutableIntStateOf(vm.errorTick) }
    LaunchedEffect(vm.errorTick) {
        if (vm.errorTick == handledErrors) return@LaunchedEffect
        handledErrors = vm.errorTick
        if (vm.vibrationOn) vibrate(context)
        for (x in listOf(22f, -22f, 16f, -16f, 8f, -8f, 0f)) shake.animateTo(x, tween(35))
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        // Barra superior
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = vm::goMenu) { Text("← Menú", color = Cafe, fontSize = 16.sp) }
            Spacer(Modifier.weight(1f))
            Text(vm.difficulty.label, fontWeight = FontWeight.Bold, color = RosaOscuro, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            Text("⏱ ${formatTime(vm.seconds)}", color = Cafe, fontSize = 16.sp)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Center) {
            Text("Vidas: ", color = Cafe)
            repeat(MAX_MISTAKES) { i ->
                Text(if (i < MAX_MISTAKES - vm.mistakes) "❤️" else "🤍")
            }
        }

        if (vm.loading) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Reuniendo a los gatitos…", color = Cafe)
                }
            }
            return@Column
        }

        Spacer(Modifier.height(4.dp))
        Board(vm, Modifier.fillMaxWidth().aspectRatio(1f), shakeX = { shake.value })
        Spacer(Modifier.weight(1f))
        Actions(vm)
        Spacer(Modifier.height(10.dp))
        Palette(vm)
        Spacer(Modifier.height(8.dp))
    }

    when (vm.status) {
        Status.WON -> AlertDialog(
            onDismissRequest = {},
            icon = { CatFace(1, Modifier.size(64.dp)) },
            title = { Text("¡Miau! ¡Lo lograste!", textAlign = TextAlign.Center) },
            text = {
                Text(
                    "Tiempo: ${formatTime(vm.seconds)}" + if (vm.newRecord) "\n🏆 ¡Nuevo récord!" else "",
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = { Button(onClick = { vm.newGame(vm.difficulty) }) { Text("Otra partida") } },
            dismissButton = { TextButton(onClick = vm::goMenu) { Text("Menú") } },
        )
        Status.LOST -> AlertDialog(
            onDismissRequest = {},
            icon = { CatFace(3, Modifier.size(64.dp)) },
            title = { Text("Ups… se acabaron las vidas") },
            text = { Text("Los gatitos se enredaron. ¿Qué quieres hacer?") },
            confirmButton = { Button(onClick = vm::oneMoreLife) { Text("Una vida más") } },
            dismissButton = {
                Row {
                    TextButton(onClick = vm::restart) { Text("Reiniciar") }
                    TextButton(onClick = { vm.newGame(vm.difficulty) }) { Text("Nueva") }
                }
            },
        )
        Status.PLAYING -> Unit
    }
}

@Composable
fun Board(vm: GameViewModel, modifier: Modifier, shakeX: () -> Float = { 0f }) {
    val sel = vm.selected
    val selValue = if (sel >= 0) vm.board[sel] else 0
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(Color.White)) {
        Column(Modifier.fillMaxSize()) {
            for (r in 0 until 9) {
                Row(Modifier.weight(1f)) {
                    for (c in 0 until 9) {
                        val i = r * 9 + c
                        val v = vm.board[i]
                        val related = sel >= 0 && (sel / 9 == r || sel % 9 == c ||
                            (sel / 27 == r / 3 && sel % 9 / 3 == c / 3))
                        val bg = when {
                            i == sel -> Color(0xFFF8BBD0)
                            vm.isWrong(i) -> Color(0xFFECE7E4)
                            v != 0 && v == selValue -> Color(0xFFFCE4EC)
                            related -> Color(0xFFFFF0F5)
                            else -> Color.White
                        }
                        Box(
                            Modifier.weight(1f).fillMaxHeight().testTag("cell_$i").background(bg).clickable { vm.select(i) }
                                .graphicsLayer { translationX = if (i == sel) shakeX() else 0f },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (v != 0) {
                                CatFace(v, Modifier.fillMaxSize().padding(2.dp), gray = vm.isWrong(i))
                                if (vm.isWrong(i)) Text("✕", color = Color(0xFFD32F2F), fontSize = 10.sp,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 2.dp))
                                if (vm.showNumbers) Text("$v", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                    color = if (vm.fixed[i]) Cafe else RosaOscuro,
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp))
                                if (!vm.fixed[i] && !vm.isWrong(i)) Box(
                                    Modifier.align(Alignment.BottomStart).padding(2.dp).size(4.dp)
                                        .clip(RoundedCornerShape(2.dp)).background(Rosa)
                                )
                            } else if (vm.notes[i].isNotEmpty()) {
                                Notes(vm.notes[i])
                            }
                        }
                    }
                }
            }
        }
        // Líneas de la cuadrícula
        Canvas(Modifier.fillMaxSize()) {
            val step = size.width / 9
            for (k in 0..9) {
                val thick = k % 3 == 0
                val w = if (thick) 2.5.dp.toPx() else 0.7.dp.toPx()
                val col = if (thick) Linea else Color(0xFFD7CCC8)
                drawLine(col, Offset(k * step, 0f), Offset(k * step, size.height), w)
                drawLine(col, Offset(0f, k * step), Offset(size.width, k * step), w)
            }
        }
    }
}

@Composable
private fun Notes(notes: Set<Int>) {
    Column(Modifier.fillMaxSize().padding(1.dp)) {
        for (r in 0 until 3) Row(Modifier.weight(1f)) {
            for (c in 0 until 3) {
                val v = r * 3 + c + 1
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if (v in notes) CatFace(v, Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun Actions(vm: GameViewModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        ActionButton("↩️", "Deshacer", onClick = vm::undo)
        ActionButton("🧽", "Borrar", onClick = vm::erase)
        ActionButton("✏️", if (vm.notesMode) "Notas: sí" else "Notas: no", active = vm.notesMode) {
            vm.notesMode = !vm.notesMode
        }
        ActionButton("💡", "Pista (${vm.hintsLeft})", enabled = vm.hintsLeft > 0, onClick = vm::hint)
    }
}

@Composable
private fun ActionButton(icon: String, label: String, active: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(14.dp))
            .background(if (active) Color(0xFFF8BBD0) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, fontSize = 22.sp)
        Text(label, fontSize = 12.sp, color = Cafe)
    }
}

@Composable
private fun Palette(vm: GameViewModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (v in 1..9) {
            val left = 9 - vm.placedCount(v)
            val done = left <= 0
            Column(
                Modifier.weight(1f)
                    .testTag("cat_$v")
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable(enabled = !done) { vm.input(v) }
                    .alpha(if (done) 0.25f else 1f)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CatFace(v, Modifier.fillMaxWidth().aspectRatio(1f).padding(horizontal = 2.dp))
                Text(
                    if (vm.showNumbers) "$v·$left" else "$left",
                    fontSize = 11.sp, color = Linea,
                )
            }
        }
    }
}
