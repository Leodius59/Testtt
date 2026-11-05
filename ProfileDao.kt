package fr.surveypilot.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile_facts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProfileFact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fact: ProfileFact)

    @Query("DELETE FROM profile_facts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM profile_facts")
    suspend fun deleteAll()
}
