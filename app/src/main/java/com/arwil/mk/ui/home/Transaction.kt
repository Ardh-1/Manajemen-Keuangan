package com.arwil.mk.ui.home

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import kotlinx.parcelize.Parcelize
import com.arwil.mk.ui.wallet.Account

@Parcelize
@Entity(
    tableName = "transactions",
    // Tambahkan foreign key
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE // Jika Akun dihapus, transaksinya juga terhapus
        )
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long  = 0L,
    val accountId: Long,
    val title: String,
    val category: String,
    val amount: Double,
    val type: String,
    val date: Long
) : Parcelable // <-- Implementasikan Parcelable