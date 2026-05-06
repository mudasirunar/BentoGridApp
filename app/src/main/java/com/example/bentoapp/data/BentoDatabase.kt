package com.example.bentoapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BentoEntity::class, ProjectEntity::class],
    version = 4,
    exportSchema = false
)
abstract class BentoDatabase : RoomDatabase() {
    abstract fun bentoDao(): BentoDao

    companion object {
        @Volatile
        private var INSTANCE: BentoDatabase? = null
        // ── MIGRATION: 1 → 2 ──
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bento_tiles ADD COLUMN shapeIndex INTEGER NOT NULL DEFAULT 0")
            }
        }

        // ── MIGRATION: 2 → 3 ──
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create the perfect new table (Removes imageOrientation)
                db.execSQL("""
            CREATE TABLE projects_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                imageUrl TEXT NOT NULL DEFAULT '',
                isBackground INTEGER NOT NULL DEFAULT 0,
                isRounded INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())

                // 2. Copy data (We skip imageOrientation entirely)
                db.execSQL("""
            INSERT INTO projects_new (id, name, imageUrl, isBackground)
            SELECT id, name, imageUrl, isBackground FROM projects
        """.trimIndent())

                // 3. Swap tables
                db.execSQL("DROP TABLE projects")
                db.execSQL("ALTER TABLE projects_new RENAME TO projects")


                // --- STEP 2: FIX THE BENTO_TILES TABLE ---
                // This stops "duplicate column: imageUri" by creating a fresh table
                // with ALL styling columns included from the start.

                // 1. Create the perfect new tiles table
                db.execSQL("""
            CREATE TABLE bento_tiles_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                projectId INTEGER NOT NULL,
                title TEXT NOT NULL DEFAULT '',
                content TEXT NOT NULL DEFAULT '',
                imageUri TEXT,
                shapeIndex INTEGER NOT NULL DEFAULT 0,
                backgroundColor INTEGER,
                textAlignment INTEGER NOT NULL DEFAULT 0,
                textColor INTEGER NOT NULL DEFAULT -1,
                isBold INTEGER NOT NULL DEFAULT 0,
                isItalic INTEGER NOT NULL DEFAULT 0,
                isUnderline INTEGER NOT NULL DEFAULT 0,
                textSizeOffset INTEGER NOT NULL DEFAULT 0,
                isReversed INTEGER NOT NULL DEFAULT 0,
                contentTextColor INTEGER NOT NULL DEFAULT -1,
                isContentBold INTEGER NOT NULL DEFAULT 0,
                isContentItalic INTEGER NOT NULL DEFAULT 0,
                isContentUnderline INTEGER NOT NULL DEFAULT 0,
                contentSizeOffset INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

                // 2. Copy existing data (Copying only the columns we know existed in v2)
                db.execSQL("""
            INSERT INTO bento_tiles_new (id, projectId, title, content, imageUri, shapeIndex, backgroundColor)
            SELECT id, projectId, title, content, imageUri, shapeIndex, backgroundColor FROM bento_tiles
        """.trimIndent())

                // 3. Swap tables
                db.execSQL("DROP TABLE bento_tiles")
                db.execSQL("ALTER TABLE bento_tiles_new RENAME TO bento_tiles")
            }
        }

        // Removes isRounded and adds shapeIndex to the projects table
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create a new table with the updated schema
                db.execSQL("""
            CREATE TABLE projects_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                imageUrl TEXT NOT NULL DEFAULT '',
                isBackground INTEGER NOT NULL DEFAULT 0,
                shapeIndex INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())

                // 2. Copy data from old table to new table
                // We map 'isRounded' value directly to 'shapeIndex' (0 stays 0, 1 stays 1)
                db.execSQL("""
            INSERT INTO projects_new (id, name, imageUrl, isBackground, shapeIndex)
            SELECT id, name, imageUrl, isBackground, isRounded FROM projects
        """.trimIndent())

                // 3. Delete the old table
                db.execSQL("DROP TABLE projects")

                // 4. Rename the new table to the original name
                db.execSQL("ALTER TABLE projects_new RENAME TO projects")
            }
        }


        fun getDatabase(context: Context): BentoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BentoDatabase::class.java,
                    "bento_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}