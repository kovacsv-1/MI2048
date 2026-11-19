package szofverfejlesztes.mi2048.game

data class TileData(
    val id: Long,
    val value: Int,
    val row: Int,
    val col: Int,
    val isMerged: Boolean = false,
    val isNew: Boolean = false
)
