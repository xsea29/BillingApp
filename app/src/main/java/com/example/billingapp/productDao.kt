package com.example.billingapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update


@Dao
interface ProductDao {

    @Insert
    suspend fun insert(product: Product)

    @Insert
    suspend fun insertAll(products: List<Product>)

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<Product>

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    /**
     * Adds [change] (negative takes away) to the stock of the product called [name]. Products
     * that aren't counted (no stock) are left alone. If two products share a name, always the
     * same one is used, so what billing takes away, deleting the bill gives back.
     */
    @Query(
        "UPDATE products SET stock = stock + :change WHERE id = " +
                "(SELECT id FROM products WHERE name = :name AND stock IS NOT NULL ORDER BY id LIMIT 1)"
    )
    suspend fun adjustStock(name: String, change: Int)
}