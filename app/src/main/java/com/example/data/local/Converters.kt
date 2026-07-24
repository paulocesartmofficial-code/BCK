package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.InstallmentStatus
import com.example.data.model.LoanStatus
import com.example.data.model.PaymentFrequency

class Converters {
    @TypeConverter
    fun fromFrequency(value: PaymentFrequency): String = value.name

    @TypeConverter
    fun toFrequency(value: String): PaymentFrequency = try {
        PaymentFrequency.valueOf(value)
    } catch (e: Exception) {
        PaymentFrequency.MONTHLY
    }

    @TypeConverter
    fun fromLoanStatus(value: LoanStatus): String = value.name

    @TypeConverter
    fun toLoanStatus(value: String): LoanStatus = try {
        LoanStatus.valueOf(value)
    } catch (e: Exception) {
        LoanStatus.ACTIVE
    }

    @TypeConverter
    fun fromInstallmentStatus(value: InstallmentStatus): String = value.name

    @TypeConverter
    fun toInstallmentStatus(value: String): InstallmentStatus = try {
        InstallmentStatus.valueOf(value)
    } catch (e: Exception) {
        InstallmentStatus.PENDING
    }
}
