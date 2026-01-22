package com.example.minesweeper

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures // For Pinch-to-Zoom/Pan
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info // For References Dialog
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf // For Zoom State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer // For Zoom Transformation
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput // For Zoom Gestures
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.max

// =========================================================================
// 💡 CONFIGURATION CONSTANTS (Dimensions)
// =========================================================================
const val GRID_WIDTH_DEFAULT = 20
const val GRID_HEIGHT_DEFAULT = 28
private const val PADDING_DP = 1

// Define difficulty settings for the UI
enum class Difficulty(val width: Int, val height: Int, val mines: Int) {
    BEGINNER(15, 25, 50),
    INTERMEDIATE(20, 28, 90),
    EXPERT(30, 50, 150),
    // Use default values for initial custom game
    CUSTOM(GRID_WIDTH_DEFAULT, GRID_HEIGHT_DEFAULT, 60)
}

// Helper function to get color for cell numbers (Minesweeper standard colors)
fun getNumberColor(value: Int): Color {
    return when (value) {
        1 -> Color(0xFF0000FF)
        2 -> Color(0xFF008000)
        3 -> Color(0xFFC00000)
        4 -> Color(0xFF000080)
        5 -> Color(0xFF800000)
        6 -> Color(0xFF008080)
        7 -> Color(0xFF000000)
        8 -> Color(0xFF808080)
        else -> Color.Black
    }
}

