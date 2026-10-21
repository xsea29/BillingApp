package com.example.billingapp

data class BillItem(

    val product: Product,

    val quantity: Int
) {

    fun amount(): Double {
        return product.price * quantity
    }
}