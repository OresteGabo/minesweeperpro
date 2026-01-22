package com.example.minesweeper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun CellButton(
    revealed: Boolean,
    value: Int,
    onClick: () -> Unit
) {
    val size = 32.dp

    Box(
        modifier = Modifier
            .size(size)
            .padding(2.dp)
            .background(
                color = when {
                    revealed && value == Game.BOMB -> Color.Red
                    revealed -> Color(0xFFEFEFEF)
                    else -> Color.DarkGray
                },
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(enabled = !revealed, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {

        if (revealed) {
            if (value == Game.BOMB) {
                Text("💣", fontSize = 18.sp)
            } else {
                Text(
                    text = value.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
