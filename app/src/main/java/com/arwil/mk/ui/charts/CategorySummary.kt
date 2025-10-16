package com.arwil.mk.ui.charts

import android.graphics.Color

data class CategorySummary(
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Float,
    val iconResId: Int,
    val color: Int
)