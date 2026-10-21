package com.example.billingapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Dao
interface CustomerDao {

    @Insert
    suspend fun insert(customer: Customer)

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomers(): List<Customer>

    @Delete
    suspend fun delete(customer: Customer)
}