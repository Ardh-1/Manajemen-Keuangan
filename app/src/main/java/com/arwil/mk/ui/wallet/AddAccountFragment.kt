package com.arwil.mk.ui.wallet

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class AddAccountFragment : BottomSheetDialogFragment() {

    private lateinit var db: AppDatabase
    private var selectedAccountType: String = ""
    private var selectedIconResId: Int = 0

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
        val etAccountType = view.findViewById<EditText>(R.id.et_account_type)
        val etBalance = view.findViewById<EditText>(R.id.et_initial_balance)
        val btnSave = view.findViewById<Button>(R.id.btn_save_account)

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

        etAccountType.setOnClickListener {
            val picker = AccountTypePickerFragment()
            picker.onAccountTypeSelectedListener = { type ->
                etAccountType.setText(type.name)
                selectedAccountType = type.name
                selectedIconResId = type.iconResId
            }
            picker.show(parentFragmentManager, "AccountTypePicker")
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString()
            // Bersihkan format titik sebelum menyimpan
            val balanceString = etBalance.text.toString().replace(".", "")
            val balance = balanceString.toDoubleOrNull() ?: 0.0

            if (name.isNotEmpty() && selectedAccountType.isNotEmpty()) {
                lifecycleScope.launch {
                    db.accountDao().insertAccount(
                        Account(
                            name = name,
                            initialBalance = balance,
                            accountType = selectedAccountType, // <-- Simpan
                            iconResId = selectedIconResId      // <-- Simpan
                        )
                    )
                    dismiss()
                }
            }
        }
    }
}