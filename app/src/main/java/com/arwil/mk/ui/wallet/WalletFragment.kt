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
    private lateinit var assetAdapter: AccountAdapter // <-- Ubah
    private lateinit var debtAdapter: AccountAdapter

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

        val btnAddAccount = view.findViewById<Button>(R.id.btn_add_account)
        val rvAssetAccounts = view.findViewById<RecyclerView>(R.id.rv_asset_accounts)
        val rvDebtAccounts = view.findViewById<RecyclerView>(R.id.rv_debt_accounts)

        setupRecyclerViews(rvAssetAccounts, rvDebtAccounts)
        observeData()

        // Tampilkan sheet untuk tambah akun
        btnAddAccount.setOnClickListener {
            val addAccountFragment = AddAccountFragment()
            addAccountFragment.show(parentFragmentManager, "AddAccountFragment")
        }
    }

    private fun setupRecyclerViews(rvAsset: RecyclerView, rvDebt: RecyclerView) {
        // Setup adapter Aset
        assetAdapter = AccountAdapter(emptyList()) { selectedAccount ->
            showEditAccountSheet(selectedAccount)
        }
        rvAsset.layoutManager = LinearLayoutManager(requireContext())
        rvAsset.adapter = assetAdapter

        // Setup adapter Hutang
        debtAdapter = AccountAdapter(emptyList()) { selectedAccount ->
            showEditAccountSheet(selectedAccount)
        }
        rvDebt.layoutManager = LinearLayoutManager(requireContext())
        rvDebt.adapter = debtAdapter
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

        // 2. Hitung total saldo untuk *semua* akun
        val accountBalances = latestAccounts.associateBy(
            { it.id }, // Key: ID Akun
            { account -> // Value: Saldo Akhir
                // Saldo dinamis = Saldo Awal + Perubahan Transaksi
                val netChange = transactionTotals[account.id] ?: 0.0
                account.initialBalance + netChange
            }
        )

        // 3. Hitung Aset (semua saldo positif) dan Hutang (semua saldo negatif)
        val totalAssets = accountBalances.values.filter { it >= 0 }.sum()
        val totalDebts = accountBalances.values.filter { it < 0 }.sum()
        val netAssets = totalAssets + totalDebts // (misal: 1000 + (-300) = 700)

        // 4. Update kartu Net Assets
        tvNetAssets.text = netAssets.toRupiahFormat()
        tvAssets.text = totalAssets.toRupiahFormat()
        tvDebts.text = abs(totalDebts).toRupiahFormat()

        // 5. === Pisahkan daftar untuk DUA ADAPTER ===
        val assetList = latestAccounts.filter { !it.isDebtAccount }
        val debtList = latestAccounts.filter { it.isDebtAccount }

        // 6. Update kedua adapter
        assetAdapter.updateData(assetList, transactionTotals)
        debtAdapter.updateData(debtList, transactionTotals)
    }
}