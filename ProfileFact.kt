package fr.surveypilot.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile_facts")
data class ProfileFact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val question: String,
    val answer: String,
    val confirmedByUser: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
