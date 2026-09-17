package com.desa.kuniran.core.model

import java.text.NumberFormat
import java.util.Locale

@JvmInline
value class Money(val rupiah: Long) {
    fun toFormattedRupiah(): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        format.maximumFractionDigits = 0
        return format.format(rupiah).replace("Rp", "Rp ")
    }

    operator fun plus(other: Money): Money = Money(rupiah + other.rupiah)
    operator fun minus(other: Money): Money = Money(rupiah - other.rupiah)
    operator fun compareTo(other: Money): Int = rupiah.compareTo(other.rupiah)

    companion object {
        val ZERO = Money(0L)
    }
}
