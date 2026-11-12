package com.nexia.ai.ai

/** Cloud mode requires a separately hosted HTTPS backend; local inference is not yet bundled. */
class HybridAiManager(private val cloud: CloudAiEngine, private val local: LocalAiEngine = LocalAiEngine()) {
    fun answer(prompt: String, preferOffline: Boolean = false): String {
        require(prompt.isNotBlank() && prompt.length <= 12000) { "Mensaje inválido" }
        if (preferOffline) return local.answer(prompt)
        return try { cloud.answer(prompt) }
        catch (_: Exception) { local.answer(prompt) }
    }
}
