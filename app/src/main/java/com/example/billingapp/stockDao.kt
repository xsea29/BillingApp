package com.example.billingapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface StockDao {

    @Insert
    suspend fun insertAll(entries: List<StockEntry>)

    @Query("SELECT * FROM stock_entries ORDER BY addedAt")
    suspend fun getAll(): List<StockEntry>

    @Query("DELETE FROM stock_entries WHERE id = :id")
    suspend fun deleteById(id: Int)
}
