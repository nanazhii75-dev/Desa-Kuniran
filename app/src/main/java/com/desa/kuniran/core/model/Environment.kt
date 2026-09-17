package com.desa.kuniran.core.model

data class WasteSchedule(
    val id: String,
    val dayText: String,
    val timeText: String,
    val wasteType: String,
    val notes: String
)

data class WasteBankInfo(
    val title: String,
    val scheduleText: String,
    val location: String,
    val priceGuide: List<WastePriceItem>
)

data class WastePriceItem(
    val category: String,
    val pricePerKg: Money
)
