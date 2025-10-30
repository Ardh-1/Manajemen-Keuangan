package com.arwil.mk.ui.wallet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arwil.mk.R
import com.arwil.mk.ui.home.toRupiahFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AccountAdapter(
    private var accounts: List<Account>,
    private val onItemClick: (Account) -> Unit // <-- Tambahkan ini
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    private var transactionTotals: Map<Long, Double> = emptyMap()

    class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_account_name)
        val balance: TextView = view.findViewById(R.id.tv_account_balance)
        val icon: ImageView = view.findViewById(R.id.iv_account_icon)
        val balanceLabel: TextView = view.findViewById(R.id.tv_balance_label)
        val dueDate: TextView = view.findViewById(R.id.tv_account_due_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = accounts[position]
        val netChange = transactionTotals[account.id] ?: 0.0
        val currentBalance = account.initialBalance + netChange
        val context = holder.itemView.context

        holder.name.text = account.name
        holder.balance.text = currentBalance.toRupiahFormat()
        holder.icon.setImageResource(account.iconResId)

        // === PERUBAHAN DI SINI ===
        // Hanya pasang listener jika BUKAN akun "Cash"
        if (!account.name.equals("Cash", ignoreCase = true)) {
            holder.itemView.setOnClickListener {
                onItemClick(account)
            }
        } else {
            // Jika ini akun Cash, pastikan tidak ada listener lama yang menempel
            holder.itemView.setOnClickListener(null)
            holder.itemView.isClickable = false // Nonaktifkan efek klik visual
        }

        if (account.isDebtAccount) {
            // Ini adalah Akun HUTANG

            // 1. Sembunyikan label "Total Hutang" (Sesuai permintaan)
            holder.balanceLabel.visibility = View.GONE

            // 2. Tampilkan nominal hutang (saldo saat ini)
            // (Nama akun akan otomatis terlihat di tengah secara vertikal)
            holder.balance.text = currentBalance.toRupiahFormat()
            holder.balance.setTextColor(ContextCompat.getColor(context, R.color.red))

            // 3. Tampilkan tanggal jatuh tempo di bawahnya
            if (account.dueDate != null) {
                val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                holder.dueDate.text = "Jatuh tempo ${sdf.format(Date(account.dueDate))}"
                holder.dueDate.visibility = View.VISIBLE

                // --- LOGIKA WARNA KONDISIONAL ---
                // Buat kalender untuk hari ini (direset ke 00:00:00)
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                // Buat kalender untuk tanggal jatuh tempo
                val dueDateCalendar = Calendar.getInstance().apply { timeInMillis = account.dueDate }

                // Cek apakah tanggal jatuh tempo sudah lewat (sebelum hari ini)
                if (dueDateCalendar.before(today)) {
                    // Terlewat: Set warna teks tanggal menjadi merah
                    holder.dueDate.setTextColor(ContextCompat.getColor(context, R.color.red))
                    holder.balance.setTextColor(ContextCompat.getColor(context, R.color.red))
                } else {
                    // Belum terlewat: Set warna teks tanggal menjadi abu-abu (sekunder)
                    holder.dueDate.setTextColor(ContextCompat.getColor(context, R.color.text1))
                    holder.balance.setTextColor(ContextCompat.getColor(context, R.color.text1))
                }
                // ---------------------------------

            } else {
                holder.dueDate.text = "Jatuh tempo tidak diatur"
                holder.dueDate.visibility = View.VISIBLE
                // Set warna default jika tidak diatur
                holder.dueDate.setTextColor(ContextCompat.getColor(context, R.color.text2))
            }

        } else {
            // Ini adalah Akun ASET (Kembalikan ke kondisi normal)

            // 1. Tampilkan label
            holder.balanceLabel.visibility = View.VISIBLE
            holder.balanceLabel.text = "Saldo Saat Ini"

            // 2. Tampilkan saldo
            holder.balance.text = currentBalance.toRupiahFormat()
            holder.balance.setTextColor(ContextCompat.getColor(context, R.color.text1))

            // 3. Sembunyikan field tanggal jatuh tempo
            holder.dueDate.visibility = View.GONE
        }
    }

    override fun getItemCount() = accounts.size

    fun updateData(newAccounts: List<Account>, totals: Map<Long, Double>) {
        accounts = newAccounts
        transactionTotals = totals // Simpan data total
        notifyDataSetChanged()
    }
}