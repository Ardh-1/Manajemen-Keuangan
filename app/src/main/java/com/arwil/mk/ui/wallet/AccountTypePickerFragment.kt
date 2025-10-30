package com.arwil.mk.ui.wallet
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arwil.mk.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AccountTypePickerFragment : BottomSheetDialogFragment() {
    var onAccountTypeSelectedListener: ((AccountType) -> Unit)? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account_type_picker, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rvAccountTypes = view.findViewById<RecyclerView>(R.id.rv_account_types)

        // Nanti Anda bisa isi daftar ini
        val types = listOf(
            AccountType("Tunai", R.drawable.ic_wallet),
            AccountType("Bank", R.drawable.ic_bank), // Ganti dengan ikon bank
            AccountType("E-Wallet", R.drawable.ic_e_wallet), // Ganti dengan ikon e-wallet
            AccountType("Lainnya", R.drawable.ic_other)
        )

        val adapter = AccountTypeAdapter(types) { selectedType ->
            onAccountTypeSelectedListener?.invoke(selectedType)
            dismiss()
        }
        rvAccountTypes.adapter = adapter
        rvAccountTypes.layoutManager = GridLayoutManager(context, 4)
    }
}