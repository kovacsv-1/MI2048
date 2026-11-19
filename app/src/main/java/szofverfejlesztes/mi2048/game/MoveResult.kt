package szofverfejlesztes.mi2048.game

data class MoveResult(
    val moved: Boolean,
    val scoreGained: Int,
    val newTileSpawned: Boolean
)
