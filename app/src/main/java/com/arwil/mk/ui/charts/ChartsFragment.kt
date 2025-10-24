package com.arwil.mk.ui.charts

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arwil.mk.R
import com.arwil.mk.ui.home.AppDatabase
import com.arwil.mk.ui.home.Transaction
import com.arwil.mk.ui.home.toRupiahFormat
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.button.MaterialButtonToggleGroup
import java.util.Calendar

class ChartsFragment : Fragment() {

    // === DEKLARASI VIEW & PROPERTI ===
    private lateinit var pieChart: PieChart
    private lateinit var rvCategorySummary: RecyclerView
    private lateinit var categoryAdapter: CategorySummaryAdapter
    private lateinit var db: AppDatabase
    private lateinit var tvChartTitle: TextView
    private lateinit var toggleButton: MaterialButtonToggleGroup

    private var allTransactions: List<Transaction> = emptyList()
    private var currentChartType = "EXPENSE" // Default: Expense

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_charts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi Views
        pieChart = view.findViewById(R.id.pieChart)
        rvCategorySummary = view.findViewById(R.id.rvCategorySummary)
        tvChartTitle = view.findViewById(R.id.tvChartTitle)
        toggleButton = view.findViewById(R.id.toggleButton)
        db = AppDatabase.getDatabase(requireContext())

        setupRecyclerView()
        setupToggleButtons()
        observeTransactions()
    }

    private fun setupToggleButtons() {
        toggleButton.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                currentChartType = if (checkedId == R.id.btnIncome) "INCOME" else "EXPENSE"
                updateUI() // Panggil update setiap kali tombol diganti
            }
        }
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategorySummaryAdapter(emptyList())
        rvCategorySummary.layoutManager = LinearLayoutManager(requireContext())
        rvCategorySummary.adapter = categoryAdapter
    }

    private fun observeTransactions() {
        db.transactionDao().getAllTransactions().observe(viewLifecycleOwner, Observer { transactions ->
            allTransactions = transactions
            updateUI() // Update UI dengan data awal (default: Expense)
        })
    }

    private fun updateUI() {
        // Ganti judul berdasarkan tipe yang dipilih
        tvChartTitle.text = if (currentChartType == "INCOME") "Statistik Pemasukan" else "Statistik Pengeluaran"

        // Filter data sesuai tipe dan bulan ini
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        val filteredTransactions = allTransactions.filter {
            val transactionCal = Calendar.getInstance().apply { timeInMillis = it.date }
            it.type == currentChartType &&
                    transactionCal.get(Calendar.MONTH) == currentMonth &&
                    transactionCal.get(Calendar.YEAR) == currentYear
        }

        // Jika tidak ada data, kosongkan chart dan list
        if (filteredTransactions.isEmpty()) {
            pieChart.clear()
            pieChart.invalidate()
            categoryAdapter.updateData(emptyList())
            return
        }

        // Proses data untuk ditampilkan
        processDataForChart(filteredTransactions)
    }

    private fun processDataForChart(filteredTransactions: List<Transaction>) {
        val summary = filteredTransactions
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val totalAmount = filteredTransactions.sumOf { it.amount }

        if (totalAmount > 0) {
            val colors = if (currentChartType == "INCOME") CHART_INCOME_COLORS else CHART_EXPENSE_COLORS
            var colorIndex = 0
            val summaryList = summary.map { (categoryName, total) ->
                val color = colors[colorIndex % colors.size]
                colorIndex++
                CategorySummary(
                    categoryName = categoryName,
                    totalAmount = total,
                    percentage = ((total / totalAmount) * 100).toFloat(),
                    iconResId = getIconForCategory(categoryName),
                    color = color
                )
            }.sortedByDescending { it.totalAmount }

            categoryAdapter.updateData(summaryList)
            setupPieChart(summaryList, totalAmount)
        }
    }

    private fun setupPieChart(summaryList: List<CategorySummary>, totalAmount: Double) {
        val entries = summaryList.map { PieEntry(it.percentage, it.categoryName) }
        val dataSet = PieDataSet(entries, currentChartType)

        dataSet.colors = summaryList.map { it.color }
        dataSet.setDrawValues(false)
        dataSet.sliceSpace = 2f

        pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 65f
            setHoleColor(Color.TRANSPARENT)
            setDrawEntryLabels(false)
            centerText = totalAmount.toRupiahFormat()
            setCenterTextSize(18f)
            setCenterTextColor(Color.BLACK)
            animateY(700)
            invalidate()
        }
    }

    private fun getIconForCategory(category: String): Int {
        return when (category) {
            // Expense
            "Makanan" -> R.drawable.ic_food
            "Transport" -> R.drawable.ic_transport
            "Tagihan" -> R.drawable.ic_bill
            "Belanja" -> R.drawable.ic_shopping
            "Hiburan" -> R.drawable.ic_entertainment
            "Kesehatan" -> R.drawable.ic_health
            // Income
            "Gaji" -> R.drawable.ic_salary
            "Bonus" -> R.drawable.ic_bonus
            "Hadiah" -> R.drawable.ic_gift
            else -> R.drawable.ic_other
        }
    }

    companion object {
        // Warna berbeda untuk Pemasukan dan Pengeluaran
        val CHART_EXPENSE_COLORS = listOf(
            Color.rgb(255, 187, 68), Color.rgb(255, 138, 128),
            Color.rgb(128, 203, 255), Color.rgb(179, 157, 219)
        )
        val CHART_INCOME_COLORS = listOf(
            Color.rgb(125, 224, 187), Color.rgb(100, 181, 246),
            Color.rgb(77, 208, 225), Color.rgb(204, 255, 144)
        )
    }
}