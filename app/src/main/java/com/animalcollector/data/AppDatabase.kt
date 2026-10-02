package com.animalcollector.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cards") data class CardEntity(@PrimaryKey val id: String, val name: String, val imageUri: String, val species: String, val breed: String?, val color: String?, val score: Int, val rarity: String, val createdAt: Long, val state: String)
@Entity(tableName = "daily_upload") data class DailyUploadEntity(@PrimaryKey val key: Int = 0, val day: String, val count: Int)
@Entity(tableName = "bag_slots", primaryKeys = ["rarity"]) data class BagSlotEntity(val rarity: String, val capacity: Int)
@Entity(tableName = "trades") data class TradeEntity(@PrimaryKey val id: String, val state: String, val nonce: String, val timestamp: Long)
@Dao interface CardDao { @Query("SELECT * FROM cards WHERE state != 'LOST' ORDER BY createdAt DESC") fun observeActive(): Flow<List<CardEntity>>; @Insert suspend fun insert(card: CardEntity); @Query("SELECT COUNT(*) FROM cards WHERE state != 'LOST'") suspend fun count(): Int }
@Dao interface DailyUploadDao { @Query("SELECT * FROM daily_upload WHERE `key`=0") suspend fun get(): DailyUploadEntity?; @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(value: DailyUploadEntity) }
@Database(entities=[CardEntity::class,DailyUploadEntity::class,BagSlotEntity::class,TradeEntity::class], version=1) abstract class AppDatabase: RoomDatabase() { abstract fun cards(): CardDao; abstract fun uploads(): DailyUploadDao }
