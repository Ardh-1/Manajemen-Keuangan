package com.arwil.mk.ui.home

import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arwil.mk.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class HomeFragment : Fragment() {

    // === DEKLARASI VIEW & PROPERTI ===
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvTotalBalance: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView
    private lateinit var tvCurrentMonth: TextView
    private lateinit var ivPrevMonth: ImageView
    private lateinit var ivNextMonth: ImageView

    private lateinit var db: AppDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var gestureDetector: GestureDetectorCompat

    private var allTransactions: List<Transaction> = emptyList()
    private var currentCalendar: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi Database dan View
        db = AppDatabase.getDatabase(requireContext())
        transactionDao = db.transactionDao()
        initializeViews(view)

        setupRecyclerView()
        setupMonthControls()
        setupGestureDetector(view)
        observeTransactions()
    }

    private fun initializeViews(view: View) {
        tvTotalBalance = view.findViewById(R.id.tv_total_balance)
        tvIncome = view.findViewById(R.id.tv_income)
        tvExpense = view.findViewById(R.id.tv_expense)
        rvTransactions = view.findViewById(R.id.rv_transactions)
        tvCurrentMonth = view.findViewById(R.id.tv_current_month)
        ivPrevMonth = view.findViewById(R.id.iv_prev_month)
        ivNextMonth = view.findViewById(R.id.iv_next_month)
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(emptyList()) { selectedTransaction ->
            showEditTransactionSheet(selectedTransaction)
        }
        rvTransactions.layoutManager = LinearLayoutManager(context)
        rvTransactions.adapter = transactionAdapter
    }

    private fun setupMonthControls() {
        ivPrevMonth.setOnClickListener { changeMonth(-1) }
        ivNextMonth.setOnClickListener { changeMonth(1) }
    }

    private fun setupGestureDetector(rootView: View) {
        gestureDetector = GestureDetectorCompat(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                if (abs(diffX) > abs(diffY) &&
                    abs(diffX) > SWIPE_THRESHOLD &&
                    abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        // Geser ke Kanan (Bulan Sebelumnya)
                        changeMonth(-1)
                    } else {
                        // Geser ke Kiri (Bulan Berikutnya)
                        changeMonth(1)
                    }
                    return true
                }
                return false
            }
        })
        // Terapkan listener ke root view agar seluruh area bisa di-swipe
        rootView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            // Return true agar event diteruskan ke child view seperti RecyclerView
            true
        }
    }

    private fun observeTransactions() {
        transactionDao.getAllTransactions().observe(viewLifecycleOwner, Observer { transactions ->
            allTransactions = transactions
            updateUIForCurrentMonth()
        })
    }

    private fun changeMonth(amount: Int) {
        currentCalendar.add(Calendar.MONTH, amount)
        updateUIForCurrentMonth()
    }

    private fun updateUIForCurrentMonth() {
        // 1. Update teks bulan dan tahun
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        tvCurrentMonth.text = monthFormat.format(currentCalendar.time)

        // 2. Filter transaksi untuk bulan yang sedang ditampilkan
        val monthlyTransactions = allTransactions.filter {
            val transactionCal = Calendar.getInstance().apply { timeInMillis = it.date }
            transactionCal.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                    transactionCal.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH)
        }

        // 3. Update semua komponen UI dengan data yang sudah difilter
        calculateAndDisplayTotal(monthlyTransactions)
        updateGroupedList(monthlyTransactions)
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
        // Urutkan dari yang terbaru
        val sortedTransactions = transactions.sortedByDescending { it.date }

        val groupedByDate = sortedTransactions.groupBy {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = it.date
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
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
        val calendar = Calendar.getInstance(); calendar.time = date
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }

        return when {
            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Hari Ini"
            calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Kemarin"
            else -> SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(date)
        }
    }

    private fun showEditTransactionSheet(transaction: Transaction) {
        val editFragment = EditTransactionFragment.newInstance(transaction)
        editFragment.onTransactionUpdated = { lifecycleScope.launch { transactionDao.updateTransaction(it) } }
        editFragment.onTransactionDeleted = { lifecycleScope.launch { transactionDao.deleteTransaction(it) } }
        editFragment.show(parentFragmentManager, "EditTransactionFragment")
    }
}