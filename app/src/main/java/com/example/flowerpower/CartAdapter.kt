package com.example.flowerpower

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class CartAdapter(
    private val cartItems: MutableList<Flower>,
    private val updateTotalCost: () -> Unit
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val flowerImageView: ImageView = itemView.findViewById(R.id.flowerImageView)
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val quantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)
        val pricePerUnitTextView: TextView = itemView.findViewById(R.id.pricePerUnitTextView)
        val priceTextView: TextView = itemView.findViewById(R.id.priceTextView)
        val addButton: ImageView = itemView.findViewById(R.id.addButton)
        val removeButton: ImageView = itemView.findViewById(R.id.removeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val flower = cartItems[position]
        holder.nameTextView.text = flower.name
        holder.quantityTextView.text = "Количество: ${flower.quantity}"
        holder.pricePerUnitTextView.text = "Цена за 1 шт.: ${flower.price} ₽"
        holder.priceTextView.text = "Стоимость: ${flower.price * flower.quantity} ₽"

        Glide.with(holder.itemView.context)
            .load(flower.photoUrl)
            .apply(RequestOptions().placeholder(R.drawable.placeholder_image).error(R.drawable.error_image))
            .into(holder.flowerImageView)

        holder.addButton.setOnClickListener {
            flower.quantity += 1
            notifyItemChanged(position)
            updateTotalCost()
        }

        holder.removeButton.setOnClickListener {
            if (position < 0 || position >= cartItems.size) {
                return@setOnClickListener // Безопасность: предотвращаем вылет
            }

            val flower = cartItems[position]

            if (flower.quantity > 1) {
                // Уменьшаем количество цветка
                flower.quantity -= 1
                notifyItemChanged(position)
            } else {
                // Удаляем цветок из списка
                cartItems.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, cartItems.size) // Обновляем оставшиеся элементы
            }

            // Обновляем общую стоимость
            updateTotalCost()
        }
    }

    override fun getItemCount(): Int = cartItems.size
}

