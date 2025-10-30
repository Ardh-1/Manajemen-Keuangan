package com.arwil.mk.ui.wallet

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.arwil.mk.R
import com.arwil.mk.ui.home.AppDatabase
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import java.text.NumberFormat
import java.util.Locale
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlin.math.abs
import com.google.android.material.textfield.TextInputLayout
import java.util.Calendar
import android.widget.AutoCompleteTextView
import androidx.core.view.isVisible
import java.text.SimpleDateFormat

class EditAccountFragment : BottomSheetDialogFragment() {

    var onAccountUpdated: ((Account) -> Unit)? = null
    var onAccountDeleted: ((Account) -> Unit)? = null

    private lateinit var account: Account
    private lateinit var db: AppDatabase

    private var selectedAccountType: String? = null
    private var selectedIconResId: Int = 0
    private var isDebtAccount: Boolean = false
    private val dueDateCalendar: Calendar = Calendar.getInstance()
    private var selectedDueDateMillis: Long? = null
    private var selectedReminderDays: Int? = null

    // Opsi pengingat
    private val reminderOptions = mapOf(
        "Tidak Usah" to null,
        "Hari-H" to 0,
        "H-1" to 1,
        "H-3" to 3,
        "H-7" to 7
    )
    private val reminderLabels = reminderOptions.keys.toList()

    companion object {
        private const val ARG_ACCOUNT = "account"

        fun newInstance(account: Account): EditAccountFragment {
            val fragment = EditAccountFragment()
            val args = Bundle()
            args.putParcelable(ARG_ACCOUNT, account)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.getDatabase(requireContext())
        arguments?.let {
            account = it.getParcelable(ARG_ACCOUNT)!!
            selectedAccountType = account.accountType
            selectedIconResId = account.iconResId
            isDebtAccount = account.isDebtAccount
            selectedDueDateMillis = account.dueDate
            selectedReminderDays = account.reminderDaysBefore
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etName = view.findViewById<EditText>(R.id.et_edit_account_name)
        val etBalance = view.findViewById<EditText>(R.id.et_edit_initial_balance)
        val etAccountType = view.findViewById<EditText>(R.id.et_edit_account_type)
        val btnUpdate = view.findViewById<Button>(R.id.btn_update_account)
        val btnDelete = view.findViewById<Button>(R.id.btn_delete_account)
        val layoutBalance = view.findViewById<TextInputLayout>(R.id.layout_edit_initial_balance)
        val layoutAccountType = view.findViewById<TextInputLayout>(R.id.layout_edit_account_type)
        val layoutDueDate = view.findViewById<TextInputLayout>(R.id.layout_edit_due_date)
        val etDueDate = view.findViewById<EditText>(R.id.et_edit_due_date)
        val layoutReminder = view.findViewById<TextInputLayout>(R.id.layout_edit_reminder)
        val spinnerReminder = view.findViewById<AutoCompleteTextView>(R.id.spinner_edit_reminder)
        val formatter = NumberFormat.getNumberInstance(Locale("in", "ID"))
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        if (account.name.equals("Cash", ignoreCase = true)) {
            etName.isEnabled = false
            etBalance.isEnabled = false
            etAccountType.isEnabled = false // Jika ada
            // Nonaktifkan field Debt jika ada
            etDueDate?.isEnabled = false
            spinnerReminder?.isEnabled = false

            btnUpdate.isEnabled = false
            btnUpdate.alpha = 0.5f // Buat terlihat nonaktif
            btnDelete.isEnabled = false
            btnDelete.alpha = 0.5f
        }

        // Isi field dengan data yang ada
        etName.setText(account.name)
        if (isDebtAccount) {
            layoutBalance.hint = "Jumlah Hutang Awal"
            // Tampilkan angka positif
            etBalance.setText(formatter.format(abs(account.initialBalance)))
            // Sembunyikan field Asset, tampilkan field Debt
            layoutAccountType.isVisible = false
            layoutDueDate.isVisible = true
            layoutReminder.isVisible = true

            // Isi data Debt yang ada
            selectedDueDateMillis?.let {
                dueDateCalendar.timeInMillis = it
                etDueDate.setText(sdf.format(dueDateCalendar.time))
            }
            // Cari label yang sesuai dengan nilai hari
            val reminderLabel = reminderOptions.entries.find { it.value == selectedReminderDays }?.key ?: "Tidak Usah"
            spinnerReminder.setText(reminderLabel, false)

        } else {
            layoutBalance.hint = "Saldo Awal"
            etBalance.setText(formatter.format(account.initialBalance))
            // Tampilkan field Asset, sembunyikan field Debt
            layoutAccountType.isVisible = true
            layoutDueDate.isVisible = false
            layoutReminder.isVisible = false
            etAccountType.setText(account.accountType)
        }

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

        etAccountType.setOnClickListener {
            if (!isDebtAccount) { // Hanya jika Asset
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
            etDueDate.setText(sdf.format(dueDateCalendar.time))
            selectedDueDateMillis = dueDateCalendar.timeInMillis
        }
        etDueDate.setOnClickListener {
            if (isDebtAccount) {
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

        btnUpdate.setOnClickListener {
            val newName = etName.text.toString()
            val newBalanceString = etBalance.text.toString().replace(".", "")
            var newBalance = newBalanceString.toDoubleOrNull() ?: abs(account.initialBalance)

            if (isDebtAccount && newBalance > 0) {
                newBalance *= -1
            }

            if (newName.isNotEmpty() && (isDebtAccount || selectedAccountType != null)) {
                val updatedAccount = account.copy(
                    name = newName,
                    initialBalance = newBalance,
                    accountType = selectedAccountType,
                    iconResId = selectedIconResId,
                    dueDate = selectedDueDateMillis,
                    reminderDaysBefore = selectedReminderDays
                    // isDebtAccount tidak berubah
                )
                onAccountUpdated?.invoke(updatedAccount)
                dismiss()
            }
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Hapus Akun")
                .setMessage("Yakin ingin menghapus '${account.name}'? Semua transaksi terkait akun ini juga akan terhapus.")
                .setPositiveButton("Ya, Hapus") { _, _ ->
                    onAccountDeleted?.invoke(account)
                    dismiss()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }
}