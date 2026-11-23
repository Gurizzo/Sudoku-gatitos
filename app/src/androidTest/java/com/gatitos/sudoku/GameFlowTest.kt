package com.gatitos.sudoku

import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Pruebas que se ejecutan en el celular, tocando la app de verdad. */
@RunWith(AndroidJUnit4::class)
class GameFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val vm: GameViewModel
        get() = ViewModelProvider(rule.activity)[GameViewModel::class.java]

    // ------------------------------------------------------------ ayudas

    private fun capture(name: String) {
        rule.waitForIdle()
        SystemClock.sleep(400)
        val bmp = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        val dir = File(rule.activity.getExternalFilesDir(null), "capturas").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun startGame(d: Difficulty) {
        rule.onNodeWithText("${d.emoji}  ${d.label}").performClick()
        rule.waitUntil(20_000) { !vm.loading && vm.hasGame }
        rule.waitForIdle()
    }

    private fun tapCell(i: Int) = rule.onNodeWithTag("cell_$i").performClick()
    private fun tapCat(v: Int) = rule.onNodeWithTag("cat_$v").performClick()

    private fun firstEmpty(): Int = (0 until 81).first { vm.board[it] == 0 }

    /** Un gatito equivocado para la celda i que todavía se pueda elegir en la paleta. */
    private fun wrongCats(i: Int) = (1..9).filter { it != vm.solutionAt(i) && vm.placedCount(it) < 9 }

    // ------------------------------------------------------------ pruebas

    @Test
    fun menuMuestraLasCuatroDificultades() {
        Difficulty.entries.forEach { rule.onNodeWithText("${it.emoji}  ${it.label}").assertIsDisplayed() }
        rule.onNodeWithText("Mostrar números en los gatitos", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Sonidos", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Vibrar al equivocarse", substring = true).assertIsDisplayed()
        capture("01_menu")
    }

    @Test
    fun cadaDificultadGeneraUnTableroValidoYResoluble() {
        for (d in Difficulty.entries) {
            startGame(d)
            val puzzle = IntArray(81) { vm.board[it] }
            for (i in 0 until 81) {
                if (puzzle[i] != 0) {
                    assertTrue(vm.fixed[i])
                    assertEquals(vm.solutionAt(i), puzzle[i])
                }
            }
            assertEquals("${d.label}: solución única", 1, SudokuGenerator.countSolutions(puzzle.copyOf(), 2))
            if (d.logicOnly) assertTrue("${d.label}: sin adivinar", SudokuGenerator.solvableByLogic(puzzle))
            Log.i("GatitosTest", "${d.label}: ${puzzle.count { it == 0 }} casillas vacías")
            capture("02_tablero_${d.name.lowercase()}")
            rule.onNodeWithText("← Menú").performClick()
            rule.waitForIdle()
        }
    }

    @Test
    fun generarEsRapidoEnElCelular() {
        for (d in Difficulty.entries) {
            repeat(5) {
                val t0 = SystemClock.elapsedRealtime()
                SudokuGenerator.generate(d)
                val ms = SystemClock.elapsedRealtime() - t0
                Log.i("GatitosTest", "${d.label}: generado en $ms ms")
                assertTrue("${d.label} tardó $ms ms", ms < 3000)
            }
        }
    }

    @Test
    fun cadaPartidaEsDistinta() {
        val vistos = HashSet<String>()
        repeat(5) {
            startGame(Difficulty.MEDIO)
            vistos += (0 until 81).joinToString("") { vm.board[it].toString() }
            rule.onNodeWithText("← Menú").performClick()
            rule.waitForIdle()
        }
        assertEquals(5, vistos.size)
    }

    @Test
    fun gatitoCorrectoSeColocaSinCastigo() {
        startGame(Difficulty.FACIL)
        val i = firstEmpty()
        tapCell(i)
        tapCat(vm.solutionAt(i))
        rule.waitForIdle()
        assertEquals(vm.solutionAt(i), vm.board[i])
        assertFalse(vm.isWrong(i))
        assertEquals(0, vm.mistakes)
        assertEquals(0, vm.errorTick)
    }

    @Test
    fun gatitoEquivocadoVibraQuedaGrisYQuitaVida() {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            rule.activity.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION") rule.activity.getSystemService(Vibrator::class.java)
        }
        assertTrue("el celular debe tener vibrador", vibrator.hasVibrator())

        startGame(Difficulty.FACIL)
        val i = firstEmpty()
        val wrong = wrongCats(i).first()
        tapCell(i)
        tapCat(wrong)
        rule.waitForIdle()

        assertEquals("el gatito se queda en la casilla", wrong, vm.board[i])
        assertTrue("marcado como error (se dibuja gris)", vm.isWrong(i))
        assertEquals(1, vm.mistakes)
        assertEquals("dispara vibración + sacudida", 1, vm.errorTick)
        assertEquals("ronronea al perder una vida", CatSound.RONRONEO, vm.lastSound)
        capture("03_gatito_equivocado_gris")

        // Se puede corregir encima
        tapCat(vm.solutionAt(i))
        rule.waitForIdle()
        assertFalse(vm.isWrong(i))
        assertEquals(1, vm.mistakes)
    }

    @Test
    fun sonidoYVibracionSePuedenDesactivar() {
        assertTrue(vm.soundOn && vm.vibrationOn)
        rule.onNodeWithTag("opcion_sonido").performClick()
        rule.onNodeWithTag("opcion_vibracion").performClick()
        rule.waitForIdle()
        try {
            assertFalse(vm.soundOn)
            assertFalse(vm.vibrationOn)
            capture("07_opciones_desactivadas")
            startGame(Difficulty.FACIL)
            val i = firstEmpty()
            tapCell(i)
            tapCat(wrongCats(i).first())
            rule.waitForIdle()
            assertEquals(1, vm.mistakes)
            assertEquals("no suena nada", null, vm.lastSound)
        } finally {
            if (!vm.soundOn) vm.toggleSound()
            if (!vm.vibrationOn) vm.toggleVibration()
        }
    }

    @Test
    fun tresErroresMuestranDerrotaYSePuedeSeguir() {
        startGame(Difficulty.FACIL)
        val i = firstEmpty()
        tapCell(i)
        wrongCats(i).take(3).forEach { tapCat(it) }
        rule.waitForIdle()
        assertEquals(Status.LOST, vm.status)
        assertEquals("gruñe al perder", CatSound.GRUNIDO, vm.lastSound)
        rule.onNodeWithText("Ups… se acabaron las vidas").assertIsDisplayed()
        capture("04_sin_vidas")

        rule.onNodeWithText("Una vida más").performClick()
        rule.waitForIdle()
        assertEquals(Status.PLAYING, vm.status)
        assertEquals(MAX_MISTAKES - 1, vm.mistakes)
    }

    @Test
    fun notasDeshacerBorrarYPista() {
        startGame(Difficulty.MEDIO)
        val i = firstEmpty()
        tapCell(i)

        rule.onNodeWithText("Notas: no").performClick()
        tapCat(1); tapCat(2)
        rule.waitForIdle()
        assertEquals(setOf(1, 2), vm.notes[i])
        capture("05_notas")
        rule.onNodeWithText("Deshacer").performClick()
        rule.waitForIdle()
        assertEquals(setOf(1), vm.notes[i])
        rule.onNodeWithText("Notas: sí").performClick()

        val wrong = wrongCats(i).first()
        tapCat(wrong)
        rule.onNodeWithText("Borrar").performClick()
        rule.waitForIdle()
        assertEquals(0, vm.board[i])

        rule.onNodeWithText("Pista (3)").performClick()
        rule.waitForIdle()
        assertEquals(vm.solutionAt(i), vm.board[i])
        assertTrue(vm.fixed[i])
        assertEquals(2, vm.hintsLeft)
    }

    @Test
    fun completarElTableroMuestraVictoria() {
        startGame(Difficulty.FACIL)
        for (i in 0 until 81) {
            if (vm.board[i] != 0) continue
            tapCell(i)
            tapCat(vm.solutionAt(i))
        }
        rule.waitForIdle()
        assertEquals(Status.WON, vm.status)
        assertEquals("maúlla al ganar", CatSound.MAULLIDO, vm.lastSound)
        rule.onNodeWithText("¡Miau! ¡Lo lograste!").assertIsDisplayed()
        capture("06_victoria")

        rule.onNodeWithText("Otra partida").performClick()
        rule.waitUntil(20_000) { !vm.loading && vm.status == Status.PLAYING }
    }
}
