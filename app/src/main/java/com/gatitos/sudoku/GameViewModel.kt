package com.gatitos.sudoku

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { MENU, GAME }
enum class Status { PLAYING, WON, LOST }

const val MAX_MISTAKES = 3
const val MAX_HINTS = 3

private data class Move(val idx: Int, val value: Int, val notes: Set<Int>)

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("gatitos", Context.MODE_PRIVATE)

    var screen by mutableStateOf(Screen.MENU)
        private set
    var loading by mutableStateOf(false)
        private set
    var hasGame by mutableStateOf(false)
        private set
    var difficulty by mutableStateOf(Difficulty.FACIL)
        private set
    var status by mutableStateOf(Status.PLAYING)
        private set

    private var solution = IntArray(81)
    /** true = celda fija (dada por el puzzle o revelada con pista). */
    val fixed = mutableStateListOf<Boolean>().apply { repeat(81) { add(false) } }
    val board = mutableStateListOf<Int>().apply { repeat(81) { add(0) } }
    val notes = mutableStateListOf<Set<Int>>().apply { repeat(81) { add(emptySet()) } }

    var selected by mutableIntStateOf(-1)
    var notesMode by mutableStateOf(false)
    var mistakes by mutableIntStateOf(0)
        private set
    var hintsLeft by mutableIntStateOf(MAX_HINTS)
        private set
    var seconds by mutableIntStateOf(0)
        private set
    var showNumbers by mutableStateOf(prefs.getBoolean("numeros", false))
        private set
    var newRecord by mutableStateOf(false)
        private set
    /** Aumenta cada vez que se coloca un gatito equivocado (la UI vibra y sacude la celda). */
    var errorTick by mutableIntStateOf(0)
        private set

    private val undoStack = ArrayDeque<Move>()

    var soundOn by mutableStateOf(prefs.getBoolean("sonido", true))
        private set
    var vibrationOn by mutableStateOf(prefs.getBoolean("vibracion", true))
        private set
    /** Último sonido que se pidió reproducir (lo usan las pruebas). */
    var lastSound by mutableStateOf<CatSound?>(null)
        private set

    private val sounds = CatSounds(app)

    private fun play(s: CatSound) {
        if (!soundOn) return
        lastSound = s
        sounds.play(s)
    }

    override fun onCleared() = sounds.release()

    fun toggleSound() {
        soundOn = !soundOn
        prefs.edit().putBoolean("sonido", soundOn).apply()
    }

    fun toggleVibration() {
        vibrationOn = !vibrationOn
        prefs.edit().putBoolean("vibracion", vibrationOn).apply()
    }

    fun toggleNumbers() {
        showNumbers = !showNumbers
        prefs.edit().putBoolean("numeros", showNumbers).apply()
    }

    fun bestTime(d: Difficulty): Int = prefs.getInt("record_${d.name}", -1)

    fun goMenu() { screen = Screen.MENU }
    fun continueGame() { if (hasGame) screen = Screen.GAME }

    fun newGame(d: Difficulty) {
        difficulty = d
        loading = true
        screen = Screen.GAME
        viewModelScope.launch {
            val (puzzle, sol) = withContext(Dispatchers.Default) { SudokuGenerator.generate(d) }
            solution = sol
            for (i in 0 until 81) {
                board[i] = puzzle[i]
                fixed[i] = puzzle[i] != 0
                notes[i] = emptySet()
            }
            resetCounters()
            loading = false
            hasGame = true
        }
    }

    /** Vuelve a empezar el mismo tablero. */
    fun restart() {
        for (i in 0 until 81) {
            if (!fixed[i]) board[i] = 0
            notes[i] = emptySet()
        }
        resetCounters()
    }

    private fun resetCounters() {
        selected = -1
        notesMode = false
        mistakes = 0
        hintsLeft = MAX_HINTS
        seconds = 0
        newRecord = false
        status = Status.PLAYING
        undoStack.clear()
    }

    fun tick() { if (status == Status.PLAYING && !loading) seconds++ }

    internal fun solutionAt(i: Int) = solution[i]

    fun isWrong(i: Int) = board[i] != 0 && board[i] != solution[i]

    /** Cuántos gatitos de cada tipo están bien colocados. */
    fun placedCount(v: Int) = (0 until 81).count { board[it] == v && solution[it] == v }

    fun select(i: Int) { selected = i }

    fun input(v: Int) {
        val i = selected
        if (i < 0 || fixed[i] || status != Status.PLAYING) return
        if (notesMode) {
            if (board[i] != 0) return
            undoStack.addLast(Move(i, board[i], notes[i]))
            notes[i] = if (v in notes[i]) notes[i] - v else notes[i] + v
            return
        }
        if (board[i] == v) return
        undoStack.addLast(Move(i, board[i], notes[i]))
        board[i] = v
        notes[i] = emptySet()
        if (v != solution[i]) {
            errorTick++
            mistakes++
            if (mistakes >= MAX_MISTAKES) {
                status = Status.LOST
                play(CatSound.GRUNIDO)
            } else {
                play(CatSound.RONRONEO)
            }
        } else {
            clearPeerNotes(i, v)
            checkWin()
        }
    }

    fun erase() {
        val i = selected
        if (i < 0 || fixed[i] || status != Status.PLAYING) return
        if (board[i] == 0 && notes[i].isEmpty()) return
        undoStack.addLast(Move(i, board[i], notes[i]))
        board[i] = 0
        notes[i] = emptySet()
    }

    fun undo() {
        if (status != Status.PLAYING) return
        val m = undoStack.removeLastOrNull() ?: return
        if (fixed[m.idx]) return
        board[m.idx] = m.value
        notes[m.idx] = m.notes
        selected = m.idx
    }

    fun hint() {
        if (hintsLeft <= 0 || status != Status.PLAYING) return
        val target = if (selected >= 0 && !fixed[selected] && board[selected] != solution[selected]) {
            selected
        } else {
            (0 until 81).filter { board[it] != solution[it] }.randomOrNull() ?: return
        }
        hintsLeft--
        board[target] = solution[target]
        notes[target] = emptySet()
        fixed[target] = true
        selected = target
        clearPeerNotes(target, solution[target])
        checkWin()
    }

    /** Si perdiste, te deja seguir con el mismo tablero dándote una vida más. */
    fun oneMoreLife() {
        if (status == Status.LOST) {
            mistakes = MAX_MISTAKES - 1
            status = Status.PLAYING
        }
    }

    private fun clearPeerNotes(i: Int, v: Int) {
        val r = i / 9
        val c = i % 9
        for (p in 0 until 81) {
            val pr = p / 9
            val pc = p % 9
            val peer = pr == r || pc == c || (pr / 3 == r / 3 && pc / 3 == c / 3)
            if (peer && v in notes[p]) notes[p] = notes[p] - v
        }
    }

    private fun checkWin() {
        if ((0 until 81).all { board[it] == solution[it] }) {
            status = Status.WON
            play(CatSound.MAULLIDO)
            val best = bestTime(difficulty)
            if (best < 0 || seconds < best) {
                newRecord = true
                prefs.edit().putInt("record_${difficulty.name}", seconds).apply()
            }
        }
    }
}

fun formatTime(s: Int) = "%d:%02d".format(s / 60, s % 60)
