package com.arwil.mk.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.arwil.mk.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.NumberFormat
import java.util.Locale
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.arwil.mk.ui.wallet.Account

class EditTransactionFragment : BottomSheetDialogFragment() {

    // Listener untuk mengirim aksi kembali ke HomeFragment
    var onTransactionUpdated: ((Transaction) -> Unit)? = null
    var onTransactionDeleted: ((Transaction) -> Unit)? = null

    private lateinit var transaction: Transaction
    private lateinit var db: AppDatabase
    private var accountList: List<Account> = emptyList()
    private var selectedAccountId: Long = -1L


    companion object {
        private const val ARG_TRANSACTION = "transaction"

        fun newInstance(transaction: Transaction): EditTransactionFragment {
            val fragment = EditTransactionFragment()
            val args = Bundle()
            args.putParcelable(ARG_TRANSACTION, transaction)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.getDatabase(requireContext()) // <-- BARU
        arguments?.let {
            transaction = it.getParcelable(ARG_TRANSACTION)!!
            selectedAccountId = transaction.accountId // <-- BARU: Simpan ID awal
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etTitle = view.findViewById<EditText>(R.id.et_edit_title)
        val etAmount = view.findViewById<EditText>(R.id.et_edit_amount)
        val etAccount = view.findViewById<AutoCompleteTextView>(R.id.et_edit_account)
        val btnUpdate = view.findViewById<Button>(R.id.btn_update)
        val btnDelete = view.findViewById<Button>(R.id.btn_delete)

        // Isi field dengan data transaksi yang ada
        etTitle.setText(transaction.title)
        val formatter = NumberFormat.getNumberInstance(Locale("in", "ID"))
        etAmount.setText(formatter.format(transaction.amount))

        etAmount.addTextChangedListener(object : TextWatcher {
            private var current = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (s.toString() != current) {
                    etAmount.removeTextChangedListener(this)

                    val cleanString = s.toString().replace("[.,]".toRegex(), "")

                    if (cleanString.isNotEmpty()) {
                        try {
                            val parsed = cleanString.toDouble()
                            val formatted = formatter.format(parsed)
                            current = formatted
                            etAmount.setText(formatted)
                            etAmount.setSelection(formatted.length)
                        } catch (e: NumberFormatException) {
                            // Abaikan error
                        }
                    } else {
                        current = ""
                    }

                    etAmount.addTextChangedListener(this)
                }
            }
        })

        setupAccountDropdown(etAccount)

        // (Anda bisa menambahkan TextWatcher untuk format angka di sini jika mau)

        btnUpdate.setOnClickListener {
            val updatedTitle = etTitle.text.toString()
            val updatedAmountString = etAmount.text.toString().replace(".", "")
            val updatedAmount = updatedAmountString.toDoubleOrNull() ?: transaction.amount

            if (updatedTitle.isNotEmpty() && selectedAccountId != -1L) {
                val updatedTransaction = transaction.copy(
                    accountId = selectedAccountId, // <-- Perbarui ID Akun
                    title = updatedTitle,
                    amount = updatedAmount
                    // Anda bisa tambahkan edit kategori/tanggal di sini jika mau
                )
                onTransactionUpdated?.invoke(updatedTransaction)
                dismiss()
            }
        }

        btnDelete.setOnClickListener {
            onTransactionDeleted?.invoke(transaction)
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Hapus")
                .setMessage("Apakah Anda yakin ingin menghapus transaksi '${transaction.title}'?")
                .setPositiveButton("Ya, Hapus") { _, _ ->
                    // Jika pengguna menekan "Ya", jalankan aksi hapus
                    onTransactionDeleted?.invoke(transaction)
                    dismiss() // Tutup bottom sheet setelah menghapus
                }
                .setNegativeButton("Batal", null) // "Batal" tidak melakukan apa-apa
                .show()
        }
    }

    private fun setupAccountDropdown(etAccount: AutoCompleteTextView) {
        db.accountDao().getAllAccounts().observe(viewLifecycleOwner, { accounts ->
            accountList = accounts
            val accountNames = accounts.map { it.name }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                accountNames
            )
            etAccount.setAdapter(adapter)

            // LANGKAH PENTING: Tampilkan akun yang sudah dipilih
            val currentAccount = accounts.find { it.id == transaction.accountId }
            if (currentAccount != null) {
                etAccount.setText(currentAccount.name, false) // false agar tidak memicu dropdown
            }
        })

        etAccount.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position) as String
            selectedAccountId = accountList.find { it.name == selectedName }?.id ?: -1L
        }
    }
}