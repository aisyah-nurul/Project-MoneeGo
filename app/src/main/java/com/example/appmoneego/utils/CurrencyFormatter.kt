package com.example.appmoneego.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    private val localeID = Locale("in", "ID")

    /**
     * Format double ke "Rp 10.400.000"
     */
    fun format(amount: Double): String {
        val nf = NumberFormat.getInstance(localeID)
        nf.maximumFractionDigits = 0
        return "Rp ${nf.format(amount)}"
    }

    /**
     * Format dengan tanda +/- untuk pemasukan/pengeluaran
     */
    fun formatWithSign(amount: Double): String {
        val nf = NumberFormat.getInstance(localeID)
        nf.maximumFractionDigits = 0
        return if (amount >= 0) "+ Rp ${nf.format(amount)}"
        else "- Rp ${nf.format(-amount)}"
    }

    /**
     * Parse string "10.400.000" atau "Rp 10.400.000" ke Double
     */
    fun parse(input: String): Double {
        return try {
            val cleaned = input
                .replace("Rp", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(",", ".")
                .trim()
            cleaned.toDouble()
        } catch (e: NumberFormatException) {
            0.0
        }
    }
}