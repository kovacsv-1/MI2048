package szofverfejlesztes.mi2048.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private lateinit var gameEngine: GameEngine

    @Before
    fun setUp() {
        // Use seed for predictable random generation in unit tests
        gameEngine = GameEngine(Random(12345))
    }

    // Elfogadási feltétel 1: 4x4 táblán egy érvényes mozgatás után a csempék összevonódnak, a pontszám nő.
    @Test
    fun testNormalMoveAndMerge() {
        val customBoard = listOf(
            2, 2, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0
        )
        gameEngine.loadGame(customBoard, savedScore = 0, savedIsGameOver = false)

        val result = gameEngine.move(Direction.LEFT)

        assertTrue(result.moved)
        assertEquals(4, result.scoreGained)
        assertEquals(4, gameEngine.score)
        assertEquals(4, gameEngine.board[0]) // First tile merged to 4
    }

    // Elfogadási feltétel 2: Érvényes mozgatás után egy új csempe (2 vagy 4) jelenik meg, táblaméret 16.
    @Test
    fun testValidMoveSpawnsNewTile() {
        gameEngine.resetGame()
        val initialNonZeroCount = gameEngine.board.count { it != 0 }
        assertEquals(2, initialNonZeroCount)

        val result = gameEngine.move(Direction.LEFT)
        if (result.moved) {
            assertEquals(16, gameEngine.board.size)
            assertTrue(result.newTileSpawned)
            val newNonZeroCount = gameEngine.board.count { it != 0 }
            assertTrue(newNonZeroCount >= 2)
        }
    }

    // Elfogadási feltétel 3: Határeset – Teli táblánál, ha nincs érvényes mozgatás, a játék véget ér.
    @Test
    fun testGameOverWhenBoardFullAndNoMoves() {
        val fullNoMovesBoard = listOf(
            2, 4, 2, 4,
            4, 2, 4, 2,
            2, 4, 2, 4,
            4, 2, 4, 2
        )
        gameEngine.loadGame(fullNoMovesBoard, savedScore = 100, savedIsGameOver = false)

        assertTrue(gameEngine.checkGameOver())

        val result = gameEngine.move(Direction.UP)
        assertFalse(result.moved)
        assertTrue(gameEngine.isGameOver)
    }

    // Elfogadási feltétel 4: Határeset – Egy lépésben egy csempe csak egyszer vonható össze (2+2+2+2 -> 4+4, nem 8).
    @Test
    fun testSingleMergeRulePerStep() {
        val (mergedLine, scoreGained) = gameEngine.mergeLine(listOf(2, 2, 2, 2))

        assertEquals(listOf(4, 4, 0, 0), mergedLine)
        assertEquals(8, scoreGained)
    }

    // Elfogadási feltétel 6: Hibás bemenet/érvénytelen gesztus – Ha semmi sem tud mozdulni, a tábla és pontszám nem változik.
    @Test
    fun testInvalidSwipeDoesNotChangeBoardOrSpawnTile() {
        val leftAlignedBoard = listOf(
            4, 8, 0, 0,
            2, 4, 0, 0,
            8, 2, 0, 0,
            4, 8, 0, 0
        )
        gameEngine.loadGame(leftAlignedBoard, savedScore = 50, savedIsGameOver = false)

        val result = gameEngine.move(Direction.LEFT)

        assertFalse(result.moved)
        assertFalse(result.newTileSpawned)
        assertEquals(0, result.scoreGained)
        assertEquals(50, gameEngine.score)
        assertEquals(leftAlignedBoard, gameEngine.board)
    }

    @Test
    fun testActiveTilesPositionTrackingOnMove() {
        val board = listOf(
            0, 0, 2, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0
        )
        gameEngine.loadGame(board, savedScore = 0, savedIsGameOver = false)

        val tileBefore = gameEngine.activeTiles.first { it.value == 2 }
        assertEquals(0, tileBefore.row)
        assertEquals(2, tileBefore.col)

        gameEngine.move(Direction.LEFT)

        val tileAfter = gameEngine.activeTiles.first { it.id == tileBefore.id }
        assertEquals(0, tileAfter.row)
        assertEquals(0, tileAfter.col)
    }

    // --------------------------------------------------------------------------------------
    // KÖTELEZŐ TESZTELÉSI KÖVETELMÉNY: Megőrzött hibás vs. javított változat tesztje
    // --------------------------------------------------------------------------------------

    /**
     * Szándékosan hibás összevonó algoritmus, ami láncoltan összevonja az elemeket egyetlen lépésben
     * (pl. a [2, 2, 2, 2] sorból [8, 0, 0, 0]-t hoz létre [4, 4, 0, 0] helyett).
     */
    private class FlawedGameEngine {
        fun mergeLineFlawed(line: List<Int>): List<Int> {
            val nonZero = line.filter { it != 0 }.toMutableList()
            var changed = true
            while (changed) {
                changed = false
                var i = 0
                while (i < nonZero.size - 1) {
                    if (nonZero[i] == nonZero[i + 1]) {
                        nonZero[i] = nonZero[i] * 2
                        nonZero.removeAt(i + 1)
                        changed = true
                        break
                    }
                    i++
                }
            }
            val result = nonZero.toMutableList()
            while (result.size < 4) result.add(0)
            return result
        }
    }

    @Test
    fun testFlawedEngineFailsSingleMergeRule() {
        val flawedEngine = FlawedGameEngine()
        val flawedResult = flawedEngine.mergeLineFlawed(listOf(2, 2, 2, 2))

        // A HIBÁS változat ELBUKIK a 2048 szabályon: [8, 0, 0, 0]-t ad vissza!
        assertEquals(listOf(8, 0, 0, 0), flawedResult)
        assertNotEquals(listOf(4, 4, 0, 0), flawedResult)
    }

    @Test
    fun testFixedEnginePassesSingleMergeRule() {
        // A JAVÍTOTT (rendes) engine ÁTMEGY: pontosan [4, 4, 0, 0]-át ad vissza.
        val (fixedResult, _) = gameEngine.mergeLine(listOf(2, 2, 2, 2))
        assertEquals(listOf(4, 4, 0, 0), fixedResult)
    }
}
