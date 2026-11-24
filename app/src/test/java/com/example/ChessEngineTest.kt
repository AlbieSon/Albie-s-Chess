package com.example

import com.example.chess.ChessBoardState
import com.example.chess.EngineStyle
import com.example.chess.GameResult
import com.example.chess.HistoricalPuzzleLibrary
import com.example.chess.OpeningBook
import com.example.chess.PuzzleCategory
import com.example.chess.PuzzleGroup
import com.example.chess.StockfishEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessEngineTest {

    @Test
    fun initialPositionHas20LegalMoves() {
        val board = ChessBoardState.initial()
        assertEquals(20, board.generateLegalMoves().size)
    }

    @Test
    fun scholarsMateDetectedCorrectly() {
        val (board, sans) = ChessBoardState.replayUciMoves(
            listOf("e2e4", "e7e5", "f1c4", "b8c6", "d1h5", "g8f6", "h5f7")
        )
        assertEquals(GameResult.WHITE_WINS_CHECKMATE, board.evaluateGameStatus())
        assertTrue(sans.last().endsWith("#"))
    }

    @Test
    fun openingBookIdentifiesItalianAndSicilianNajdorf() {
        val italian = OpeningBook.identifyOpening(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4"))
        assertEquals("C50", italian.eco)
        assertTrue(italian.name.contains("Italian"))

        val najdorf = OpeningBook.identifyOpening(
            listOf("e2e4", "c7c5", "g1f3", "d7d6", "d2d4", "c5d4", "f3d4", "g8f6", "b1c3", "a7a6")
        )
        assertEquals("B90", najdorf.eco)
        assertTrue(najdorf.name.contains("Najdorf"))
    }

    @Test
    fun engineFindsMateInOneAndGuessMatchesGold() {
        // White to move: Qxf7# is mate in 1
        val board = ChessBoardState.fromFen("r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4")
        val engine = StockfishEngine()
        val eval = engine.findBestMove(board, EngineStyle.UNIVERSAL, timeBudgetMs = 400L, maxDepth = 3, useOpeningBook = false)
        assertNotNull(eval.bestMove)
        val bestMove = eval.bestMove!!
        assertEquals("h5f7", bestMove.toUci())

        // Player guesses h5f7 -> Exact Best Match (100% accuracy, Gold arrow!)
        val guess = engine.evaluateGuess(board, bestMove, EngineStyle.UNIVERSAL)
        assertTrue(guess.isExactBest)
        assertEquals(100.0, guess.accuracyPercent, 0.01)
    }

    @Test
    fun allHistoricalPuzzlesHaveValidFenAndLegalSolutionLines() {
        val puzzles = HistoricalPuzzleLibrary.allPuzzles
        assertTrue("Expected both tactics and strategy puzzles", puzzles.size >= 12)
        assertTrue(puzzles.any { it.group.category == PuzzleCategory.TACTICS })
        assertTrue(puzzles.any { it.group.category == PuzzleCategory.STRATEGY })

        val seenIds = mutableSetOf<String>()
        for (puzzle in puzzles) {
            assertTrue("Duplicate puzzle id: ${puzzle.id}", seenIds.add(puzzle.id))
            assertFalse("Empty solution for ${puzzle.id}", puzzle.solutionLineUci.isEmpty())
            assertEquals("Solution length must be odd (player starts and finishes): ${puzzle.id}", 1, puzzle.solutionLineUci.size % 2)

            var board = ChessBoardState.fromFen(puzzle.fen)
            for ((stepIdx, uci) in puzzle.solutionLineUci.withIndex()) {
                val move = board.findLegalMoveFromUci(uci)
                assertNotNull(
                    "Illegal move '$uci' at step $stepIdx in puzzle '${puzzle.id}' (${puzzle.title}) from FEN '${board.toFen()}'",
                    move
                )
                board = board.applyMove(move!!)
            }

            if (puzzle.group == PuzzleGroup.CHECKMATE_BRILLIANCIES) {
                val status = board.evaluateGameStatus()
                assertTrue(
                    "Expected checkmate at end of '${puzzle.id}' (${puzzle.title}), got $status (FEN=${board.toFen()})",
                    status == GameResult.WHITE_WINS_CHECKMATE || status == GameResult.BLACK_WINS_CHECKMATE
                )
            }
        }
    }
}
