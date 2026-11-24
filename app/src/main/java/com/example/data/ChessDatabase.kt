package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [GameRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ChessDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: ChessDatabase? = null

        fun getInstance(context: Context): ChessDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChessDatabase::class.java,
                    "albies_chess_offline.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
