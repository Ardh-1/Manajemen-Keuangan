package com.arwil.mk.ui.home

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize // <-- Tambahkan anotasi ini
data class Transaction(
    val title: String,
    val category: String,
    val amount: Double,
    val type: String
) : Parcelable // <-- Implementasikan Parcelable