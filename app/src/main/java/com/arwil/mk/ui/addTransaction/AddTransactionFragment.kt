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
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.arwil.mk.ui.wallet.Account
import com.arwil.mk.ui.home.AppDatabase

class AddTransactionFragment : BottomSheetDialogFragment() {

    // Listener untuk mengirim data kembali ke HomeFragment
    var onTransactionAddedListener: ((Transaction) -> Unit)? = null
    private var selectedType = "EXPENSE" // Default
    private var selectedCategoryName: String = ""
    private val selectedDate = Calendar.getInstance()
    private lateinit var db: AppDatabase
    private var accountList: List<Account> = emptyList()
    private var selectedAccountId: Long = -1L
    private lateinit var etCategory: EditText
    private lateinit var etAccount: AutoCompleteTextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())

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

        etCategory = view.findViewById(R.id.et_category)
        etAccount = view.findViewById(R.id.et_account)

        // Fungsi untuk update UI Tab
        fun updateTabs(type: String) {
            if (selectedType == type) return
            selectedType = type

            etCategory.setText("")
            selectedCategoryName = ""

            if (type == "INCOME") {
                viewExpenseBg.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bgColor))
                viewIncomeBg.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue_light))

                tabIncome.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                tabExpense.setTextColor(ContextCompat.getColor(requireContext(), R.color.text1))
            } else { // EXPENSE
                viewExpenseBg.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
                viewIncomeBg.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bgColor))

                tabExpense.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                tabIncome.setTextColor(ContextCompat.getColor(requireContext(), R.color.text1))
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

        setupAccountDropdown()

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

            // Tambahkan validasi untuk selectedAccountId
            if (title.isNotEmpty() && amount > 0 && selectedCategoryName.isNotEmpty() && selectedAccountId != -1L) {
                val newTransaction =
                    Transaction(
                        accountId = selectedAccountId, // <-- WAJIB ADA
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

    private fun setupAccountDropdown() {
        // Gunakan query baru untuk hanya mendapatkan akun aset
        db.accountDao().getAssetAccounts().observe(viewLifecycleOwner, { assetAccounts ->
            // Simpan daftar akun aset ini untuk digunakan nanti
            accountList = assetAccounts
            val accountNames = assetAccounts.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, accountNames)
            etAccount.setAdapter(adapter)
            // ======================

            // Cari akun "Cash" di antara akun aset
            val cashAccount = accountList.find { it.name.equals("Cash", ignoreCase = true) }
            if (cashAccount != null) {
                // Set default jika ditemukan
                etAccount.setText(cashAccount.name, false)
                selectedAccountId = cashAccount.id
            } else if (assetAccounts.isNotEmpty()) {
                // Jika Cash tidak ada TAPI ada akun aset lain, pilih yang pertama
                etAccount.setText(assetAccounts[0].name, false)
                selectedAccountId = assetAccounts[0].id
            } else {
                // Jika tidak ada akun aset sama sekali
                etAccount.setText("")
                selectedAccountId = -1L
                // Mungkin tambahkan pesan error atau nonaktifkan tombol simpan
            }
        })

        etAccount.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position) as String
            // Dapatkan ID dari akun yang dipilih (pasti akun aset)
            selectedAccountId = accountList.find { it.name == selectedName }?.id ?: -1L
        }
    }
}