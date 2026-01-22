package com.example.minesweeper

import java.util.Random

// Data class to represent the state of a single cell on the board
data class Cell(
    val value: Int, // -1 for bomb, 0-8 for surrounding bomb count
    var isRevealed: Boolean = false,
    var isFlagged: Boolean = false
)

class IllegalNumberOfBombsException :
    Exception("Illegal number of bombs (must be between 1 and h * w / 2)")

class Game(private val width: Int, private val height: Int, private val bombCounter: Int) {

    companion object {
        const val EMPTY = 0
        const val BOMB = -1

        val DIRS = listOf(
            -1 to -1, -1 to 0, -1 to 1,
            0 to -1,          0 to 1,
            1 to -1,  1 to 0,  1 to 1
        )
    }

    private val board: Array<Array<Cell>> = Array(height) { Array(width) { Cell(EMPTY) } }
    private var revealedCount = 0
    private var flagsPlacedCount = 0
    private var currentPlayer = "ORESTE"
    private var bestTimeInSeconds = 0
    private var currentTimeInSeconds = 0
    private var bestPlayer = "ORESTE"

    // 💡 NEW STATE: Tracks if the board has been initialized (bombs placed)
    private var isInitialized = false


    @Throws(IllegalNumberOfBombsException::class)
    // 💡 UPDATED: Takes the first click coordinates (x0, y0) to guarantee safety
    private fun initBoard(x0: Int, y0: Int) {
        if (!numberOfBombsLegal(bombCounter)) throw IllegalNumberOfBombsException()
        if (isInitialized) return

        // 1. Identify safe zone (first click and its neighbors)
        val safeCells = mutableSetOf(x0 to y0)
        for ((dx, dy) in DIRS) {
            val nx = x0 + dx
            val ny = y0 + dy
            if (nx in 0 until height && ny in 0 until width) {
                safeCells.add(nx to ny)
            }
        }

        // 2. Place bombs outside the safe zone
        var bombsToPlace = bombCounter
        val random = Random()
        while (bombsToPlace > 0) {
            val x = random.nextInt(height)
            val y = random.nextInt(width)

            if (!safeCells.contains(x to y) && board[x][y].value != BOMB) {
                board[x][y] = Cell(BOMB)
                bombsToPlace--
            }
        }

        // 3. Calculate neighbor counts
        calculateNeighborBombs()
        isInitialized = true
    }

    private fun calculateNeighborBombs() {
        // Iterate over Rows (x)
        for (x in 0 until height) {
            // Iterate over Columns (y)
            for (y in 0 until width) {
                if (board[x][y].value == BOMB) continue

                var count = 0
                for ((dx, dy) in DIRS) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until height && ny in 0 until width) {
                        if (board[nx][ny].value == BOMB) count++
                    }
                }
                board[x][y] = Cell(count)
            }
        }
    }

    // --- Public Game Interaction Methods ---

    fun getCell(x: Int, y: Int): Cell = board[x][y]

    fun getWidth() = width
    fun getHeight() = height
    fun getBombCount() = bombCounter
    fun getFlagCount() = flagsPlacedCount
    fun getUnrevealedNonBombCells() = (width * height) - bombCounter

    fun getBestTimeInSeconds() = bestTimeInSeconds
    fun getCurrentPlayer() = currentPlayer
    fun setBestTimeInSeconds(seconds: Int) {
        bestTimeInSeconds = seconds
    }
    fun setCurrentPlayer(player: String) {
        currentPlayer = player
    }


    // 💡 UPDATED REVEAL: Handles initialization
    fun reveal(x: Int, y: Int): Boolean {
        // 1. Initialize board if this is the first click
        if (!isInitialized) {
            initBoard(x, y)
        }

        if (x !in 0 until height || y !in 0 until width) return false

        val cell = board[x][y]
        if (cell.isRevealed || cell.isFlagged) return false

        cell.isRevealed = true
        revealedCount++

        if (cell.value == BOMB) return true

        // Recursive reveal for EMPTY cells
        if (cell.value == EMPTY) {
            for ((dx, dy) in DIRS) {
                val nx = x + dx
                val ny = y + dy
                reveal(nx, ny)
            }
        }
        return false
    }

    // 💡 NEW: CHORD REVEAL LOGIC
    fun chordReveal(x: Int, y: Int): Boolean {
        val centerCell = board[x][y]
        if (!centerCell.isRevealed || centerCell.value == EMPTY || centerCell.value == BOMB) return false

        var flagsAround = 0
        var hitBomb = false

        // 1. Count flags around the center cell
        for ((dx, dy) in DIRS) {
            val nx = x + dx
            val ny = y + dy
            if (nx in 0 until height && ny in 0 until width) {
                if (board[nx][ny].isFlagged) {
                    flagsAround++
                }
            }
        }

        // 2. If flags match the cell's number, reveal neighbors
        if (flagsAround == centerCell.value) {
            for ((dx, dy) in DIRS) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until height && ny in 0 until width) {
                    val neighbor = board[nx][ny]

                    // Only reveal unflagged, unrevealed neighbors
                    if (!neighbor.isFlagged && !neighbor.isRevealed) {
                        // Use the main reveal function to trigger recursion/bomb check
                        val isBomb = reveal(nx, ny)
                        if (isBomb) {
                            hitBomb = true
                        }
                    }
                }
            }
        }

        return hitBomb
    }

    fun toggleFlag(x: Int, y: Int) {
        if (!isInitialized) return // Prevent flagging before game starts (optional)

        // ... (rest of the toggleFlag logic remains the same)
        if (x !in 0 until height || y !in 0 until width) return

        val cell = board[x][y]
        if (cell.isRevealed) return

        if (cell.isFlagged) {
            flagsPlacedCount--
        } else {
            flagsPlacedCount++
        }

        cell.isFlagged = !cell.isFlagged
    }

    fun checkWin(): Boolean {
        return revealedCount == getUnrevealedNonBombCells()
    }

    private fun numberOfBombsLegal(nb: Int): Boolean =
        nb > 0 && nb <= width * height / 2
}