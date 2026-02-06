package moe.memesta.vibeon.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TrackEntity::class, AlbumArtEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun albumArtDao(): AlbumArtDao
    
    companion object {
        @Volatile
        private var INSTANCE: LibraryDatabase? = null
        
        fun getInstance(context: Context): LibraryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LibraryDatabase::class.java,
                    "vibe_on_library.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
