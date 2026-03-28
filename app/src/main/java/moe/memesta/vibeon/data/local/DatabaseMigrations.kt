package moe.memesta.vibeon.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from v3 to v4: Add dedup fields
 * - Add `source` column with default "pc"
 * - Add `canonicalId` column calculated from title+artist+album
 * - Add `localPath` column for cached/downloaded files
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns
        database.execSQL("ALTER TABLE tracks ADD COLUMN source TEXT NOT NULL DEFAULT 'pc'")
        database.execSQL("ALTER TABLE tracks ADD COLUMN canonicalId TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE tracks ADD COLUMN localPath TEXT")
        
        // Populate canonicalId for existing tracks
        database.execSQL("""
            UPDATE tracks SET canonicalId = (
                SELECT substr(hex(zeroblob(32)), 1, 16) 
                FROM (
                    SELECT lower(title || '|' || artist || '|' || album) as key
                ) 
                WHERE key = lower(title || '|' || artist || '|' || album)
            )
        """.trimIndent())
        
        // Create index on canonicalId
        database.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_canonicalId ON tracks(canonicalId)")
    }
}
