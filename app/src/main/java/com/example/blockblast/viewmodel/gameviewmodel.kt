package com.example.blockblast.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blockblast.FirebaseManager
import com.example.blockblast.data.PieceGenerator
import com.example.blockblast.model.Piece
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(private val context: Context) : ViewModel() {

    companion object {
        const val BOARD_SIZE = 8
    }

    private val _board = MutableStateFlow(emptyBoard())
    val board: StateFlow<Array<IntArray>> = _board.asStateFlow()

    private val _boardColors = MutableStateFlow(emptyBoardColors())
    val boardColors: StateFlow<Array<IntArray>> = _boardColors.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _availablePieces = MutableStateFlow<List<Piece>>(emptyList())
    val availablePieces: StateFlow<List<Piece>> = _availablePieces.asStateFlow()

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    private val _waveEvent = MutableSharedFlow<WaveEvent>()
    val waveEvent: SharedFlow<WaveEvent> = _waveEvent.asSharedFlow()

    data class WaveEvent(val centerRow: Float, val centerCol: Float)

    private var selectedPiece: Piece? = null

    init {
        startNewGame()
        FirebaseManager.init()
    }

    private fun emptyBoard(): Array<IntArray> {
        return Array(BOARD_SIZE) { IntArray(BOARD_SIZE) { 0 } }
    }

    private fun emptyBoardColors(): Array<IntArray> {
        return Array(BOARD_SIZE) { IntArray(BOARD_SIZE) { 0 } }
    }

    private fun copyBoard(board: Array<IntArray>): Array<IntArray> {
        return Array(board.size) { i -> board[i].copyOf() }
    }

    private fun copyBoardColors(boardColors: Array<IntArray>): Array<IntArray> {
        return Array(boardColors.size) { i -> boardColors[i].copyOf() }
    }

    fun startNewGame() {
        _board.value = emptyBoard()
        _boardColors.value = emptyBoardColors()
        _score.value = 0
        _isGameOver.value = false
        _availablePieces.value = PieceGenerator.getThreeRandomPieces()
        selectedPiece = null
    }

    fun selectPiece(piece: Piece) {
        if (_isGameOver.value) return
        selectedPiece = piece
    }

    fun placeSelectedPiece(row: Int, col: Int): Boolean {
        val piece = selectedPiece ?: return false
        if (_isGameOver.value) return false

        if (!canPlacePiece(_board.value, piece, row, col)) return false

        val newBoard = copyBoard(_board.value)
        val newBoardColors = copyBoardColors(_boardColors.value)
        placePieceOnBoard(newBoard, newBoardColors, piece, row, col)

        val (clearedBoard, clearedColors, linesCleared, removedRows, removedCols) = clearFullLines(newBoard, newBoardColors)
        val pointsEarned = calculatePoints(linesCleared)
        _score.value += pointsEarned
        _board.value = clearedBoard
        _boardColors.value = clearedColors

        val updatedPieces = _availablePieces.value.toMutableList()
        val pieceIndex = updatedPieces.indexOfFirst { it == piece }
        if (pieceIndex != -1) {
            updatedPieces[pieceIndex] = PieceGenerator.getRandomPiece()
        }
        _availablePieces.value = updatedPieces

        selectedPiece = null

        if (linesCleared > 0) {
            val centerRow = (removedRows.sum().toFloat() / removedRows.size).coerceIn(0f, (BOARD_SIZE - 1).toFloat())
            val centerCol = (removedCols.sum().toFloat() / removedCols.size).coerceIn(0f, (BOARD_SIZE - 1).toFloat())
            viewModelScope.launch {
                _waveEvent.emit(WaveEvent(centerRow, centerCol))
            }
        }

        checkAndSetGameOver()
        return true
    }

    private fun checkAndSetGameOver() {
        val currentBoard = _board.value
        val currentPieces = _availablePieces.value
        for (piece in currentPieces) {
            for (row in 0 until BOARD_SIZE) {
                for (col in 0 until BOARD_SIZE) {
                    if (canPlacePiece(currentBoard, piece, row, col)) {
                        _isGameOver.value = false
                        return
                    }
                }
            }
        }
        _isGameOver.value = true
        saveScoreToFirebase()
    }

    private fun saveScoreToFirebase() {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val playerName = prefs.getString("user_name", "Player") ?: "Player"
        val currentScore = _score.value

        viewModelScope.launch {
            try {
                FirebaseManager.saveScore(playerName, currentScore)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun canPlacePiece(board: Array<IntArray>, piece: Piece, row: Int, col: Int): Boolean {
        if (row + piece.height > BOARD_SIZE || col + piece.width > BOARD_SIZE) return false
        for (i in piece.shape.indices) {
            for (j in piece.shape[i].indices) {
                if (piece.shape[i][j] == 1 && board[row + i][col + j] == 1) return false
            }
        }
        return true
    }

    private fun placePieceOnBoard(board: Array<IntArray>, boardColors: Array<IntArray>, piece: Piece, row: Int, col: Int) {
        for (i in piece.shape.indices) {
            for (j in piece.shape[i].indices) {
                if (piece.shape[i][j] == 1) {
                    board[row + i][col + j] = 1
                    boardColors[row + i][col + j] = piece.color
                }
            }
        }
    }

    private fun clearFullLines(board: Array<IntArray>, boardColors: Array<IntArray>): ClearLinesResult {
        val newBoard = copyBoard(board)
        val newColors = copyBoardColors(boardColors)
        var linesCleared = 0
        val removedRows = mutableListOf<Int>()
        val removedCols = mutableListOf<Int>()

        for (row in 0 until BOARD_SIZE) {
            if (newBoard[row].all { it == 1 }) {
                for (col in 0 until BOARD_SIZE) {
                    newBoard[row][col] = 0
                    newColors[row][col] = 0
                }
                linesCleared++
                removedRows.add(row)
            }
        }

        for (col in 0 until BOARD_SIZE) {
            var full = true
            for (row in 0 until BOARD_SIZE) {
                if (newBoard[row][col] == 0) {
                    full = false
                    break
                }
            }
            if (full) {
                for (row in 0 until BOARD_SIZE) {
                    newBoard[row][col] = 0
                    newColors[row][col] = 0
                }
                linesCleared++
                removedCols.add(col)
            }
        }
        return ClearLinesResult(newBoard, newColors, linesCleared, removedRows, removedCols)
    }

    data class ClearLinesResult(
        val board: Array<IntArray>,
        val colors: Array<IntArray>,
        val linesCleared: Int,
        val removedRows: List<Int>,
        val removedCols: List<Int>
    )

    private fun calculatePoints(linesCleared: Int): Int = when (linesCleared) {
        0 -> 0
        1 -> 100
        2 -> 250
        3 -> 450
        4 -> 700
        else -> 1000
    }
}