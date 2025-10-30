package com.arwil.mk.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.arwil.mk.R
import com.arwil.mk.ui.home.AppDatabase
import com.arwil.mk.ui.home.Transaction
import com.arwil.mk.ui.home.toRupiahFormat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.animation.Easing
import com.google.android.material.button.MaterialButtonToggleGroup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportsFragment : Fragment() {

    // === DEKLARASI VIEW & PROPERTI ===
    private lateinit var lineChart: LineChart
    private lateinit var db: AppDatabase
    private lateinit var tvReportTotalIncome: TextView
    private lateinit var tvReportTotalExpense: TextView
    private lateinit var tvReportNetProfit: TextView
    private lateinit var tvReportTitle: TextView
    private lateinit var toggleButton: MaterialButtonToggleGroup
    private lateinit var myMarkerView: MyMarkerView

    private var allTransactions: List<Transaction> = emptyList()
    private var currentReportType = "EXPENSE" // Default: Expense

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi Views
        lineChart = view.findViewById(R.id.lineChart)
        tvReportTotalIncome = view.findViewById(R.id.tv_report_total_income)
        tvReportTotalExpense = view.findViewById(R.id.tv_report_total_expense)
        tvReportNetProfit = view.findViewById(R.id.tv_report_net_profit)
        tvReportTitle = view.findViewById(R.id.tvReportTitle)
        toggleButton = view.findViewById(R.id.toggleButton)

        db = AppDatabase.getDatabase(requireContext())

        myMarkerView = MyMarkerView(
            context = requireContext(),
            layoutResource = R.layout.custom_marker_view
        ) { value ->
            value.toDouble().toRupiahFormat()
        }

        myMarkerView.chartView = lineChart
        lineChart.marker = myMarkerView

        setupToggleButtons()
        observeTransactions()
    }

    private fun setupToggleButtons() {
        toggleButton.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                currentReportType = if (checkedId == R.id.btnIncome) "INCOME" else "EXPENSE"
                val color = if (currentReportType == "INCOME") {
                    ContextCompat.getColor(requireContext(), R.color.green)
                } else {
                    ContextCompat.getColor(requireContext(), R.color.red)
                }
                myMarkerView.setColor(color)
                updateUI() // Panggil update setiap kali tombol diganti
            }
        }
    }

    private fun observeTransactions() {
        db.transactionDao().getAllTransactions().observe(viewLifecycleOwner, Observer { transactions ->
            allTransactions = transactions
            val initialColor = if (currentReportType == "INCOME") {
                ContextCompat.getColor(requireContext(), R.color.green)
            } else {
                ContextCompat.getColor(requireContext(), R.color.red)
            }
            myMarkerView.setColor(initialColor)
            updateUI() // Update UI dengan data awal (default: Expense)
        })
    }

    private fun updateUI() {
        // 1. Update judul
        tvReportTitle.text = if (currentReportType == "INCOME") {
            "Laporan Tren Pemasukan"
        } else {
            "Laporan Tren Pengeluaran"
        }

        // 2. Proses data
        processDataForLineChart(allTransactions)
    }

    // Fungsi helper untuk format sumbu Y
    private fun formatAxisValue(value: Float): String {
        return when {
            value >= 1_000_000_000 -> "${(value / 1_000_000_000).toInt()} M"
            value >= 1_000_000 -> "${(value / 1_000_000).toInt()} Jt"
            value >= 1_000 -> "${(value / 1_000).toInt()} Rb"
            else -> value.toInt().toString()
        }
    }

    private fun processDataForLineChart(transactions: List<Transaction>) {
        val calendar = Calendar.getInstance()
        val numMonths = 6
        val monthLabels = mutableListOf<String>()

        val incomeEntries = mutableListOf<Entry>()
        val expenseEntries = mutableListOf<Entry>()

        var totalIncome = 0.0
        var totalExpense = 0.0

        for (i in (numMonths - 1) downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.MONTH, -i)

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)

            val monthFormat = SimpleDateFormat("MMM", Locale("id", "ID"))
            monthLabels.add(monthFormat.format(calendar.time))

            val monthlyTransactions = transactions.filter {
                val transactionCal = Calendar.getInstance().apply { timeInMillis = it.date }
                transactionCal.get(Calendar.YEAR) == year && transactionCal.get(Calendar.MONTH) == month
            }

            val monthlyIncome = monthlyTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val monthlyExpense = monthlyTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

            val index = (numMonths - 1 - i).toFloat()

            incomeEntries.add(Entry(index, monthlyIncome.toFloat()))
            expenseEntries.add(Entry(index, monthlyExpense.toFloat()))

            totalIncome += monthlyIncome
            totalExpense += monthlyExpense
        }

        // Update kartu ringkasan (ini selalu menampilkan total 6 bulan)
        tvReportTotalIncome.text = totalIncome.toRupiahFormat()
        tvReportTotalExpense.text = totalExpense.toRupiahFormat()
        tvReportNetProfit.text = (totalIncome - totalExpense).toRupiahFormat()

        // Setup Line Chart
        setupLineChart(monthLabels, incomeEntries, expenseEntries)
    }

    private fun setupLineChart(monthLabels: List<String>, incomeEntries: List<Entry>, expenseEntries: List<Entry>) {

        val colorGreen = ContextCompat.getColor(requireContext(), R.color.green)
        val colorRed = ContextCompat.getColor(requireContext(), R.color.red)

        // 1. Dataset Pemasukan (Garis Hijau)
        val incomeDataSet = LineDataSet(incomeEntries, "Pemasukan")
        incomeDataSet.color = colorGreen
        incomeDataSet.setCircleColor(colorGreen)
        incomeDataSet.lineWidth = 2.5f
        incomeDataSet.circleRadius = 5f
        incomeDataSet.setDrawCircleHole(false)
        incomeDataSet.setDrawValues(false)
        incomeDataSet.setDrawFilled(true)
        incomeDataSet.fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_fill_green)
        incomeDataSet.mode = LineDataSet.Mode.LINEAR

        // 2. Dataset Pengeluaran (Garis Merah)
        val expenseDataSet = LineDataSet(expenseEntries, "Pengeluaran")
        expenseDataSet.color = colorRed
        expenseDataSet.setCircleColor(colorRed)
        expenseDataSet.lineWidth = 2.5f
        expenseDataSet.circleRadius = 5f
        expenseDataSet.setDrawCircleHole(false)
        expenseDataSet.setDrawValues(false)
        expenseDataSet.setDrawFilled(true)
        expenseDataSet.fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_fill_red)
        expenseDataSet.mode = LineDataSet.Mode.LINEAR

        // === PERUBAHAN UTAMA DI SINI ===
        // Tentukan dataset mana yang akan ditampilkan
        val dataSets = mutableListOf<ILineDataSet>()
        if (currentReportType == "INCOME") {
            dataSets.add(incomeDataSet)
            lineChart.legend.isEnabled = false // Sembunyikan legenda jika hanya 1
        } else {
            dataSets.add(expenseDataSet)
            lineChart.legend.isEnabled = false // Sembunyikan legenda jika hanya 1
        }

        val lineData = LineData(dataSets)

        lineChart.data = lineData
        // === AKHIR PERUBAHAN ===

        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(false)
        lineChart.setScaleEnabled(false)

        lineChart.isHighlightPerTapEnabled = true
        lineChart.isHighlightPerDragEnabled = false

        // Konfigurasi Sumbu X
        val xAxis = lineChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(monthLabels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text1)

        // Konfigurasi Sumbu Y
        val leftAxis = lineChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text1)
        leftAxis.axisMinimum = 0f
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return formatAxisValue(value)
            }
        }
        lineChart.axisRight.isEnabled = false

        // Segarkan chart
        lineChart.animateY(1200, Easing.EaseInOutCubic)
    }
}