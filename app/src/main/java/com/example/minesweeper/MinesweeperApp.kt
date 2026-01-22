package com.example.minesweeper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MinesweeperApp() {
    var game by remember { mutableStateOf(Game(10, 10, 12)) }
    var revealed by remember {
        mutableStateOf(Array(game.getWidth()) { BooleanArray(game.getHeight()) })
    }
    var gameOver by remember { mutableStateOf(false) }

    if (gameOver) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Game Over") },
            text = { Text("You clicked on a bomb!") },
            confirmButton = {
                TextButton(onClick = {
                    game = Game(10, 10, 12)
                    revealed = Array(game.getWidth()) { BooleanArray(game.getHeight()) }
                    gameOver = false
                }) {
                    Text("Restart")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Minesweeper", fontSize = 28.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .background(Color.LightGray, RoundedCornerShape(8.dp))
                .padding(6.dp)
        ) {
            Column {
                for (x in 0 until game.getHeight()) {
                    Row {
                        for (y in 0 until game.getWidth()) {

                            val cellRevealed = revealed[x][y]
                            val value = game.getCell(x, y)

                            CellButton(
                                revealed = cellRevealed,
                                value = value.value,
                                onClick = {
                                    if (value.value == Game.BOMB) {
                                        gameOver = true
                                    } else {
                                        revealed = revealed.copyOf().also { it[x][y] = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
