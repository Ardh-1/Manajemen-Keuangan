package com.arwil.mk.ui.wallet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arwil.mk.R
import com.arwil.mk.ui.home.toRupiahFormat

class AccountAdapter(
    private var accounts: List<Account>,
    private val onItemClick: (Account) -> Unit // <-- Tambahkan ini
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    private var transactionTotals: Map<Long, Double> = emptyMap()

    class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_account_name)
        val balance: TextView = view.findViewById(R.id.tv_account_balance)
        val icon: ImageView = view.findViewById(R.id.iv_account_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = accounts[position]
        val netChange = transactionTotals[account.id] ?: 0.0
        // 2. Hitung saldo saat ini
        val currentBalance = account.initialBalance + netChange

        holder.name.text = account.name
        // 3. Tampilkan saldo yang sudah dinamis
        holder.balance.text = currentBalance.toRupiahFormat()
        holder.icon.setImageResource(account.iconResId)

        holder.itemView.setOnClickListener {
            onItemClick(account)
        }

        // Nanti kita akan hitung saldo dinamis di sini
    }

    override fun getItemCount() = accounts.size

    fun updateData(newAccounts: List<Account>, totals: Map<Long, Double>) {
        accounts = newAccounts
        transactionTotals = totals // Simpan data total
        notifyDataSetChanged()
    }
}