package com.armsone.denimdex.core.data.local

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "denimdex_user_preferences"
        private const val KEY_DID_ACKNOWLEDGE_AI_TRANSFER = "did_acknowledge_ai_transfer"
        private const val KEY_LAST_SYNC_PROMPT_COUNT = "last_sync_prompt_count"
        private const val KEY_EXPLICIT_CHATGPT_LOGOUT = "explicit_chatgpt_logout"
    }

    var didAcknowledgeAITransfer: Boolean
        get() = prefs.getBoolean(KEY_DID_ACKNOWLEDGE_AI_TRANSFER, false)
        set(value) = prefs.edit().putBoolean(KEY_DID_ACKNOWLEDGE_AI_TRANSFER, value).apply()

    var lastSyncPromptCount: Int
        get() = prefs.getInt(KEY_LAST_SYNC_PROMPT_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_SYNC_PROMPT_COUNT, value).apply()

    var explicitChatGPTLogout: Boolean
        get() = prefs.getBoolean(KEY_EXPLICIT_CHATGPT_LOGOUT, false)
        set(value) = prefs.edit().putBoolean(KEY_EXPLICIT_CHATGPT_LOGOUT, value).apply()
}
