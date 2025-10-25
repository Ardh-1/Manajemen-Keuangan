package com.arwil.mk.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arwil.mk.R
import com.arwil.mk.ui.home.AppDatabase
import androidx.lifecycle.lifecycleScope
import com.arwil.mk.ui.home.Transaction
import kotlinx.coroutines.launch
import com.arwil.mk.ui.home.toRupiahFormat
import kotlin.math.abs

class WalletFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var accountAdapter: AccountAdapter
    private var latestAccounts: List<Account> = emptyList()
    private var allTransactions: List<Transaction> = emptyList()
    private lateinit var tvNetAssets: TextView
    private lateinit var tvAssets: TextView
    private lateinit var tvDebts: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())

        tvNetAssets = view.findViewById(R.id.tv_net_assets)
        tvAssets = view.findViewById(R.id.tv_assets)
        tvDebts = view.findViewById(R.id.tv_debts)

        val rvAccounts = view.findViewById<RecyclerView>(R.id.rv_accounts)
        val btnAddAccount = view.findViewById<Button>(R.id.btn_add_account)

        setupRecyclerView(rvAccounts)
        observeData()

        rvAccounts.layoutManager = LinearLayoutManager(requireContext())
        rvAccounts.adapter = accountAdapter

        // Tampilkan sheet untuk tambah akun
        btnAddAccount.setOnClickListener {
            val addAccountFragment = AddAccountFragment()
            addAccountFragment.show(parentFragmentManager, "AddAccountFragment")
        }
    }

    private fun setupRecyclerView(rvAccounts: RecyclerView) {
        accountAdapter = AccountAdapter(emptyList()) { selectedAccount ->
            showEditAccountSheet(selectedAccount)
        }
        rvAccounts.layoutManager = LinearLayoutManager(requireContext())
        rvAccounts.adapter = accountAdapter
    }

    private fun showEditAccountSheet(account: Account) {
        val editFragment = EditAccountFragment.newInstance(account)

        editFragment.onAccountUpdated = { updatedAccount ->
            lifecycleScope.launch {
                db.accountDao().updateAccount(updatedAccount)
            }
        }

        editFragment.onAccountDeleted = { accountToDelete ->
            lifecycleScope.launch {
                db.accountDao().deleteAccount(accountToDelete)
            }
        }

        editFragment.show(parentFragmentManager, "EditAccountFragment")
    }

    private fun observeData() {
        // 1. Amati perubahan di tabel Akun
        db.accountDao().getAllAccounts().observe(viewLifecycleOwner, Observer { accounts ->
            latestAccounts = accounts
            updateAdapterData() // Perbarui UI
        })

        // 2. Amati perubahan di tabel Transaksi
        db.transactionDao().getAllTransactions().observe(viewLifecycleOwner, Observer { transactions ->
            allTransactions = transactions
            updateAdapterData() // Perbarui UI
        })
    }

    // Fungsi baru untuk menghitung total dan mengirim ke adapter
    private fun updateAdapterData() {
        // 1. Hitung total (pemasukan - pengeluaran) untuk setiap ID akun
        val transactionTotals = allTransactions
            .groupBy { it.accountId }
            .mapValues { entry ->
                entry.value.sumOf { transaction ->
                    if (transaction.type == "INCOME") transaction.amount
                    else -transaction.amount
                }
            }

        // 2. Hitung saldo akhir untuk setiap akun
        val finalBalances = latestAccounts.map { account ->
            account.initialBalance + (transactionTotals[account.id] ?: 0.0)
        }

        // 3. Hitung Aset, Hutang, dan Aset Bersih
        val totalAssets = finalBalances.filter { it > 0 }.sum()
        // Saldo negatif adalah hutang, sum() akan menjumlahkan angka negatif
        val totalDebts = finalBalances.filter { it < 0 }.sum()
        val netAssets = totalAssets + totalDebts // (misal: 1000 + (-300) = 700)

        // 4. Perbarui TextView di kartu atas
        tvNetAssets.text = netAssets.toRupiahFormat()
        tvAssets.text = totalAssets.toRupiahFormat()
        // Gunakan .abs() agar tidak ada tanda minus di tampilan hutang
        tvDebts.text = abs(totalDebts).toRupiahFormat()
        // 5. Kirim data baru ke adapter
        accountAdapter.updateData(latestAccounts, transactionTotals)
    }
}