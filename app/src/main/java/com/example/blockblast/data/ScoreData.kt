package com.example.blockblast.data

data class ScoreData(
    val playerName: String = "",
    val score: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)