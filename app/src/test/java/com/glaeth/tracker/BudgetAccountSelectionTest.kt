package com.glaeth.tracker

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetAccountSelectionTest {
    @Test
    fun keepsSelectionWhenAccountBelongsToPerson() {
        val resolved = resolveAccountIdForPerson(
            accountIds = listOf("bank-ben", "card-ayse"),
            accountPersonIds = listOf("ben", "ayse"),
            personId = "ayse",
            selectedAccountId = "card-ayse",
        )
        assertEquals("card-ayse", resolved)
    }

    @Test
    fun fallsBackWhenSelectionBelongsToAnotherPerson() {
        val resolved = resolveAccountIdForPerson(
            accountIds = listOf("bank-ben", "card-ayse"),
            accountPersonIds = listOf("ben", "ayse"),
            personId = "ayse",
            selectedAccountId = "bank-ben",
        )
        assertEquals("card-ayse", resolved)
    }

    @Test
    fun clearsSelectionWhenPersonHasNoAccounts() {
        val resolved = resolveAccountIdForPerson(
            accountIds = listOf("bank-ben"),
            accountPersonIds = listOf("ben"),
            personId = "ayse",
            selectedAccountId = "bank-ben",
        )
        assertEquals("", resolved)
    }
}
