package com.example.foodtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * S2: exportSchema = true writes the schema JSON to app/schemas so future versions have a
 * baseline to migrate from. Migration policy: on any schema change, bump [version] and add a
 * Migration in [getDatabase]. We deliberately do NOT use fallbackToDestructiveMigration, so a
 * missing migration fails loudly in development instead of silently wiping a user's inventory.
 */
@Database(entities = [FoodItem::class, Category::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class FoodDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao

    companion object {
        @Volatile
        private var INSTANCE: FoodDatabase? = null

        fun getDatabase(context: Context): FoodDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FoodDatabase::class.java,
                    "food_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
