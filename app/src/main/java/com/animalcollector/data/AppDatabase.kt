package com.animalcollector.data
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cards") data class CardEntity(@PrimaryKey val id: String, val name: String, val imageUri: String, val species: String, val scientificName: String?, val family: String, val breed: String?, val color: String?, val confidence: Int, val description: String, val wikipediaQuery: String, val score: Int, val rarity: String, val createdAt: Long, val state: String)
@Entity(tableName = "daily_upload") data class DailyUploadEntity(@PrimaryKey val key: Int = 0, val day: String, val count: Int)
@Entity(tableName = "bag_slots", primaryKeys = ["rarity"]) data class BagSlotEntity(val rarity: String, val capacity: Int)
@Entity(tableName = "trades") data class TradeEntity(@PrimaryKey val id: String, val state: String, val nonce: String, val timestamp: Long)
@Dao interface CardDao { @Query("SELECT * FROM cards WHERE state != 'LOST' ORDER BY createdAt DESC") fun observeActive(): Flow<List<CardEntity>>; @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(card: CardEntity); @Query("SELECT COUNT(*) FROM cards WHERE state != 'LOST'") suspend fun count(): Int }
@Dao interface DailyUploadDao { @Query("SELECT * FROM daily_upload WHERE `key`=0") suspend fun get(): DailyUploadEntity?; @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(value: DailyUploadEntity) }
@Database(entities=[CardEntity::class,DailyUploadEntity::class,BagSlotEntity::class,TradeEntity::class], version=2)
abstract class AppDatabase: RoomDatabase() {
    abstract fun cards(): CardDao
    abstract fun uploads(): DailyUploadDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE cards ADD COLUMN scientificName TEXT")
                database.execSQL("ALTER TABLE cards ADD COLUMN family TEXT NOT NULL DEFAULT 'OTHER'")
                database.execSQL("ALTER TABLE cards ADD COLUMN confidence INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE cards ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE cards ADD COLUMN wikipediaQuery TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
