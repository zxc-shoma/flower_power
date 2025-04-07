package com.example.flowerpower


object CartManager {
    val cartItems = mutableListOf<Flower>()

    fun addToCart(flower: Flower) {
        // Проверка, есть ли цветок с таким же названием
        val existingItem = cartItems.find { it.name == flower.name }
        if (existingItem != null) {
            // Увеличиваем количество, если цветок уже есть
            existingItem.quantity += flower.quantity
        } else {
            // Если такого цветка нет, добавляем его
            cartItems.add(flower)
        }
    }

    fun getTotalCost(): Int {
        return cartItems.sumOf { it.price * it.quantity }
    }
}
