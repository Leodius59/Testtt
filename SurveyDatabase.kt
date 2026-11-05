package fr.surveypilot.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ProfileFact::class], version = 1, exportSchema = false)
abstract class SurveyDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile private var INSTANCE: SurveyDatabase? = null
        fun get(context: Context): SurveyDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                SurveyDatabase::class.java,
                "surveypilot-local.db"
            ).build().also { INSTANCE = it }
        }
    }
}
