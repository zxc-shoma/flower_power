package com.example.flowerpower

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter(private val orders: List<Order>) : RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderCodeTextView: TextView = itemView.findViewById(R.id.orderCodeTextView)
        val totalCostTextView: TextView = itemView.findViewById(R.id.totalCostTextView)
        val paymentMethodTextView: TextView = itemView.findViewById(R.id.paymentMethodTextView)
        val deliveryMethodTextView: TextView = itemView.findViewById(R.id.DeliveryMethodTextView)
        val orderDateTextView: TextView = itemView.findViewById(R.id.orderDateTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]
        holder.orderCodeTextView.text = "Код заказа: ${order.orderCode}"
        holder.totalCostTextView.text = "Стоимость: ${order.totalCost} ₽"
        holder.paymentMethodTextView.text = "Способ оплаты: ${order.paymentMethod}"
        holder.deliveryMethodTextView.text = "Способ получения: ${order.deliveryAddress}"
        holder.orderDateTextView.text = "Дата заказа: ${formatDate(order.date)}"

        holder.itemView.setOnLongClickListener {
            showOrderDetailsDialog(holder.itemView.context, order)
            true
        }
    }

    override fun getItemCount(): Int = orders.size

    private fun showOrderDetailsDialog(context: Context, order: Order) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_order_details, null)
        val orderDetailsTextView: TextView = dialogView.findViewById(R.id.orderDetailsTextView)
        val closeButton: Button = dialogView.findViewById(R.id.closeButton)

        // Формируем строку с товарами
        val itemsString = order.items.joinToString(separator = "\n") { "${it.name} - ${it.quantity} шт." }
        orderDetailsTextView.text = itemsString

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        closeButton.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }


    private fun formatDate(date: Date): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(date)
    }
}
