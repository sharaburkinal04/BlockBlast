package com.example.blockblast

import android.util.Log
import com.example.blockblast.data.ScoreData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private lateinit var database: DatabaseReference

    fun init() {
        database = FirebaseDatabase.getInstance().getReference("users")
        Log.d(TAG, "Firebase initialized")
    }

    fun saveScore(playerName: String, score: Int) {
        if (score <= 0) return
        val userRef = database.child(playerName)
        userRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val currentScore = mutableData.child("score").getValue(Int::class.java) ?: 0
                if (score > currentScore) {
                    mutableData.child("playerName").value = playerName
                    mutableData.child("score").value = score
                    mutableData.child("timestamp").value = System.currentTimeMillis()
                }

                return Transaction.success(mutableData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e(TAG, "Ошибка записи: ${error.message}")
                } else {
                    Log.d(TAG, "Рекорд успешно обновлен: $score")
                }
            }
        })
    }

    fun getTopScores(): Flow<List<ScoreData>> = callbackFlow {
        val query = database.orderByChild("score").limitToLast(50)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val scores = mutableListOf<ScoreData>()
                for (child in snapshot.children) {
                    val score = child.getValue(ScoreData::class.java)
                    if (score != null) {
                        scores.add(score)
                    }
                }
                trySend(scores.sortedByDescending { it.score })
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }
}