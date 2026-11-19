package szofverfejlesztes.mi2048.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import szofverfejlesztes.mi2048.game.Direction
import szofverfejlesztes.mi2048.game.TileData
import kotlin.math.abs

@Composable
fun GameScreen(
    viewModel: MainViewModel,
    onNavigateToLeaderboard: () -> Unit,
    onLogout: () -> Unit
) {
    val activeTiles by viewModel.activeTiles.collectAsState()
    val score by viewModel.score.collectAsState()
    val isGameOver by viewModel.isGameOver.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isGuest by viewModel.isGuest.collectAsState()

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val dragThreshold = 60f

    // Kétfázisú animáció vezérlés:
    // 1. fázis (!isRevealed): Csúszási fázis (150 ms) -> csak a csúszó forráscsempék láthatóak.
    // 2. fázis (isRevealed): Megjelenési fázis (150 ms után) -> új összevont és új spavnált csempék felbukkannak.
    var isRevealed by remember { mutableStateOf(true) }

    LaunchedEffect(activeTiles) {
        isRevealed = false
        delay(150L)
        isRevealed = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Fejléc
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "2048",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isGuest) "Vendég mód" else "Játékos: ${currentUser?.username ?: ""}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "PONTSZÁM", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$score", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Akció gombok
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { viewModel.restartGame() }) {
                Text("Új játék")
            }
            OutlinedButton(onClick = onNavigateToLeaderboard) {
                Text("Toplista")
            }
            OutlinedButton(onClick = {
                viewModel.logout()
                onLogout()
            }) {
                Text("Kijelentkezés")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4x4 Játéktábla Swipe és Kétfázisú Időzített Animáció
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFFBBADA0), RoundedCornerShape(12.dp))
                .padding(8.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        },
                        onDragEnd = {
                            if (abs(offsetX) > abs(offsetY)) {
                                if (abs(offsetX) > dragThreshold) {
                                    if (offsetX > 0) {
                                        viewModel.move(Direction.RIGHT)
                                    } else {
                                        viewModel.move(Direction.LEFT)
                                    }
                                }
                            } else {
                                if (abs(offsetY) > dragThreshold) {
                                    if (offsetY > 0) {
                                        viewModel.move(Direction.DOWN)
                                    } else {
                                        viewModel.move(Direction.UP)
                                    }
                                }
                            }
                            offsetX = 0f
                            offsetY = 0f
                        },
                        onDragCancel = {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    )
                }
        ) {
            val padding = 8.dp
            val spacing = 6.dp
            val availableWidth = maxWidth - (padding * 2) - (spacing * 3)
            val cellSize = availableWidth / 4

            // 1. Üres háttér rácscellák
            for (row in 0..3) {
                for (col in 0..3) {
                    val cellX = padding + (cellSize + spacing) * col
                    val cellY = padding + (cellSize + spacing) * row
                    Box(
                        modifier = Modifier
                            .offset(x = cellX, y = cellY)
                            .size(cellSize)
                            .background(Color(0xFFCDC1B4), RoundedCornerShape(8.dp))
                    )
                }
            }

            // 2. Aktív csempék kirajzolása a 2 fázis alapján
            for (tile in activeTiles) {
                key(tile.id) {
                    val targetX = padding + (cellSize + spacing) * tile.col
                    val targetY = padding + (cellSize + spacing) * tile.row

                    val animatedX by animateDpAsState(
                        targetValue = targetX,
                        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                        label = "tileX_${tile.id}"
                    )
                    val animatedY by animateDpAsState(
                        targetValue = targetY,
                        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                        label = "tileY_${tile.id}"
                    )

                    val isMergedTile = tile.isMerged
                    val isNewTile = tile.isNew
                    val isSourceMergedTile = !tile.isMerged && !tile.isNew && activeTiles.any { it.isMerged && it.row == tile.row && it.col == tile.col }

                    // Láthatóság és Skálázás időzítése
                    val targetAlpha = when {
                        isMergedTile || isNewTile -> if (isRevealed) 1.0f else 0.0f
                        isSourceMergedTile -> if (isRevealed) 0.0f else 1.0f
                        else -> 1.0f
                    }

                    val targetScale = when {
                        isMergedTile || isNewTile -> if (isRevealed) 1.0f else 0.5f
                        else -> 1.0f
                    }

                    val animatedAlpha by animateFloatAsState(
                        targetValue = targetAlpha,
                        animationSpec = tween(durationMillis = 100),
                        label = "alpha_${tile.id}"
                    )
                    val animatedScale by animateFloatAsState(
                        targetValue = targetScale,
                        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
                        label = "scale_${tile.id}"
                    )

                    if (animatedAlpha > 0.01f) {
                        AnimatedTileView(
                            tile = tile,
                            modifier = Modifier
                                .offset(x = animatedX, y = animatedY)
                                .size(cellSize)
                                .graphicsLayer(
                                    scaleX = animatedScale,
                                    scaleY = animatedScale,
                                    alpha = animatedAlpha
                                )
                        )
                    }
                }
            }
        }
    }

    // Game Over Dialog
    if (isGameOver) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Játék vége!") },
            text = {
                Text(
                    if (isGuest)
                        "Megtelt a tábla! Végső pontszámod: $score (Vendég módban a pontszám nem kerül toplistára)."
                    else
                        "Megtelt a tábla! Végső pontszámod: $score. A pontszámod bekerült a toplistára!"
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.restartGame() }) {
                    Text("Új játék")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onNavigateToLeaderboard) {
                    Text("Toplista")
                }
            }
        )
    }
}

@Composable
fun AnimatedTileView(tile: TileData, modifier: Modifier = Modifier) {
    val (backgroundColor, textColor) = getTileColors(tile.value)

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (tile.value > 0) {
            Text(
                text = "${tile.value}",
                fontSize = when {
                    tile.value < 100 -> 28.sp
                    tile.value < 1000 -> 22.sp
                    else -> 18.sp
                },
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

fun getTileColors(value: Int): Pair<Color, Color> {
    val darkText = Color(0xFF776E65)
    val lightText = Color(0xFFF9F6F2)

    return when (value) {
        2 -> Pair(Color(0xFFEEE4DA), darkText)
        4 -> Pair(Color(0xFFEDE0C8), darkText)
        8 -> Pair(Color(0xFFF2B179), lightText)
        16 -> Pair(Color(0xFFF59563), lightText)
        32 -> Pair(Color(0xFFF67C5F), lightText)
        64 -> Pair(Color(0xFFF65E3B), lightText)
        128 -> Pair(Color(0xFFEDCF72), lightText)
        256 -> Pair(Color(0xFFEDCC61), lightText)
        512 -> Pair(Color(0xFFEDC850), lightText)
        1024 -> Pair(Color(0xFFEDC53F), lightText)
        2048 -> Pair(Color(0xFFEDC22E), lightText)
        else -> if (value > 2048) Pair(Color(0xFF3C3A32), lightText) else Pair(Color(0xFFCDC1B4), darkText)
    }
}
