package com.armsone.denimdex.core.model

enum class SyncEligibilityState(val rawValue: String) {
    NOT_ELIGIBLE("not_eligible"),
    PENDING_CONSENT("pending_consent"),
    READY_FOR_SYNC("ready_for_sync"),
    SYNCED("synced");

    companion object {
        fun fromString(value: String): SyncEligibilityState =
            entries.find { it.rawValue.equals(value.trim(), ignoreCase = true) } ?: NOT_ELIGIBLE
    }
}
