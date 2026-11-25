package com.gatitos.sudoku

import android.os.SystemClock
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Juega una partida despacio para grabar el GIF de la demo. Sólo corre si se pide:
 *   adb shell am instrument -w -e demo true -e class com.gatitos.sudoku.DemoRecording \
 *     com.gatitos.sudoku.debug.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
class DemoRecording {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val vm: GameViewModel
        get() = ViewModelProvider(rule.activity)[GameViewModel::class.java]

    @Before
    fun soloSiSePide() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("demo") == "true")
    }

    /** Deja correr el reloj de Compose en tiempo real durante [ms] (animaciones incluidas). */
    private fun pause(ms: Long) {
        val step = 32L
        var left = ms
        while (left > 0) {
            rule.mainClock.advanceTimeBy(step)
            SystemClock.sleep(step)
            left -= step
        }
    }

    private fun tapCell(i: Int) = rule.onNodeWithTag("cell_$i").performClick()
    private fun tapCat(v: Int) = rule.onNodeWithTag("cat_$v").performClick()
    private fun empties() = (0 until 81).filter { vm.board[it] == 0 }

    @Test
    fun demo() {
        rule.mainClock.autoAdvance = false
        pause(2000)
        rule.onNodeWithText("${Difficulty.FACIL.emoji}  ${Difficulty.FACIL.label}").performClick()
        rule.mainClock.autoAdvance = true
        rule.waitUntil(20_000) { !vm.loading && vm.hasGame }
        rule.mainClock.autoAdvance = false
        pause(1200)

        // Algunos gatitos bien puestos
        empties().take(4).forEach { i ->
            tapCell(i); pause(450)
            tapCat(vm.solutionAt(i)); pause(650)
        }

        // Un error: vibra, se sacude y queda gris
        val e = empties().first()
        tapCell(e); pause(450)
        tapCat((1..9).first { it != vm.solutionAt(e) && vm.placedCount(it) < 9 }); pause(1800)
        tapCat(vm.solutionAt(e)); pause(900)

        // Notas
        val n = empties().first()
        tapCell(n); pause(400)
        rule.onNodeWithText("Notas: no").performClick(); pause(400)
        tapCat(vm.solutionAt(n)); pause(350)
        tapCat(if (vm.solutionAt(n) == 9) 1 else vm.solutionAt(n) + 1); pause(1200)
        rule.onNodeWithText("Notas: sí").performClick(); pause(400)

        // Pista
        rule.onNodeWithText("Pista (3)").performClick(); pause(1300)

        // Completar el resto rápido hasta ganar
        empties().forEach { i ->
            tapCell(i); pause(70)
            tapCat(vm.solutionAt(i)); pause(90)
        }
        pause(3500)
    }
}
