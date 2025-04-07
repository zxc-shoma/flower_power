package com.example.flowerpower

import java.util.Date

data class Order(
    val orderCode: String = "",
    val items: List<Flower> = emptyList(),
    val totalCost: Int = 0,
    val paymentMethod: String = "",
    val deliveryAddress: String = "",
    val date: Date = Date()
)