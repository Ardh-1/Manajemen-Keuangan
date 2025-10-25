package com.arwil.mk.ui.home

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.arwil.mk.ui.wallet.Account

@Dao
interface AccountDao {

    @Insert
    suspend fun insertAccount(account: Account)

    @Update // <-- Tambahkan ini
    suspend fun updateAccount(account: Account)

    @Delete // <-- Tambahkan ini
    suspend fun deleteAccount(account: Account)

    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): LiveData<List<Account>>

    // Kita akan butuh fungsi lain nanti, tapi ini cukup untuk sekarang
}