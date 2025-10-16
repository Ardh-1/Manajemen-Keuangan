package com.arwil.mk.ui.home

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TransactionDao {

    // Menyisipkan satu transaksi. OnConflictStrategy.REPLACE berarti jika ada data
    // dengan primary key yang sama, data lama akan diganti.
    @Insert
    suspend fun insertTransaction(transaction: Transaction)

    // Menghapus transaksi
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    // Mengambil semua transaksi, diurutkan dari yang terbaru,
    // dan mengembalikannya sebagai LiveData agar UI bisa update otomatis.
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): LiveData<List<Transaction>>
}