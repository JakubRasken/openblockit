package com.openblockit.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "days")
data class DayEntity(
    @PrimaryKey val date: String,
    val minutes: Int,
    val sessions: Int,
)

@Dao
interface DayDao {
    @Query("SELECT * FROM days WHERE date = :date")
    fun byDate(date: String): DayEntity?

    @Query("SELECT * FROM days ORDER BY date")
    fun all(): List<DayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(day: DayEntity)
}

@Database(entities = [DayEntity::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun days(): DayDao

    companion object {
        @Volatile
        private var instance: AppDb? = null

        fun get(context: Context): AppDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDb::class.java,
                    "openblockit.db",
                ).build().also { instance = it }
            }
    }
}
