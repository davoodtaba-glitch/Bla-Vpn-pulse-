package com.xraypulse.app.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.xraypulse.app.data.model.ProtocolType
import com.xraypulse.app.data.model.ServerProfile
import com.xraypulse.app.data.model.StreamSecurity
import com.xraypulse.app.data.model.Subscription
import com.xraypulse.app.data.model.TransportNetwork
import kotlinx.coroutines.flow.Flow

class Converters {
    @TypeConverter fun fromProtocol(v: ProtocolType): String = v.name
    @TypeConverter fun toProtocol(v: String): ProtocolType = ProtocolType.valueOf(v)
    @TypeConverter fun fromNetwork(v: TransportNetwork): String = v.name
    @TypeConverter fun toNetwork(v: String): TransportNetwork = TransportNetwork.valueOf(v)
    @TypeConverter fun fromSecurity(v: StreamSecurity): String = v.name
    @TypeConverter fun toSecurity(v: String): StreamSecurity = StreamSecurity.valueOf(v)
}

@Dao
interface ServerDao {
    /** Stable list order — do not pin selected server to top (avoids jump on select). */
    @Query("SELECT * FROM servers ORDER BY updatedAt DESC, id DESC")
    fun observeAll(): Flow<List<ServerProfile>>

    @Query("SELECT * FROM servers ORDER BY updatedAt DESC, id DESC")
    suspend fun getAll(): List<ServerProfile>

    @Query("SELECT * FROM servers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ServerProfile?

    @Query("SELECT * FROM servers WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelected(): ServerProfile?

    @Query("SELECT * FROM servers WHERE isSelected = 1 LIMIT 1")
    fun observeSelected(): Flow<ServerProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(server: ServerProfile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(servers: List<ServerProfile>): List<Long>

    @Update
    suspend fun update(server: ServerProfile)

    @Query("UPDATE servers SET isSelected = 0")
    suspend fun clearSelection()

    @Query("UPDATE servers SET isSelected = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun select(id: Long)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM servers WHERE subscriptionId = :subId")
    suspend fun deleteBySubscription(subId: Long)

    @Query("UPDATE servers SET latencyMs = :latency WHERE id = :id")
    suspend fun updateLatency(id: Long, latency: Long)

    @Query("SELECT COUNT(*) FROM servers")
    suspend fun count(): Int
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY id DESC")
    fun observeAll(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions")
    suspend fun getAll(): List<Subscription>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getById(id: Long): Subscription?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sub: Subscription): Long

    @Update
    suspend fun update(sub: Subscription)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(
    entities = [ServerProfile::class, Subscription::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "xraypulse.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
