package com.arwil.mk.ui.home

import java.text.NumberFormat
import java.util.Locale

// Ini adalah extension function untuk tipe data Double
fun Double.toRupiahFormat(): String {
    val localeID = Locale("in", "ID")
    val numberFormat = NumberFormat.getCurrencyInstance(localeID)
    numberFormat.maximumFractionDigits = 0 // Menghilangkan ,00 di akhir
    return numberFormat.format(this) // 'this' mengacu pada angka Double itu sendiri
}