// Composable for a single Minesweeper cell button
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CellButton(
    x: Int,
    y: Int,
    cell: Cell,
    onReveal: (Int, Int) -> Unit,
    onToggleFlag: (Int, Int) -> Unit,
    onChord: (Int, Int) -> Unit,
    isGameOver: Boolean,
    cellSize: Dp,
) {
    val haptics = LocalHapticFeedback.current

    val backgroundColor = when {
        isGameOver && cell.value == Game.BOMB && cell.isRevealed -> Color.Red
        isGameOver && cell.value == Game.BOMB && !cell.isFlagged && !cell.isRevealed -> Color(0xFFFFCC00).copy(alpha = 0.8f)
        cell.isRevealed -> Color.LightGray.copy(alpha = 0.8f)
        else -> Color.DarkGray
    }

    val fontSize = (cellSize.value * 0.5f).sp
    val minFontSize = 10.sp
    val finalFontSize = maxOf(fontSize.value, minFontSize.value).sp

    Box(
        modifier = Modifier
            .size(cellSize)
            .padding(0.5.dp)
            .shadow(
                elevation = if (cell.isRevealed) 2.dp else 8.dp,
                shape = RoundedCornerShape(4.dp)
            )
            .background(
                color = if (cell.isRevealed) Color.LightGray else Color.Gray,
                shape = RoundedCornerShape(4.dp)
            )
            .background(backgroundColor, RoundedCornerShape(2.dp))
            .combinedClickable(
                // Logic combined into a single onClick for reveal/chord
                onClick = {
                    if (isGameOver) return@combinedClickable

                    if (cell.isRevealed) {
                        // Case 1: Execute Chord Reveal on revealed number cells
                        if (cell.value > Game.EMPTY) {
                            onChord(x, y)
                        }
                    } else if (!cell.isFlagged) {
                        // Case 2: Execute Initial Reveal
                        onReveal(x, y)
                    }
                },

                onLongClick = {
                    if (!cell.isRevealed && !isGameOver) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleFlag(x, y)
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (cell.isRevealed) {
            when (cell.value) {
                Game.BOMB -> Text("💣", fontSize = finalFontSize)
                Game.EMPTY -> Text("")
                else -> Text(
                    text = cell.value.toString(),
                    color = getNumberColor(cell.value),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = finalFontSize
                )
            }
        } else if (cell.isFlagged) {
            Text("🚩", fontSize = finalFontSize)
        } else if (isGameOver && cell.value == Game.BOMB) {
            if (!cell.isFlagged) {
                Text("💣", fontSize = finalFontSize)
            }
        }
    }
}

// -------------------------------------------------------------------------
// Game Timer Composable
// -------------------------------------------------------------------------

@Composable
fun GameTimer(
    isGameActive: Boolean,
    isGameOver: Boolean,
    isGameWon: Boolean,
    onTick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Only one source of truth for time
    var totalSeconds by remember { mutableIntStateOf(0) }

    // 2. Reset timer when game is reset (not active and not over)
    LaunchedEffect(isGameActive, isGameOver) {
        if (!isGameActive && !isGameOver) {
            totalSeconds = 0
            onTick(0)
        }
    }

    // 3. Single Timer Loop
    LaunchedEffect(isGameActive, isGameOver, isGameWon) {
        // Start ticking ONLY if game is active and not over
        if (isGameActive && !isGameOver && !isGameWon) {
            while (true) {
                delay(1000L)
                totalSeconds = (totalSeconds + 1).coerceAtMost(9999)
                onTick(totalSeconds)
            }
        }
    }

    // 4. UI Rendering
    val timeDisplay = "%04d".format(totalSeconds)

    Text(
        text = "⏱️ $timeDisplay",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = Color.White,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

// -------------------------------------------------------------------------
// Main Screen
// -------------------------------------------------------------------------

// In MinesweeperScreen.kt (Replace the existing function)

@Composable
fun MinesweeperScreen(modifier: Modifier = Modifier) {

    // 💡 Game State Variables
    var currentDifficulty by remember { mutableStateOf(Difficulty.CUSTOM) }
    var gridWidth by remember { mutableIntStateOf(GRID_WIDTH_DEFAULT) }
    var gridHeight by remember { mutableIntStateOf(GRID_HEIGHT_DEFAULT) }
    var bombCount by remember { mutableIntStateOf(Difficulty.CUSTOM.mines) }
    var seconds by remember { mutableIntStateOf(0) }

    var isGameActive by remember { mutableStateOf(false) }

    var game by remember { mutableStateOf(Game(gridWidth, gridHeight, bombCount)) }
    var gameOver by remember { mutableStateOf(false) }
    var gameWon by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showReferenceDialog by remember { mutableStateOf(false) }

    var gameKey by remember { mutableIntStateOf(0) }
    var boardUpdateTrigger by remember { mutableIntStateOf(0) }

    // 💡 ZOOM STATE VARIABLES
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val MIN_SCALE = 1f
    val MAX_SCALE = 4f // Increased max zoom for better visibility

    // Game restart logic
    val restartGame: () -> Unit = {
        game = Game(gridWidth, gridHeight, bombCount)
        gameOver = false
        gameWon = false
        isGameActive = false
        seconds = 0
        gameKey++
        boardUpdateTrigger++
        // Reset zoom state on restart
        scale = MIN_SCALE
        offsetX = 0f
        offsetY = 0f
    }

    // --- Dynamic Sizing Logic ---
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val availableWidth = screenWidth - (PADDING_DP * 2).dp
    val cellSize = (availableWidth.value / gridWidth).dp

    // --- Handlers (Unchanged) ---
    val onReveal: (Int, Int) -> Unit = reveal@ { x, y ->
        if (gameOver) return@reveal
        if (!isGameActive) isGameActive = true

        val isBomb = game.reveal(x, y)
        boardUpdateTrigger++

        if (isBomb) {
            gameOver = true
            isGameActive = false
        } else if (game.checkWin()) {
            gameOver = true
            gameWon = true
            game.setBestTimeInSeconds(seconds)
            isGameActive = false
        }
    }

    val onToggleFlag: (Int, Int) -> Unit = toggle@ { x, y ->
        if (gameOver) return@toggle
        if (!isGameActive) isGameActive = true

        game.toggleFlag(x, y)
        boardUpdateTrigger++
    }

    val onChord: (Int, Int) -> Unit = chord@ { x, y ->
        if (gameOver || !isGameActive) return@chord

        val hitBomb = game.chordReveal(x, y)
        boardUpdateTrigger++

        if (hitBomb) {
            gameOver = true
            isGameActive = false
        } else if (game.checkWin()) {
            gameOver = true
            gameWon = true
            isGameActive = false
        }
    }

    // --- Main Layout ---
    Column(
        modifier
            .fillMaxSize()
            .padding(PADDING_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Top Bar (Unchanged)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Gabo's Grid Logic",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {

                // Mine Counter
                Text(
                    text = "💣 ${game.getBombCount() - game.getFlagCount()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 16.dp)
                )

                // Timer
                GameTimer(
                    isGameActive = isGameActive,
                    isGameOver = gameOver,
                    isGameWon = gameWon,
                    onTick = { seconds = it },
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                Spacer(Modifier.width(8.dp))

                // Settings Button
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray)
                }

                // References Button
                IconButton(onClick = { showReferenceDialog = true }) {
                    Icon(Icons.Default.Info, contentDescription = "References", tint = Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 💡 UPDATED: Game Board Display with Refined Zoom/Pan
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(2.dp)
                .weight(1f)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // 1. Update Scale (Pinch) and clamp
                        val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                        scale = newScale

                        // 2. Update Offsets (Pan)
                        if (scale > MIN_SCALE) {
                            offsetX += pan.x / scale // Divide by scale for smooth panning when zoomed
                            offsetY += pan.y / scale

                            // Clamping: Ensure content edges stay within the Box boundaries
                            val contentWidth = size.width * scale
                            val contentHeight = size.height * scale

                            val maxX = (contentWidth - size.width) / 2f
                            val maxY = (contentHeight - size.height) / 2f

                            // Clamping is applied to the raw translation values
                            offsetX = offsetX.coerceIn(-maxX / scale, maxX / scale)
                            offsetY = offsetY.coerceIn(-maxY / scale, maxY / scale)
                        } else {
                            // Reset offsets when scale is minimum
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
        ) {
            // Apply the calculated Scale and Offset to the entire grid Column
            Column(
                modifier = Modifier
                    .fillMaxSize() // Use fillMaxSize here to ensure it uses the Box dimensions
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX * scale, // Multiply by scale for correct translation effect
                        translationY = offsetY * scale
                    )
            ) {
                key(gameKey, boardUpdateTrigger, cellSize) {
                    for (x in 0 until game.getHeight()) {
                        Row(Modifier.fillMaxWidth()) {
                            for (y in 0 until game.getWidth()) {
                                val cell = game.getCell(x, y)
                                CellButton(
                                    x = x,
                                    y = y,
                                    cell = cell,
                                    onReveal = onReveal,
                                    onToggleFlag = onToggleFlag,
                                    onChord = onChord,
                                    isGameOver = gameOver,
                                    cellSize = cellSize
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Win/Lose Dialog (Unchanged)
    if (gameOver) {
        val title = if (gameWon) "🥳 Congratulations! You Won!" else "💥 Game Over!"
        val text = if (gameWon) "You found all the mines!" else "You hit a bomb!"
        val confirmText = "Play Again"

        AlertDialog(
            onDismissRequest = { /* No dismiss on game over */ },
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = restartGame) {
                    Text(confirmText)
                }
            }
        )
    }

    // Settings Dialog (Unchanged)
    if (showSettingsDialog) {
        SettingsDialog(
            currentDifficulty = currentDifficulty,
            currentBombCount = bombCount,
            currentGridWidth = gridWidth,
            currentGridHeight = gridHeight,
            onDismiss = { showSettingsDialog = false },
            onApply = { newDifficulty, newWidth, newHeight, newBombCount ->
                currentDifficulty = newDifficulty
                gridWidth = newWidth
                gridHeight = newHeight
                bombCount = newBombCount
                showSettingsDialog = false
                restartGame()
            }
        )
    }

    // References Dialog (Unchanged)
    if (showReferenceDialog) {
        ReferenceDialog(onDismiss = { showReferenceDialog = false })
    }
}

// -------------------------------------------------------------------------
// Settings Dialog Composable
// -------------------------------------------------------------------------

@Composable
fun SettingsDialog(
    currentDifficulty: Difficulty,
    currentBombCount: Int,
    currentGridWidth: Int,
    currentGridHeight: Int,
    onDismiss: () -> Unit,
    onApply: (Difficulty, Int, Int, Int) -> Unit
) {
    var selectedDifficulty by remember { mutableStateOf(currentDifficulty) }
    var bombInput by remember { mutableStateOf(currentBombCount.toString()) }
    var isError by remember { mutableStateOf(false) }

    // Use the current W/H for max bomb calculation
    val currentMaxBombs = (currentGridWidth * currentGridHeight) / 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Game Settings") },
        text = {
            Column {
                Text(
                    "Select Difficulty:",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Difficulty Selection Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Difficulty.entries.filter { it != Difficulty.CUSTOM }.forEach { difficulty ->
                        Button(
                            onClick = {
                                selectedDifficulty = difficulty
                                bombInput = difficulty.mines.toString()
                                isError = false
                            },
                            enabled = selectedDifficulty != difficulty
                        ) {
                            Text(difficulty.name.capitalize(), fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Custom Bomb Input
                Text(
                    "Custom Mine Count (Max: $currentMaxBombs)",
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                TextField(
                    value = bombInput,
                    onValueChange = { newValue ->
                        selectedDifficulty = Difficulty.CUSTOM
                        bombInput = newValue.filter { it.isDigit() }
                        val value = bombInput.toIntOrNull()
                        isError = value == null || value <= 0 || value > currentMaxBombs
                    },
                    label = { Text("Number of Mines") },
                    isError = isError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text(
                        "Must be a number between 1 and $currentMaxBombs.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newCount = bombInput.toIntOrNull()
                    if (newCount != null && newCount > 0 && newCount <= currentMaxBombs) {

                        if (selectedDifficulty != Difficulty.CUSTOM) {
                            // Apply predefined difficulty's dimensions and mines
                            onApply(
                                selectedDifficulty,
                                selectedDifficulty.width,
                                selectedDifficulty.height,
                                selectedDifficulty.mines
                            )
                        } else {
                            // Apply current dimensions and custom mine count
                            onApply(
                                Difficulty.CUSTOM,
                                currentGridWidth,
                                currentGridHeight,
                                newCount
                            )
                        }
                    } else {
                        isError = true
                    }
                },
                enabled = !isError
            ) {
                Text("Apply & Restart")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


// -------------------------------------------------------------------------
// Reference Dialog Composable
// -------------------------------------------------------------------------

@Composable
fun ReferenceDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("References and Resources") },
        text = {
            Column {
                Text(
                    "Resources used for the development of Gabo's Grid Logic:",
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 1. YouTube Link
                Text(
                    text = "▶️ YouTube Link:",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                // PLACEHOLDER: Replace with your actual link
                Text("https://www.youtube.com/your/tutorial/link")

                Spacer(Modifier.height(12.dp))

                // 2. PDF Resources
                Text(
                    text = "📄 Documentation (PDFs):",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                // PLACEHOLDER: Replace with actual document names/links
                Text("- PDF 1: Jetpack Compose Fundamentals (Link/File Name)")
                Text("- PDF 2: Minesweeper Algorithm Design (Link/File Name)")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}