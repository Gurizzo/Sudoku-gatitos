package com.gatitos.sudoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuGeneratorTest {

    private val partidasPorNivel = 40

    @Test
    fun todosLosNivelesSonResolublesYAleatorios() {
        for (d in Difficulty.entries) {
            val vistos = HashSet<String>()
            var vaciasTotal = 0
            repeat(partidasPorNivel) {
                val (puzzle, solucion) = SudokuGenerator.generate(d)

                // La solución es un sudoku completo y válido
                for (i in 0 until 81) {
                    assertTrue(solucion[i] in 1..9)
                    assertTrue("solución inválida", SudokuGenerator.canPlace(solucion, i, solucion[i]))
                }
                // Los gatitos dados coinciden con la solución
                for (i in 0 until 81) if (puzzle[i] != 0) assertEquals(solucion[i], puzzle[i])

                // Tiene exactamente una solución → siempre se puede superar
                assertEquals("${d.label}: debe tener solución única", 1,
                    SudokuGenerator.countSolutions(puzzle.copyOf(), 2))
                if (d.logicOnly) {
                    assertTrue("${d.label}: debe resolverse sin adivinar", SudokuGenerator.solvableByLogic(puzzle))
                }
                vistos += puzzle.joinToString("")
                vaciasTotal += puzzle.count { it == 0 }
            }
            assertEquals("${d.label}: cada partida debe ser distinta", partidasPorNivel, vistos.size)
            println("${d.label}: $partidasPorNivel tableros OK, promedio de casillas vacías = ${vaciasTotal / partidasPorNivel}")
        }
    }
}
