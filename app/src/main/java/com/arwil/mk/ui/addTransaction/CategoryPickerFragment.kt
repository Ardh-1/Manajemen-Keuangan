package com.arwil.mk.ui.addTransaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arwil.mk.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CategoryPickerFragment : BottomSheetDialogFragment() {

    // Listener untuk mengirim kategori yang dipilih kembali
    var onCategorySelectedListener: ((Category) -> Unit)? = null

    companion object {
        private const val ARG_TRANSACTION_TYPE = "transaction_type"

        fun newInstance(transactionType: String): CategoryPickerFragment {
            val fragment = CategoryPickerFragment()
            val args = Bundle()
            args.putString(ARG_TRANSACTION_TYPE, transactionType)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_category_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvCategories = view.findViewById<RecyclerView>(R.id.rv_categories)

        val transactionType = requireArguments().getString(ARG_TRANSACTION_TYPE)

        // Siapkan daftar kategori Anda di sini
        val incomeCategories = listOf(
            Category("Gaji", R.drawable.ic_salary),
            Category("Bonus", R.drawable.ic_bonus), // Pastikan Anda punya drawable ini
            Category("Hadiah", R.drawable.ic_gift), // Pastikan Anda punya drawable ini
            Category("Lainnya", R.drawable.ic_other)
        )

        val expenseCategories = listOf(
            Category("Makanan", R.drawable.ic_food),
            Category("Transport", R.drawable.ic_transport),
            Category("Tagihan", R.drawable.ic_bill),
            Category("Belanja", R.drawable.ic_shopping),
            Category("Hiburan", R.drawable.ic_entertainment),
            Category("Kesehatan", R.drawable.ic_health),
            Category("Lainnya", R.drawable.ic_other)
        )

        val categoriesToShow = when (transactionType) {
            "INCOME" -> incomeCategories
            else -> expenseCategories // Default ke expense
        }

        val adapter = CategoryAdapter(categoriesToShow) { selectedCategory ->
            // Kirim data kembali dan tutup bottom sheet
            onCategorySelectedListener?.invoke(selectedCategory)
            dismiss()
        }

        rvCategories.adapter = adapter
        // Gunakan GridLayoutManager untuk membuat layout grid, contoh: 4 kolom
        rvCategories.layoutManager = GridLayoutManager(context, 4)
    }
}