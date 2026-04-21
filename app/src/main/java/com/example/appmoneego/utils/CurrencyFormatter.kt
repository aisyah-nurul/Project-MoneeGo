package com.example.appmoneego.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    fun format(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(amount)
            .replace("Rp", "Rp")
            .replace(",00", "")
    }

    fun formatShort(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> "Rp${(amount / 1_000_000_000).toInt()}M"
            amount >= 1_000_000 -> "Rp${(amount / 1_000_000).toInt()}jt"
            amount >= 1_000 -> "Rp${(amount / 1_000).toInt()}rb"
            else -> format(amount)
        }
    }
}