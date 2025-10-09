package com.arwil.mk.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arwil.mk.R
import java.text.NumberFormat
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var rvTransactions: RecyclerView

    private lateinit var tvTotalBalance: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView

    private val transactionsList = mutableListOf<Transaction>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTotalBalance = view.findViewById(R.id.tv_total_balance)
        tvIncome = view.findViewById(R.id.tv_income)
        tvExpense = view.findViewById(R.id.tv_expense)

        setupInitialData()

        rvTransactions = view.findViewById(R.id.rv_transactions)

        transactionAdapter = TransactionAdapter(transactionsList) { transactionToDelete ->
            showDeleteConfirmationDialog(transactionToDelete)
        }

        rvTransactions.layoutManager = LinearLayoutManager(context)
        rvTransactions.adapter = transactionAdapter

        calculateAndDisplayTotal(transactionsList)
    }

    private fun showDeleteConfirmationDialog(transactionToDelete: Transaction) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Konfirmasi Hapus")
        builder.setMessage("Apakah Anda yakin ingin menghapus transaksi '${transactionToDelete.title}'?")

        builder.setPositiveButton("Ya") { dialog, which ->
            val position = transactionsList.indexOf(transactionToDelete)
            if (position != -1) {
                transactionsList.removeAt(position)
                transactionAdapter.notifyItemRemoved(position)
                calculateAndDisplayTotal(transactionsList)
            }
        }

        builder.setNegativeButton("Tidak") { dialog, which ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun setupInitialData() {
        // Fungsi baru agar lebih rapi
        transactionsList.clear() // Bersihkan list sebelum menambahkan data baru
        transactionsList.addAll(listOf(
            Transaction("Gaji Bulan Ini", "Pemasukan", 5000000.0, "INCOME"),
            Transaction("Makan Siang", "Makanan", 50000.0, "EXPENSE"),
            Transaction("Beli Bensin", "Transportasi", 100000.0, "EXPENSE"),
            Transaction("Bonus Proyek", "Pemasukan", 1500000.0, "INCOME"),
            Transaction("Bayar Listrik", "Tagihan", 350000.0, "EXPENSE")
        ))
    }

    private fun calculateAndDisplayTotal(transactions: List<Transaction>) {
        var totalIncome = 0.0
        var totalExpense = 0.0

        for (transaction in transactions) {
            if (transaction.type == "INCOME") {
                totalIncome += transaction.amount
            } else {
                totalExpense += transaction.amount
            }
        }

        val totalBalance = totalIncome - totalExpense

        tvTotalBalance.text = formatToRupiah(totalBalance)
        tvIncome.text = formatToRupiah(totalIncome)
        tvExpense.text = formatToRupiah(totalExpense)
    }

    fun showAddTransactionSheet() {
        val addTransactionFragment = AddTransactionFragment()

        addTransactionFragment.onTransactionAddedListener = { newTransaction ->
            transactionsList.add(0, newTransaction)
            transactionAdapter.notifyItemInserted(0)
            rvTransactions.scrollToPosition(0)
            calculateAndDisplayTotal(transactionsList)
        }

        addTransactionFragment.show(parentFragmentManager, "AddTransactionFragment")
    }

    private fun formatToRupiah(amount: Double): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        numberFormat.maximumFractionDigits = 0
        return numberFormat.format(amount)
    }
}