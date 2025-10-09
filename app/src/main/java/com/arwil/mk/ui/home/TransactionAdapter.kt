package com.arwil.mk.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arwil.mk.R
import com.arwil.mk.ui.home.Transaction

class TransactionAdapter(
    private val transactions: List<Transaction>,
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_transaction_title)
        val category: TextView = view.findViewById(R.id.tv_transaction_category)
        val amount: TextView = view.findViewById(R.id.tv_transaction_amount)
        val deleteButton: ImageView = view.findViewById(R.id.iv_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.title.text = transaction.title
        holder.category.text = transaction.category

        val context = holder.itemView.context
        if (transaction.type == "INCOME") {
            holder.amount.text = "+ ${transaction.amount.toRupiahFormat()}"
            holder.amount.setTextColor(ContextCompat.getColor(context, R.color.green)) // Buat color green
        } else {
            holder.amount.text = "- ${transaction.amount.toRupiahFormat()}"
            holder.amount.setTextColor(ContextCompat.getColor(context, R.color.red)) // Buat color red
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClick(transaction)
        }
    }

    override fun getItemCount() = transactions.size
}