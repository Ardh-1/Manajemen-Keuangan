package com.arwil.mk.ui.wallet

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import com.arwil.mk.R
import com.arwil.mk.ui.home.AppDatabase
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputLayout
import androidx.core.view.isVisible
import java.util.Calendar
import android.widget.AutoCompleteTextView
import java.text.SimpleDateFormat

class AddAccountFragment : BottomSheetDialogFragment() {

    private lateinit var db: AppDatabase
    private var selectedAccountType: String? = null
    private var selectedIconResId: Int = R.drawable.ic_other
    private var isDebtAccount: Boolean = false
    private val dueDateCalendar: Calendar = Calendar.getInstance()
    private var selectedDueDateMillis: Long? = null
    private var selectedReminderDays: Int? = null
    private val reminderOptions = mapOf(
        "Tidak Usah" to null,
        "Hari-H" to 0,
        "H-1" to 1,
        "H-3" to 3,
        "H-7" to 7
    )
    private val reminderLabels = reminderOptions.keys.toList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext())

        // Tambahkan ini untuk membuat sheet terbuka penuh
        dialog?.setOnShowListener {
            val bottomSheetDialog = it as BottomSheetDialog
            val bottomSheet =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        val etName = view.findViewById<EditText>(R.id.et_account_name)
        val etBalance = view.findViewById<EditText>(R.id.et_initial_balance)
        val etAccountType = view.findViewById<EditText>(R.id.et_account_type)
        val btnSave = view.findViewById<Button>(R.id.btn_save_account)
        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.toggle_account_classification)
        val layoutBalance = view.findViewById<TextInputLayout>(R.id.layout_initial_balance)
        val layoutAccountType = view.findViewById<TextInputLayout>(R.id.layout_account_type) // <-- Ambil layoutnya
        val layoutDueDate = view.findViewById<TextInputLayout>(R.id.layout_due_date) // <-- Baru
        val etDueDate = view.findViewById<EditText>(R.id.et_due_date) // <-- Baru
        val layoutReminder = view.findViewById<TextInputLayout>(R.id.layout_reminder) // <-- Baru
        val spinnerReminder = view.findViewById<AutoCompleteTextView>(R.id.spinner_reminder) // <-- Baru

        val formatter = NumberFormat.getNumberInstance(Locale("in", "ID"))

        // Tambahkan TextWatcher untuk format angka ribuan
        etBalance.addTextChangedListener(object : TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (s.toString() != current) {
                    etBalance.removeTextChangedListener(this)
                    val cleanString = s.toString().replace("[.,]".toRegex(), "")
                    if (cleanString.isNotEmpty()) {
                        try {
                            val parsed = cleanString.toDouble()
                            val formatted = formatter.format(parsed)
                            current = formatted
                            etBalance.setText(formatted)
                            etBalance.setSelection(formatted.length)
                        } catch (e: NumberFormatException) { }
                    } else {
                        current = ""
                    }
                    etBalance.addTextChangedListener(this)
                }
            }
        })

        toggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == R.id.btn_debt) {
                    isDebtAccount = true
                    layoutBalance.hint = "Jumlah Hutang Awal"
                    // Tampilkan field Debt, sembunyikan field Asset
                    layoutAccountType.isVisible = false // <-- Sembunyikan Jenis Akun
                    layoutDueDate.isVisible = true      // <-- Tampilkan Tanggal Bayar
                    layoutReminder.isVisible = true     // <-- Tampilkan Pengingat
                    selectedAccountType = null // Kosongkan tipe untuk debt
                    selectedIconResId = R.drawable.ic_debt // Atur ikon default untuk debt
                } else {
                    isDebtAccount = false
                    layoutBalance.hint = "Saldo Awal"
                    // Sembunyikan field Debt, tampilkan field Asset
                    layoutAccountType.isVisible = true // <-- Tampilkan Jenis Akun
                    layoutDueDate.isVisible = false    // <-- Sembunyikan Tanggal Bayar
                    layoutReminder.isVisible = false   // <-- Sembunyikan Pengingat
                    // Reset field debt
                    etDueDate.setText("")
                    spinnerReminder.setText("", false)
                    selectedDueDateMillis = null
                    selectedReminderDays = null
                }
            }
        }

        etAccountType.setOnClickListener {
            if (!isDebtAccount) { // Hanya buka jika bukan Debt
                val picker = AccountTypePickerFragment()
                picker.onAccountTypeSelectedListener = { type ->
                    etAccountType.setText(type.name)
                    selectedAccountType = type.name
                    selectedIconResId = type.iconResId
                }
                picker.show(parentFragmentManager, "AccountTypePicker")
            }
        }

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            dueDateCalendar.set(Calendar.YEAR, year)
            dueDateCalendar.set(Calendar.MONTH, monthOfYear)
            dueDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            etDueDate.setText(sdf.format(dueDateCalendar.time))
            selectedDueDateMillis = dueDateCalendar.timeInMillis
        }
        etDueDate.setOnClickListener {
            if (isDebtAccount) { // Hanya buka jika Debt
                DatePickerDialog(
                    requireContext(), dateSetListener,
                    dueDateCalendar.get(Calendar.YEAR),
                    dueDateCalendar.get(Calendar.MONTH),
                    dueDateCalendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }

        val reminderAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            reminderLabels
        )
        spinnerReminder.setAdapter(reminderAdapter)
        spinnerReminder.setOnItemClickListener { parent, _, position, _ ->
            val selectedLabel = parent.getItemAtPosition(position) as String
            selectedReminderDays = reminderOptions[selectedLabel]
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString()
            val balanceString = etBalance.text.toString().replace(".", "")
            var balance = balanceString.toDoubleOrNull() ?: 0.0

            if (isDebtAccount && balance > 0) {
                balance *= -1
            }

            val isValid = name.isNotEmpty() && (isDebtAccount || selectedAccountType != null)

            if (isValid) {
                lifecycleScope.launch {
                    db.accountDao().insertAccount(
                        Account(
                            name = name,
                            initialBalance = balance,
                            accountType = selectedAccountType, // Akan null jika Debt
                            iconResId = selectedIconResId,     // Default ikon debt atau pilihan user
                            isDebtAccount = isDebtAccount,
                            dueDate = selectedDueDateMillis,        // <-- Simpan
                            reminderDaysBefore = selectedReminderDays // <-- Simpan
                        )
                    )
                    dismiss()
                }
            }
        }
    }
}