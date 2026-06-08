package com.example.blockblast.model
import kotlin.random.Random
data class Piece(
    val shape: Array<IntArray>,
    val color: Int,
    val id: Int = Random.nextInt()
) {
    val width: Int
        get() = shape.maxOfOrNull { it.size } ?: 0
    val height: Int
        get() = shape.size

    fun rotate(): Piece {
        val rotated = Array(shape[0].size) { IntArray(shape.size) }
        for (i in shape.indices) {
            for (j in shape[i].indices) {
                rotated[j][shape.size - 1 - i] = shape[i][j]
            }
        }
        return Piece(rotated, color)
    }

    fun hasBlockAt(row: Int, col: Int): Boolean {
        return row in shape.indices &&
                col in shape[row].indices &&
                shape[row][col] == 1
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Piece
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int = id

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Piece(id=$id, color=$color, shape=\n")
        shape.forEach { row ->
            sb.append("  ")
            row.forEach { cell ->
                sb.append(if (cell == 1) "█" else "·")
            }
            sb.append("\n")
        }
        sb.append(")")
        return sb.toString()
    }
}