package com.example.blockblast.data

import com.example.blockblast.model.Piece
import kotlin.random.Random

object PieceGenerator {
    private val COLORS = listOf(
        0xFF3E5F8A.toInt(),
        0xFF4E77AD.toInt(),
        0xFF002F55.toInt(),
        0xFF0067BB.toInt(),
        0xFF1560BD.toInt(),
        0xFF42AAFF.toInt(),
        0xFFA6BDD7.toInt(),
        0xFF004D99.toInt(),
    )

    private val SHAPES = listOf(
        arrayOf(intArrayOf(1)),
        arrayOf(intArrayOf(1, 1)),
        arrayOf(intArrayOf(1, 1, 1)),
        arrayOf(intArrayOf(1, 1, 1, 1)),
        arrayOf(
            intArrayOf(1, 1),
            intArrayOf(1, 1)
        ),
        arrayOf(
            intArrayOf(1, 1),
            intArrayOf(1, 0)
        ),
        arrayOf(
            intArrayOf(1, 1),
            intArrayOf(0, 1)
        ),
        arrayOf(
            intArrayOf(1, 0),
            intArrayOf(1, 1)
        ),
        arrayOf(
            intArrayOf(1, 0, 0),
            intArrayOf(1, 0, 0),
            intArrayOf(1, 1, 1)
        ),
        arrayOf(
            intArrayOf(1, 1, 1),
            intArrayOf(1, 0, 0),
            intArrayOf(1, 0, 0)
        ),
        arrayOf(
            intArrayOf(0, 1, 1),
            intArrayOf(1, 1, 0)
        ),
        arrayOf(
            intArrayOf(1, 1, 0),
            intArrayOf(0, 1, 1)
        ),
        arrayOf(
            intArrayOf(1, 1, 1),
            intArrayOf(0, 1, 0)
        ),
        arrayOf(
            intArrayOf(1, 1, 1),
            intArrayOf(1, 0, 0)
        ),
        arrayOf(
            intArrayOf(1, 1, 1),
            intArrayOf(1, 1, 1),
            intArrayOf(1, 1, 1)
        ),
    )

    fun getRandomPiece(): Piece {
        val shapeIndex = Random.nextInt(SHAPES.size)
        val colorIndex = Random.nextInt(COLORS.size)
        val shapeCopy = SHAPES[shapeIndex].map { it.clone() }.toTypedArray()
        return Piece(
            shape = shapeCopy,
            color = COLORS[colorIndex]
        )
    }

    fun getRandomPieces(count: Int): List<Piece> {
        return List(count) { getRandomPiece() }
    }

    fun getThreeRandomPieces(): List<Piece> {
        return getRandomPieces(3)
    }

    fun getAllShapes(): List<Piece> {
        return SHAPES.mapIndexed { index, shape ->
            val shapeCopy = shape.map { it.clone() }.toTypedArray()
            Piece(
                shape = shapeCopy,
                color = COLORS[index % COLORS.size],
                id = index
            )
        }
    }

    fun getTestPieces(): List<Piece> {
        return listOf(
            Piece(
                shape = arrayOf(
                    intArrayOf(1, 1),
                    intArrayOf(1, 1)
                ),
                color = COLORS[0],
                id = 1001
            ),
            Piece(
                shape = arrayOf(
                    intArrayOf(1, 1, 1, 1)
                ),
                color = COLORS[1],
                id = 1002
            ),
            Piece(
                shape = arrayOf(
                    intArrayOf(1, 0),
                    intArrayOf(1, 1)
                ),
                color = COLORS[2],
                id = 1003
            )
        )
    }
}