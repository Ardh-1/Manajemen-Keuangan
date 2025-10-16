package com.arwil.mk.ui.home

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize // <-- Tambahkan anotasi ini
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long  = 0L,
    val title: String,
    val category: String,
    val amount: Double,
    val type: String,
    val date: Long
) : Parcelable // <-- Implementasikan Parcelable