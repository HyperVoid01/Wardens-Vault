package com.soo.wardensvault

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/*
XP, Gold, login streak and counters - one document per user at users/{uid}/player_stats/stats
 */
class PlayerStatsDao(firestore: FirebaseFirestore) : FirestoreDao(firestore) {
    private fun statsDoc(userId: String) = col(userId, "player_stats").document("stats")

    suspend fun getStats(userId: String): PlayerStats? {
        return statsDoc(userId).get().await().toObject(PlayerStats::class.java)
    }

    suspend fun upsert(stats: PlayerStats) {
        statsDoc(stats.userId).set(stats).await()
    }

    /*
    Reads the stats, applies [change] and saves the result inside one Firestore transaction,
    so two updates can never overwrite each other (this replaces Room's withTransaction).
    [change] returns null to leave the stats untouched.
    Returns (before, after), or null when nothing was changed.
     */
    suspend fun update(
        userId: String,
        change: (PlayerStats) -> PlayerStats?
    ): Pair<PlayerStats, PlayerStats>? {
        val ref = statsDoc(userId)
        return firestore.runTransaction<Pair<PlayerStats, PlayerStats>?> { tx ->
            val current = tx.get(ref).toObject(PlayerStats::class.java) ?: PlayerStats(userId = userId)
            val updated = change(current)
            if (updated == null) {
                null
            } else {
                tx.set(ref, updated)
                Pair(current, updated)
            }
        }.await()
    }
}
