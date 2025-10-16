package com.arwil.mk.ui.addTransaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.arwil.mk.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.app.DatePickerDialog
import com.arwil.mk.ui.home.Transaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTransactionFragment : BottomSheetDialogFragment() {

    // Listener untuk mengirim data kembali ke HomeFragment
    var onTransactionAddedListener: ((Transaction) -> Unit)? = null
    private var selectedType = "EXPENSE" // Default

    private val selectedDate = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.setOnShowListener {
            val bottomSheetDialog = it as BottomSheetDialog
            val bottomSheet =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        val tabIncome = view.findViewById<TextView>(R.id.tab_income)
        val tabExpense = view.findViewById<TextView>(R.id.tab_expense)
        val viewIncomeBg = view.findViewById<View>(R.id.view_income_bg)
        val viewExpenseBg = view.findViewById<View>(R.id.view_expense_bg)

        // Fungsi untuk update UI Tab
        fun updateTabs(type: String) {
            selectedType = type
            if (type == "INCOME") {
                viewIncomeBg.visibility = View.VISIBLE
                viewExpenseBg.visibility = View.INVISIBLE

                tabIncome.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                tabExpense.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            } else { // EXPENSE
                viewExpenseBg.visibility = View.VISIBLE
                viewIncomeBg.visibility = View.INVISIBLE

                tabExpense.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                tabIncome.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            }
        }

        // Atur kondisi awal
        updateTabs("EXPENSE")

        tabIncome.setOnClickListener { updateTabs("INCOME") }
        tabExpense.setOnClickListener { updateTabs("EXPENSE") }

        val etTitle = view.findViewById<EditText>(R.id.et_title)
        val etAmount = view.findViewById<EditText>(R.id.et_amount)
        val etCategory = view.findViewById<EditText>(R.id.et_category)
        val btnSave = view.findViewById<Button>(R.id.btn_save)
        val etDate = view.findViewById<EditText>(R.id.et_date)

        fun updateDateInView() {
            val myFormat = "dd/MM/yyyy" // Format tanggal
            val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
            etDate.setText(sdf.format(selectedDate.time))
        }

        // Set tanggal hari ini saat pertama kali dibuka
        updateDateInView()

        // Buat DatePickerDialog
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, monthOfYear)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            }

        etDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                requireContext(), dateSetListener,
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val category = etCategory.text.toString()

            if (title.isNotEmpty() && amount > 0 && category.isNotEmpty()) {
                val newTransaction = Transaction(
                    title = title,
                    category = category,
                    amount = amount,
                    type = selectedType,
                    date = selectedDate.timeInMillis
                )
                onTransactionAddedListener?.invoke(newTransaction)
                dismiss()
            }
        }
    }
}