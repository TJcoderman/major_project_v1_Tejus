package com.securebank.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardScreenTest {

    @Test
    fun masksSignedUpAccountNumberWithActualLastFourDigits() {
        assertEquals(
            "•••• •••• •••• 4321",
            maskAccountNumberForDisplay("987654321")
        )
    }

    @Test
    fun ignoresFormattingWhenMaskingAccountNumber() {
        assertEquals(
            "•••• •••• •••• 5555",
            maskAccountNumberForDisplay("1234-0000-5555")
        )
    }
}
