package com.arwil.mk.ui.wallet

import android.app.AlertDialog
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
import java.text.NumberFormat
import java.util.Locale

class EditAccountFragment : BottomSheetDialogFragment() {

    var onAccountUpdated: ((Account) -> Unit)? = null
    var onAccountDeleted: ((Account) -> Unit)? = null

    private lateinit var account: Account
    private lateinit var db: AppDatabase

    private var selectedAccountType: String = ""
    private var selectedIconResId: Int = 0

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
        val formatter = NumberFormat.getNumberInstance(Locale("in", "ID"))

        // Isi field dengan data yang ada
        etName.setText(account.name)
        etAccountType.setText(account.accountType)
        etBalance.setText(formatter.format(account.initialBalance))

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
            val picker = AccountTypePickerFragment()
            picker.onAccountTypeSelectedListener = { type ->
                etAccountType.setText(type.name)
                selectedAccountType = type.name
                selectedIconResId = type.iconResId
            }
            picker.show(parentFragmentManager, "AccountTypePicker")
        }

        btnUpdate.setOnClickListener {
            val newName = etName.text.toString()
            val newBalanceString = etBalance.text.toString().replace(".", "")
            val newBalance = newBalanceString.toDoubleOrNull() ?: account.initialBalance

            if (newName.isNotEmpty() && selectedAccountType.isNotEmpty()) {
                val updatedAccount = account.copy(
                    name = newName,
                    initialBalance = newBalance,
                    accountType = selectedAccountType,
                    iconResId = selectedIconResId
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