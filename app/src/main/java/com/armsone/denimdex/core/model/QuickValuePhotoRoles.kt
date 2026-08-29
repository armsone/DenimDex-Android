package com.armsone.denimdex.core.model

object QuickValuePhotoRoles {
    const val minCount: Int = 1
    const val maxCount: Int = 20

    fun roleForIndex(index: Int): String {
        require(index in 0 until maxCount) { "Index must be between 0 and ${maxCount - 1}" }
        return "photo_${index + 1}"
    }

    fun allRoles(count: Int): List<String> {
        val bounded = count.coerceIn(0, maxCount)
        return (1..bounded).map { "photo_$it" }
    }

    fun isValidRole(role: String): Boolean {
        val trimmed = role.trim()
        if (!trimmed.startsWith("photo_")) return false
        val numberPart = trimmed.removePrefix("photo_").toIntOrNull() ?: return false
        return numberPart in minCount..maxCount
    }
}
