package com.glaeth.tracker

/**
 * Picks a valid account for [personId].
 *
 * The budget form can display a fallback account chip when the stored selection belongs to
 * another person; always resolve through this helper before persisting a transaction so
 * ledger rows cannot be silently attributed to the wrong vault account.
 */
internal fun resolveAccountIdForPerson(
    accountIds: List<String>,
    accountPersonIds: List<String>,
    personId: String,
    selectedAccountId: String,
): String {
    require(accountIds.size == accountPersonIds.size) {
        "accountIds and accountPersonIds must be the same size"
    }
    val visibleIds = accountIds.indices
        .filter { accountPersonIds[it] == personId }
        .map { accountIds[it] }
    if (selectedAccountId in visibleIds) return selectedAccountId
    return visibleIds.firstOrNull().orEmpty()
}
