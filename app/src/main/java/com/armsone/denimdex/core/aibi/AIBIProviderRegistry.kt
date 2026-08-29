package com.armsone.denimdex.core.aibi

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import org.json.JSONObject

class AIBIProviderRegistry(private val context: Context) {

    val runtimeJavaScript: String by lazy {
        try {
            context.assets.open("aibi-browser-runtime.js").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            ""
        }
    }

    private val providersJsonString: String by lazy {
        try {
            context.assets.open("aibi-providers.json").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            "{}"
        }
    }

    fun getProviderConfig(providerId: String = "chatgpt"): AIBIProviderConfig {
        return try {
            val root = JSONObject(providersJsonString)
            val providers = root.optJSONObject("providers")
            val pObj = providers?.optJSONObject(providerId) ?: return defaultChatGPTConfig()

            val id = pObj.optString("id", providerId)
            val displayName = pObj.optString("displayName", "OpenAI ChatGPT")
            val initialUrl = pObj.optString("initialUrl", "https://chatgpt.com/")

            val allowedScriptOrigins = optStringList(pObj, "allowedScriptOrigins", listOf(
                "https://chatgpt.com",
                "https://chat.openai.com"
            ))
            val allowedAuthOrigins = optStringList(pObj, "allowedAuthOrigins", listOf(
                "https://auth0.openai.com",
                "https://auth.openai.com",
                "https://accounts.google.com",
                "https://appleid.apple.com",
                "https://login.microsoftonline.com"
            ))

            val selectorsObj = pObj.optJSONObject("selectors")
            val selectors = if (selectorsObj != null) {
                AIBIProviderSelectors(
                    promptInput = optStringList(selectorsObj, "promptInput", listOf("#prompt-textarea", "textarea[data-id='root']", "div[contenteditable='true']#prompt-textarea", "div[role='textbox']")),
                    submitButton = optStringList(selectorsObj, "submitButton", listOf("button[data-testid='send-button']", "button[aria-label*='Send' i]", "button[aria-label*='보내기' i]", "button:has(svg[data-icon='arrow-up'])")),
                    stopButton = optStringList(selectorsObj, "stopButton", listOf("button[data-testid='stop-button']", "button[aria-label*='Stop' i]", "button[aria-label*='중지' i]")),
                    assistantMessage = optStringList(selectorsObj, "assistantMessage", listOf("[data-message-author-role='assistant']", "div.agent-turn", "article[data-turn='assistant']", "article[data-testid*='conversation-turn'] [data-message-author-role='assistant']", "article[data-testid*='conversation-turn'] .markdown", "div[data-testid*='conversation-turn'] .markdown")),
                    preCode = optStringList(selectorsObj, "preCode", listOf("pre code", "div.code-block pre")),
                    errorBanner = optStringList(selectorsObj, "errorBanner", listOf(".text-red-500", "[data-testid*='error-notification']", "div.border-red-500")),
                    loginIndicator = optStringList(selectorsObj, "loginIndicator", listOf("button[data-testid='login-button']", "a[href*='/auth/login']", "button:contains('Log in')")),
                    challengeIndicator = optStringList(selectorsObj, "challengeIndicator", listOf("#cf-challenge-running", "iframe[src*='challenges.cloudflare.com']", "#challenge-form")),
                    attachmentInput = optStringList(selectorsObj, "attachmentInput", listOf("input[type='file'][accept*='image']", "input[type='file'][accept*='.jpg']", "input[type='file'][accept*='.jpeg']", "input[type='file'][accept*='.png']", "input[type='file']")),
                    attachmentTrigger = optStringList(selectorsObj, "attachmentTrigger", listOf("button[aria-label*='Attach']", "button[aria-label*='첨부']", "button[data-testid='composer-plus-btn']", "button[aria-label*='Add photos']", "button[aria-label*='사진']")),
                    attachmentMenuAction = optStringList(selectorsObj, "attachmentMenuAction", emptyList()),
                    attachmentMenuActionText = optStringList(selectorsObj, "attachmentMenuActionText", listOf("사진", "Photos", "Upload photos")),
                    attachmentPreview = optStringList(selectorsObj, "attachmentPreview", listOf("div[data-testid*='attachment']", "img[alt='Uploaded image']", "button[aria-label*='uploaded image' i]", "button[aria-label*='업로드한 이미지']", "img[src*='/backend-api/estuary/content']", "div[class*='attachment-tile']"))
                )
            } else {
                defaultChatGPTSelectors()
            }

            val mediaCapObj = pObj.optJSONObject("mediaCapabilities")
            val mediaCapabilities = AIBIMediaCapabilities(
                supportsImages = mediaCapObj?.optBoolean("supportsImages", true) ?: true,
                maxImagesPerTask = mediaCapObj?.optInt("maxImagesPerTask", 20) ?: 20,
                requiresMultipleInputForBatch = mediaCapObj?.optBoolean("requiresMultipleInputForBatch", true) ?: true
            )

            AIBIProviderConfig(
                id = id,
                displayName = displayName,
                initialUrl = initialUrl,
                allowedScriptOrigins = allowedScriptOrigins,
                allowedAuthOrigins = allowedAuthOrigins,
                selectors = selectors,
                mediaCapabilities = mediaCapabilities
            )
        } catch (_: Exception) {
            defaultChatGPTConfig()
        }
    }

