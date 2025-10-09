package com.arwil.mk.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arwil.mk.R

// Definisikan tipe view
private const val VIEW_TYPE_DATE = 0
private const val VIEW_TYPE_TRANSACTION = 1

class TransactionAdapter(
    private var items: List<ListItem>, // Terima List dari sealed class
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // ViewHolder untuk menampilkan tanggal
    class DateHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.tv_date_header)
    }

    // ViewHolder untuk menampilkan transaksi
    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_transaction_title)
        val category: TextView = view.findViewById(R.id.tv_transaction_category)
        val amount: TextView = view.findViewById(R.id.tv_transaction_amount)
        val deleteButton: ImageView = view.findViewById(R.id.iv_delete)
        // val icon: ImageView = view.findViewById(R.id.iv_transaction_icon) // Jika perlu
    }

    // Fungsi ini menentukan tipe view pada posisi tertentu
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.DateHeader -> VIEW_TYPE_DATE
            is ListItem.TransactionItem -> VIEW_TYPE_TRANSACTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_DATE) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_date_header, parent, false)
            DateHeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction, parent, false)
            TransactionViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val currentItem = items[position]) {
            is ListItem.DateHeader -> {
                (holder as DateHeaderViewHolder).dateText.text = currentItem.date
            }
            is ListItem.TransactionItem -> {
                val transactionHolder = (holder as TransactionViewHolder)
                val transaction = currentItem.transaction
                transactionHolder.title.text = transaction.title
                transactionHolder.category.text = transaction.category

                val context = holder.itemView.context
                if (transaction.type == "INCOME") {
                    transactionHolder.amount.text = "+ ${transaction.amount.toRupiahFormat()}"
                    transactionHolder.amount.setTextColor(ContextCompat.getColor(context, R.color.green))
                } else {
                    transactionHolder.amount.text = "- ${transaction.amount.toRupiahFormat()}"
                    transactionHolder.amount.setTextColor(ContextCompat.getColor(context, R.color.red))
                }

                transactionHolder.deleteButton.setOnClickListener {
                    onDeleteClick(transaction)
                }
            }
        }
    }

    override fun getItemCount() = items.size

    // Fungsi untuk mengupdate data di adapter
    fun updateData(newItems: List<ListItem>) {
        items = newItems
        notifyDataSetChanged() // Untuk kesederhanaan, kita gunakan ini
    }
}