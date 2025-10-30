package com.arwil.mk.ui.wallet

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val initialBalance: Double,
    val accountType: String?,
    val iconResId: Int,
    val isDebtAccount: Boolean,
    val dueDate: Long? = null,
    val reminderDaysBefore: Int? = null
) : Parcelable