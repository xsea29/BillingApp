package com.example.billingapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query


@Dao
interface ProductDao {

    @Insert
    suspend fun insert(product: Product)

    @Insert
    suspend fun insertAll(products: List<Product>)

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<Product>

    @Delete
    suspend fun delete(product: Product)
}