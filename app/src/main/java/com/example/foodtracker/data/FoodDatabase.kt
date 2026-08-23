package com.example.foodtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FoodItem::class, Category::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FoodDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao

    companion object {
        @Volatile
        private var INSTANCE: FoodDatabase? = null

        // v1 -> v2: adds indices on FoodItem.category and FoodItem.expiryDate (see the
        // @Entity(indices = [...]) on FoodItem). The index names below must exactly match
        // Room's generated naming convention (index_<table>_<column>) or Room's schema
        // validation will fail at runtime when it compares this migration's end state
        // against the entity's compiled schema.
        //
        // No foreign key from FoodItem.category to Category.name is added here: SQLite has
        // no ALTER TABLE ... ADD CONSTRAINT, so a real FK would require rebuilding the whole
        // food_items table (create new table, copy rows, drop old, rename). Out of scope for
        // this pass.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_items_category` ON `food_items` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_items_expiryDate` ON `food_items` (`expiryDate`)")
            }
        }

        fun getDatabase(context: Context): FoodDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FoodDatabase::class.java,
                    "food_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
