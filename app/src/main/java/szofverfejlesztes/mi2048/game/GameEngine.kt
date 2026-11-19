package szofverfejlesztes.mi2048.game

import kotlin.random.Random

class GameEngine(private val random: Random = Random.Default) {
    var board: List<Int> = List(16) { 0 }
        private set

    var activeTiles: List<TileData> = emptyList()
        private set

    var score: Int = 0
        private set

    var isGameOver: Boolean = false
        private set

    private var nextTileId = 1L

    fun resetGame() {
        board = List(16) { 0 }
        activeTiles = emptyList()
        score = 0
        isGameOver = false
        nextTileId = 1L
        spawnRandomTile()
        spawnRandomTile()
    }

    fun loadGame(savedBoard: List<Int>, savedScore: Int, savedIsGameOver: Boolean) {
        require(savedBoard.size == 16) { "Board size must be exactly 16" }
        board = savedBoard
        score = savedScore
        val newTiles = mutableListOf<TileData>()
        nextTileId = 1L
        for (r in 0..3) {
            for (c in 0..3) {
                val valAtCell = savedBoard[r * 4 + c]
                if (valAtCell > 0) {
                    newTiles.add(TileData(id = nextTileId++, value = valAtCell, row = r, col = c))
                }
            }
        }
        activeTiles = newTiles
        isGameOver = savedIsGameOver || checkGameOver()
    }

    fun spawnRandomTile(): Boolean {
        val emptyIndices = (0..15).filter { board[it] == 0 }
        if (emptyIndices.isEmpty()) return false

        val randomIndex = emptyIndices[random.nextInt(emptyIndices.size)]
        val row = randomIndex / 4
        val col = randomIndex % 4
        val value = if (random.nextDouble() < 0.9) 2 else 4

        val newTile = TileData(id = nextTileId++, value = value, row = row, col = col, isNew = true)
        activeTiles = cleanEffectiveTiles(activeTiles).map { it.copy(isNew = false, isMerged = false) } + newTile

        syncBoardFromActiveTiles()
        return true
    }

    private fun cleanEffectiveTiles(tiles: List<TileData>): List<TileData> {
        val cellMap = mutableMapOf<Pair<Int, Int>, TileData>()
        for (tile in tiles) {
            val key = Pair(tile.row, tile.col)
            val existing = cellMap[key]
            if (existing == null || tile.isMerged || tile.id > existing.id) {
                cellMap[key] = tile
            }
        }
        return cellMap.values.toList()
    }

    private fun syncBoardFromActiveTiles() {
        val newBoard = IntArray(16) { 0 }
        for (tile in cleanEffectiveTiles(activeTiles)) {
            newBoard[tile.row * 4 + tile.col] = tile.value
        }
        board = newBoard.toList()
    }

    fun move(direction: Direction): MoveResult {
        if (isGameOver) {
            return MoveResult(moved = false, scoreGained = 0, newTileSpawned = false)
        }

        // Clean up any remaining merged source tiles from prior turn
        val effectiveTiles = cleanEffectiveTiles(activeTiles)

        val updatedTiles = mutableListOf<TileData>()
        var totalScoreGained = 0
        var movedAny = false

        for (lineIdx in 0..3) {
            val lineTiles = getTilesInLine(effectiveTiles, lineIdx, direction)
            val (mergedLineTiles, lineScore, lineMoved) = processLineMove(lineTiles, lineIdx, direction)
            updatedTiles.addAll(mergedLineTiles)
            totalScoreGained += lineScore
            if (lineMoved) movedAny = true
        }

        if (movedAny) {
            activeTiles = updatedTiles
            syncBoardFromActiveTiles()
            score += totalScoreGained
            val spawned = spawnRandomTile()
            isGameOver = checkGameOver()
            return MoveResult(moved = true, scoreGained = totalScoreGained, newTileSpawned = spawned)
        } else {
            isGameOver = checkGameOver()
            return MoveResult(moved = false, scoreGained = 0, newTileSpawned = false)
        }
    }

    private fun getTilesInLine(tiles: List<TileData>, lineIdx: Int, direction: Direction): List<TileData> {
        return tiles.filter { tile ->
            when (direction) {
                Direction.LEFT, Direction.RIGHT -> tile.row == lineIdx
                Direction.UP, Direction.DOWN -> tile.col == lineIdx
            }
        }.sortedBy { tile ->
            when (direction) {
                Direction.LEFT -> tile.col
                Direction.RIGHT -> -tile.col
                Direction.UP -> tile.row
                Direction.DOWN -> -tile.row
            }
        }
    }

    private fun processLineMove(
        tilesInLine: List<TileData>,
        lineIdx: Int,
        direction: Direction
    ): Triple<List<TileData>, Int, Boolean> {
        val resultTiles = mutableListOf<TileData>()
        var lineScore = 0
        var lineMoved = false
        var targetPos = 0
        var i = 0

        while (i < tilesInLine.size) {
            val current = tilesInLine[i]
            val (targetRow, targetCol) = getCoordinates(lineIdx, targetPos, direction)

            if (i + 1 < tilesInLine.size && current.value == tilesInLine[i + 1].value) {
                val nextTile = tilesInLine[i + 1]
                val mergedVal = current.value * 2
                lineScore += mergedVal

                // Both source tiles slide to target position
                resultTiles.add(current.copy(row = targetRow, col = targetCol, isMerged = false, isNew = false))
                resultTiles.add(nextTile.copy(row = targetRow, col = targetCol, isMerged = false, isNew = false))

                // New merged tile created at target position
                val mergedTile = TileData(
                    id = nextTileId++,
                    value = mergedVal,
                    row = targetRow,
                    col = targetCol,
                    isMerged = true
                )
                resultTiles.add(mergedTile)

                lineMoved = true
                i += 2
            } else {
                if (current.row != targetRow || current.col != targetCol) {
                    lineMoved = true
                }
                resultTiles.add(current.copy(row = targetRow, col = targetCol, isMerged = false, isNew = false))
                i += 1
            }
            targetPos += 1
        }

        return Triple(resultTiles, lineScore, lineMoved)
    }

    private fun getCoordinates(lineIdx: Int, targetPos: Int, direction: Direction): Pair<Int, Int> {
        return when (direction) {
            Direction.LEFT -> Pair(lineIdx, targetPos)
            Direction.RIGHT -> Pair(lineIdx, 3 - targetPos)
            Direction.UP -> Pair(targetPos, lineIdx)
            Direction.DOWN -> Pair(3 - targetPos, lineIdx)
        }
    }

    fun mergeLine(line: List<Int>): Pair<List<Int>, Int> {
        val nonZero = line.filter { it != 0 }
        val result = mutableListOf<Int>()
        var scoreGained = 0
        var i = 0

        while (i < nonZero.size) {
            if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
                val mergedVal = nonZero[i] * 2
                result.add(mergedVal)
                scoreGained += mergedVal
                i += 2
            } else {
                result.add(nonZero[i])
                i += 1
            }
        }

        while (result.size < 4) {
            result.add(0)
        }

        return Pair(result, scoreGained)
    }

    fun checkGameOver(): Boolean {
        if (board.contains(0)) return false

        for (row in 0..3) {
            for (col in 0..3) {
                val current = board[row * 4 + col]
                if (col < 3 && current == board[row * 4 + (col + 1)]) return false
                if (row < 3 && current == board[(row + 1) * 4 + col]) return false
            }
        }

        return true
    }
}
