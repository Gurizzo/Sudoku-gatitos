package com.gatitos.sudoku

import kotlin.random.Random

/**
 * [logicOnly] = el tablero se puede terminar sólo con deducciones simples
 * (única casilla posible / único lugar posible), sin tener que adivinar nunca.
 */
enum class Difficulty(val label: String, val emoji: String, val removals: Int, val logicOnly: Boolean) {
    FACIL("Fácil", "🐾", 38, true),
    MEDIO("Medio", "🐱", 46, true),
    DIFICIL("Difícil", "😼", 52, false),
    EXPERTO("Experto", "🦁", 58, false),
}

/**
 * Genera sudokus aleatorios que siempre se pueden superar.
 * Se parte de un tablero completo válido (la solución) y se van quitando gatitos
 * sólo si el puzzle sigue teniendo UNA única solución. Las celdas valen 0 (vacía) o 1..9.
 */
object SudokuGenerator {

    private val UNITS: List<IntArray> = buildList {
        for (r in 0 until 9) add(IntArray(9) { r * 9 + it })
        for (c in 0 until 9) add(IntArray(9) { it * 9 + c })
        for (b in 0 until 9) add(IntArray(9) { (b / 3 * 3 + it / 3) * 9 + b % 3 * 3 + it % 3 })
    }

    fun generate(difficulty: Difficulty, rnd: Random = Random.Default): Pair<IntArray, IntArray> {
        val solution = IntArray(81)
        fill(solution, 0, rnd)
        val puzzle = solution.copyOf()
        var removed = 0
        for (i in (0 until 81).shuffled(rnd)) {
            if (removed >= difficulty.removals) break
            val backup = puzzle[i]
            puzzle[i] = 0
            val ok = if (difficulty.logicOnly) solvableByLogic(puzzle) else countSolutions(puzzle.copyOf(), 2) == 1
            if (ok) removed++ else puzzle[i] = backup
        }
        return puzzle to solution
    }

    fun canPlace(g: IntArray, idx: Int, v: Int): Boolean {
        val r = idx / 9
        val c = idx % 9
        for (k in 0 until 9) {
            if (r * 9 + k != idx && g[r * 9 + k] == v) return false
            if (k * 9 + c != idx && g[k * 9 + c] == v) return false
        }
        val br = r / 3 * 3
        val bc = c / 3 * 3
        for (i in 0 until 3) for (j in 0 until 3) {
            val p = (br + i) * 9 + bc + j
            if (p != idx && g[p] == v) return false
        }
        return true
    }

    private fun fill(g: IntArray, idx: Int, rnd: Random): Boolean {
        if (idx == 81) return true
        for (v in (1..9).shuffled(rnd)) {
            if (canPlace(g, idx, v)) {
                g[idx] = v
                if (fill(g, idx + 1, rnd)) return true
                g[idx] = 0
            }
        }
        return false
    }

    /**
     * Intenta resolver sólo con "única casilla" y "único lugar".
     * Si lo logra, el puzzle tiene solución única y no requiere adivinar.
     */
    fun solvableByLogic(puzzle: IntArray): Boolean {
        val g = puzzle.copyOf()
        var progress = true
        while (progress) {
            progress = false
            for (i in 0 until 81) {
                if (g[i] != 0) continue
                var cnt = 0
                var only = 0
                for (v in 1..9) if (canPlace(g, i, v)) { cnt++; only = v }
                if (cnt == 0) return false
                if (cnt == 1) { g[i] = only; progress = true }
            }
            for (u in UNITS) for (v in 1..9) {
                if (u.any { g[it] == v }) continue
                var cnt = 0
                var pos = -1
                for (i in u) if (g[i] == 0 && canPlace(g, i, v)) { cnt++; pos = i }
                if (cnt == 0) return false
                if (cnt == 1) { g[pos] = v; progress = true }
            }
        }
        return g.all { it != 0 }
    }

    /** Cuenta soluciones hasta [limit], eligiendo siempre la celda con menos candidatos. */
    fun countSolutions(g: IntArray, limit: Int): Int {
        var best = -1
        var bestCount = 10
        for (i in 0 until 81) {
            if (g[i] != 0) continue
            var cnt = 0
            for (v in 1..9) if (canPlace(g, i, v)) cnt++
            if (cnt == 0) return 0
            if (cnt < bestCount) {
                bestCount = cnt
                best = i
            }
        }
        if (best == -1) return 1
        var total = 0
        for (v in 1..9) {
            if (!canPlace(g, best, v)) continue
            g[best] = v
            total += countSolutions(g, limit - total)
            g[best] = 0
            if (total >= limit) return total
        }
        return total
    }
}