    private fun optStringList(parent: JSONObject, key: String, fallback: List<String>): List<String> {
        val arr = parent.optJSONArray(key) ?: return fallback
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val s = arr.optString(i)
            if (s.isNotBlank()) list.add(s)
        }
        return if (list.isNotEmpty()) list else fallback
    }

    private fun defaultChatGPTConfig(): AIBIProviderConfig {
        return AIBIProviderConfig(
            id = "chatgpt",
            displayName = "OpenAI ChatGPT",
            initialUrl = "https://chatgpt.com/",
            allowedScriptOrigins = listOf("https://chatgpt.com", "https://chat.openai.com"),
            allowedAuthOrigins = listOf("https://auth0.openai.com", "https://auth.openai.com", "https://accounts.google.com", "https://appleid.apple.com", "https://login.microsoftonline.com"),
            selectors = defaultChatGPTSelectors(),
            mediaCapabilities = AIBIMediaCapabilities(
                supportsImages = true,
                maxImagesPerTask = 20,
                requiresMultipleInputForBatch = true
            )
        )
    }

    private fun defaultChatGPTSelectors(): AIBIProviderSelectors {
        return AIBIProviderSelectors(
            promptInput = listOf("#prompt-textarea", "textarea[data-id='root']", "div[contenteditable='true']#prompt-textarea", "div[role='textbox']"),
            submitButton = listOf("button[data-testid='send-button']", "button[aria-label*='Send' i]", "button[aria-label*='보내기' i]", "button:has(svg[data-icon='arrow-up'])"),
            stopButton = listOf("button[data-testid='stop-button']", "button[aria-label*='Stop' i]", "button[aria-label*='중지' i]"),
            assistantMessage = listOf("[data-message-author-role='assistant']", "div.agent-turn", "article[data-turn='assistant']", "article[data-testid*='conversation-turn'] [data-message-author-role='assistant']", "article[data-testid*='conversation-turn'] .markdown", "div[data-testid*='conversation-turn'] .markdown"),
            preCode = listOf("pre code", "div.code-block pre"),
            errorBanner = listOf(".text-red-500", "[data-testid*='error-notification']", "div.border-red-500"),
            loginIndicator = listOf("button[data-testid='login-button']", "a[href*='/auth/login']", "button:contains('Log in')"),
            challengeIndicator = listOf("#cf-challenge-running", "iframe[src*='challenges.cloudflare.com']", "#challenge-form"),
            attachmentInput = listOf("input[type='file'][accept*='image']", "input[type='file'][accept*='.jpg']", "input[type='file'][accept*='.jpeg']", "input[type='file'][accept*='.png']", "input[type='file']"),
            attachmentTrigger = listOf("button[aria-label*='Attach']", "button[aria-label*='첨부']", "button[data-testid='composer-plus-btn']", "button[aria-label*='Add photos']", "button[aria-label*='사진']"),
            attachmentMenuAction = emptyList(),
            attachmentMenuActionText = listOf("사진", "Photos", "Upload photos"),
            attachmentPreview = listOf("div[data-testid*='attachment']", "img[alt='Uploaded image']", "button[aria-label*='uploaded image' i]", "button[aria-label*='업로드한 이미지']", "img[src*='/backend-api/estuary/content']", "div[class*='attachment-tile']")
        )
    }

    companion object {
        fun clearChatGPTWebSession(onComplete: () -> Unit) {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies {
                cookieManager.flush()
                WebStorage.getInstance().deleteAllData()
                onComplete()
            }
        }
    }
}
