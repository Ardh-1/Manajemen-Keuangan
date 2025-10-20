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
import android.text.Editable
import com.arwil.mk.ui.home.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.text.TextWatcher

class AddTransactionFragment : BottomSheetDialogFragment() {

    // Listener untuk mengirim data kembali ke HomeFragment
    var onTransactionAddedListener: ((Transaction) -> Unit)? = null
    private var selectedType = "EXPENSE" // Default
    private val selectedDate = Calendar.getInstance()
    private var selectedCategoryName: String = ""
    private lateinit var etCategory: EditText

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

        etCategory = view.findViewById<EditText>(R.id.et_category)

        // Fungsi untuk update UI Tab
        fun updateTabs(type: String) {
            if (selectedType == type) return

            selectedType = type

            etCategory.setText("")
            selectedCategoryName = ""

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

        etAmount.addTextChangedListener(object : TextWatcher {
            private var current = ""
            private val formatter = NumberFormat.getNumberInstance(Locale("in", "ID"))

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (s.toString() != current) {
                    // 1. Hapus listener untuk sementara agar tidak terjadi loop tak terbatas
                    etAmount.removeTextChangedListener(this)

                    // 2. Hapus semua titik/koma untuk mendapatkan angka bersih
                    val cleanString = s.toString().replace("[.,]".toRegex(), "")

                    if (cleanString.isNotEmpty()) {
                        try {
                            // 3. Ubah string bersih menjadi angka, lalu format kembali dengan titik
                            val parsed = cleanString.toDouble()
                            val formatted = formatter.format(parsed)
                            current = formatted
                            etAmount.setText(formatted)
                            etAmount.setSelection(formatted.length)
                        } catch (e: NumberFormatException) {
                            // Jika terjadi error, biarkan saja
                        }
                    } else {
                        current = ""
                    }

                    // 4. Tambahkan kembali listener
                    etAmount.addTextChangedListener(this)
                }
            }
        })

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

        etCategory.setOnClickListener {
            val categoryPicker = CategoryPickerFragment.newInstance(selectedType)
            categoryPicker.onCategorySelectedListener = { category ->
                etCategory.setText(category.name)
                selectedCategoryName = category.name
            }
            categoryPicker.show(parentFragmentManager, "CategoryPicker")
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            val amountString = etAmount.text.toString().replace(".", "")
            val amount = amountString.toDoubleOrNull() ?: 0.0


            if (title.isNotEmpty() && amount > 0 && selectedCategoryName.isNotEmpty()) {
                val newTransaction = Transaction(
                    title = title,
                    category = selectedCategoryName,
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