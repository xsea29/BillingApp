package com.example.billingapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update


@Dao
interface ProductDao {

    // Returns the new product's id.
    @Insert
    suspend fun insert(product: Product): Long

    @Insert
    suspend fun insertAll(products: List<Product>)

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<Product>

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)
}
