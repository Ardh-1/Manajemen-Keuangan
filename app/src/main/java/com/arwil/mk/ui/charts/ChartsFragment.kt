package com.arwil.mk.ui.charts

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.github.mikephil.charting.formatter.PercentFormatter
import java.util.Calendar

class ChartsFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var rvCategorySummary: RecyclerView
    private lateinit var categoryAdapter: CategorySummaryAdapter
    private lateinit var db: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_charts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pieChart = view.findViewById(R.id.pieChart)
        rvCategorySummary = view.findViewById(R.id.rvCategorySummary)
        db = AppDatabase.getDatabase(requireContext())

        setupRecyclerView()
        observeTransactions()
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategorySummaryAdapter(emptyList())
        rvCategorySummary.layoutManager = LinearLayoutManager(requireContext())
        rvCategorySummary.adapter = categoryAdapter
    }

    private fun observeTransactions() {
        // Kita ambil semua data, lalu filter di sini
        db.transactionDao().getAllTransactions().observe(viewLifecycleOwner, Observer { transactions ->
            processDataForChart(transactions)
        })
    }

    private fun processDataForChart(transactions: List<Transaction>) {
        // 1. Filter hanya untuk EXPENSE dan untuk bulan ini
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        val monthlyExpenses = transactions.filter {
            val transactionCal = Calendar.getInstance().apply { timeInMillis = it.date }
            it.type == "EXPENSE" &&
                    transactionCal.get(Calendar.MONTH) == currentMonth &&
                    transactionCal.get(Calendar.YEAR) == currentYear
        }

        // 2. Kelompokkan berdasarkan kategori dan jumlahkan totalnya
        val summary = monthlyExpenses
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val totalExpense = monthlyExpenses.sumOf { it.amount }

        if (totalExpense > 0) {
            var colorIndex = 0 // <-- Tambahkan ini
            val summaryList = summary.map { (categoryName, totalAmount) ->
                // Ambil warna dari daftar, dan putar kembali ke awal jika sudah habis
                val color = CHART_COLORS[colorIndex % CHART_COLORS.size]
                colorIndex++ // <-- Tambahkan ini

                CategorySummary(
                    categoryName = categoryName,
                    totalAmount = totalAmount,
                    percentage = ((totalAmount / totalExpense) * 100).toFloat(),
                    iconResId = getIconForCategory(categoryName),
                    color = color // <-- Berikan warna di sini
                )
            }.sortedByDescending { it.totalAmount } // Urutkan dari terbesar

            // Update RecyclerView
            categoryAdapter.updateData(summaryList)

            // Update Pie Chart
            setupPieChart(summaryList, totalExpense)
        }
    }

    private fun setupPieChart(summaryList: List<CategorySummary>, totalExpense: Double) {
        val entries = ArrayList<PieEntry>()
        for (item in summaryList) {
            entries.add(PieEntry(item.percentage, item.categoryName))
        }

        val dataSet = PieDataSet(entries, "Expense Categories")
        dataSet.colors = summaryList.map { it.color } // Menggunakan warna yang kita definisikan

        dataSet.setDrawValues(false) // <-- PENTING: Sembunyikan nilai persentase di chart
        dataSet.sliceSpace = 2f

        val data = PieData(dataSet)
        // data.setValueFormatter(PercentFormatter(pieChart))

        pieChart.apply {
            this.data = data
            // setUsePercentValues(true) // <-- Tidak lagi diperlukan
            description.isEnabled = false
            legend.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 65f // <-- Perbesar lubang agar lebih mirip contoh
            setHoleColor(Color.TRANSPARENT)

            setDrawEntryLabels(false) // <-- PENTING: Sembunyikan nama kategori di chart

            // Sesuaikan teks di tengah
            centerText = totalExpense.toRupiahFormat()
            setCenterTextSize(22f)
            setCenterTextColor(Color.BLACK)

            animateY(750)
            invalidate()
        }
    }

    // Fungsi helper dari TransactionAdapter, kita copy ke sini
    private fun getIconForCategory(category: String): Int {
        return when (category) {
            "Makanan" -> R.drawable.ic_food
            "Transport" -> R.drawable.ic_transport
            "Tagihan" -> R.drawable.ic_bill
            "Belanja" -> R.drawable.ic_shopping
            "Hiburan" -> R.drawable.ic_entertainment
            "Kesehatan" -> R.drawable.ic_health
            "Gaji" -> R.drawable.ic_salary
            "Bonus" -> R.drawable.ic_bonus
            "Hadiah" -> R.drawable.ic_gift
            else -> R.drawable.ic_other
        }
    }

    // Daftar warna untuk chart
    companion object {
        val CHART_COLORS = listOf(
            Color.rgb(255, 187, 68), // Kuning
            Color.rgb(125, 224, 187), // Hijau muda
            Color.rgb(255, 138, 128), // Merah muda
            Color.rgb(128, 203, 255), // Biru muda
            Color.rgb(179, 157, 219)  // Ungu muda
        )
    }
}