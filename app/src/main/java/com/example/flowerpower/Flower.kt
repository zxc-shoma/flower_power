package com.example.flowerpower

data class Flower(
    val compatible: List<String> = emptyList(),
    val name: String = "",
    val occasions: List<String> = emptyList(),
    val photoUrl: String ="",
    val price: Int = 0,
    val season: String = "",
    val description: String ="",
    var quantity: Int = 1




)