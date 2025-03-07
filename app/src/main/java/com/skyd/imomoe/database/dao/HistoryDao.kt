package com.skyd.imomoe.database.dao

import androidx.room.*
import com.skyd.imomoe.bean.HistoryBean
import com.skyd.imomoe.config.Const.Database.AppDataBase.HISTORY_TABLE_NAME
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHistory(historyBean: HistoryBean)

    //按照时间戳顺序，从大到小。最后搜索的元组在最上方（下标0）显示
    @Query(value = "SELECT * FROM $HISTORY_TABLE_NAME ORDER BY time DESC")
    fun getHistoryList(): MutableList<HistoryBean>

    @Query(value = "SELECT * FROM $HISTORY_TABLE_NAME WHERE animeUrl = :animeUrl")
    fun getHistory(animeUrl: String): HistoryBean?

    @Query("SELECT * FROM $HISTORY_TABLE_NAME WHERE animeUrl = :animeUrl")
    fun getHistoryFlow(animeUrl: String): Flow<HistoryBean?>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateHistory(historyBean: HistoryBean)

    @Query(value = "UPDATE $HISTORY_TABLE_NAME SET animeTitle = :animeTitle WHERE animeUrl = :animeUrl")
    fun updateHistoryTitle(animeUrl: String, animeTitle: String)

    @Query(value = "DELETE FROM $HISTORY_TABLE_NAME WHERE animeUrl = :animeUrl")
    fun deleteHistory(animeUrl: String)

    @Query(value = "DELETE FROM $HISTORY_TABLE_NAME")
    fun deleteAllHistory()

    // 获取记录条数
    @Query(value = "SELECT COUNT(1) FROM $HISTORY_TABLE_NAME")
    fun getHistoryCount(): Long
}