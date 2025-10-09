package com.arwil.mk.ui.home

sealed class ListItem {
    data class DateHeader(val date: String) : ListItem()
    data class TransactionItem(val transaction: Transaction) : ListItem()
}