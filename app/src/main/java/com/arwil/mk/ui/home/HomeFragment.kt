package com.arwil.mk.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arwil.mk.R
import com.arwil.mk.ui.addTransaction.AddTransactionFragment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvTotalBalance: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView

    // Database instance
    private lateinit var db: AppDatabase
    private lateinit var transactionDao: TransactionDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi Database
        db = AppDatabase.getDatabase(requireContext())
        transactionDao = db.transactionDao()

        tvTotalBalance = view.findViewById(R.id.tv_total_balance)
        tvIncome = view.findViewById(R.id.tv_income)
        tvExpense = view.findViewById(R.id.tv_expense)
        rvTransactions = view.findViewById(R.id.rv_transactions)

        // Setup RecyclerView
        setupRecyclerView()

        // Observe data dari database
        observeTransactions()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(emptyList()) { transactionToDelete ->
            showDeleteConfirmationDialog(transactionToDelete)
        }
        rvTransactions.layoutManager = LinearLayoutManager(context)
        rvTransactions.adapter = transactionAdapter
    }

    private fun observeTransactions() {
        // LiveData akan otomatis mengupdate UI jika ada perubahan data
        transactionDao.getAllTransactions().observe(viewLifecycleOwner, Observer { transactions ->
            updateGroupedList(transactions)
            calculateAndDisplayTotal(transactions)
        })
    }

    private fun showDeleteConfirmationDialog(transactionToDelete: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Hapus")
            .setMessage("Apakah Anda yakin ingin menghapus transaksi '${transactionToDelete.title}'?")
            .setPositiveButton("Ya") { _, _ ->
                // Hapus data dari database di background thread
                lifecycleScope.launch {
                    transactionDao.deleteTransaction(transactionToDelete)
                }
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    // Fungsi ini sekarang hanya untuk mengisi data awal jika database kosong
    private fun insertInitialData() {
        lifecycleScope.launch {
            val today = Calendar.getInstance().timeInMillis
            val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }.timeInMillis
            val twoDaysAgo = Calendar.getInstance().apply { add(Calendar.DATE, -2) }.timeInMillis

            transactionDao.insertTransaction(Transaction(title = "Gaji Bulan Ini", category = "Pemasukan", amount = 5000000.0, type = "INCOME", date = today))
            transactionDao.insertTransaction(Transaction(title = "Makan Siang", category = "Makanan", amount = 50000.0, type = "EXPENSE", date = today))
            transactionDao.insertTransaction(Transaction(title = "Beli Bensin", category = "Transportasi", amount = 100000.0, type = "EXPENSE", date = yesterday))
            transactionDao.insertTransaction(Transaction(title = "Bonus Proyek", category = "Pemasukan", amount = 1500000.0, type = "INCOME", date = yesterday))
            transactionDao.insertTransaction(Transaction(title = "Bayar Listrik", category = "Tagihan", amount = 350000.0, type = "EXPENSE", date = twoDaysAgo))
        }
    }


    private fun calculateAndDisplayTotal(transactions: List<Transaction>) {
        val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val totalBalance = totalIncome - totalExpense

        tvTotalBalance.text = totalBalance.toRupiahFormat()
        tvIncome.text = totalIncome.toRupiahFormat()
        tvExpense.text = totalExpense.toRupiahFormat()
    }

    private fun updateGroupedList(transactions: List<Transaction>) {
        // Tidak perlu sorting lagi karena query DAO sudah mengurutkan
        val groupedByDate = transactions.groupBy {
            // Logika pengelompokan tanggal tetap sama
            val calendar = Calendar.getInstance().apply {
                timeInMillis = it.date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            calendar.time
        }

        val combinedList = mutableListOf<ListItem>()
        for ((date, transactionItems) in groupedByDate) {
            combinedList.add(ListItem.DateHeader(formatDateHeader(date)))
            transactionItems.forEach { transaction ->
                combinedList.add(ListItem.TransactionItem(transaction))
            }
        }
        transactionAdapter.updateData(combinedList)
    }


    private fun formatDateHeader(date: Date): String {
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }

        calendar.time = date

        return when {
            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Hari Ini"

            calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                    calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Kemarin"

            else -> SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(date)
        }
    }
}