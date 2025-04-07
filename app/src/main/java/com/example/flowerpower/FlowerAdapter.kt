package com.example.flowerpower

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class FlowerAdapter(
    private val flowers: MutableList<Flower>,
    private val onAddToCart: (Flower) -> Unit,
    private val onItemLongClick: (Flower) -> Unit
) : RecyclerView.Adapter<FlowerAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val seasonTextView: TextView = itemView.findViewById(R.id.seasonTextView)
        val priceTextView: TextView = itemView.findViewById(R.id.priceTextView)
        val flowerImageView: ImageView = itemView.findViewById(R.id.flowerImageView)
        val addToCartImageView: ImageView = itemView.findViewById(R.id.addToCartImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_flower, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val flower = flowers[position]
        holder.nameTextView.text = flower.name
        holder.seasonTextView.text = "Сезон: ${flower.season}"
        holder.priceTextView.text = "Цена: ${flower.price} ₽"

        Glide.with(holder.itemView.context)
            .load(flower.photoUrl)
            .apply(RequestOptions().placeholder(R.drawable.placeholder_image).error(R.drawable.error_image))
            .into(holder.flowerImageView)

        holder.addToCartImageView.setOnClickListener {
            onAddToCart(flower)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(flower)
            true
        }
    }

    override fun getItemCount(): Int {
        return flowers.size
    }

    fun updateList(newFlowers: List<Flower>) {
        flowers.clear()
        flowers.addAll(newFlowers)
        notifyDataSetChanged()
    }
}
