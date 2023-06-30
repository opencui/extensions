package io.opencui.system1

import io.opencui.core.Configuration
import io.opencui.core.ExtensionBuilder
import com.cjcrafter.openai.OpenAI
import com.cjcrafter.openai.chat.ChatMessage
import com.cjcrafter.openai.chat.ChatMessage.Companion.toAssistantMessage
import com.cjcrafter.openai.chat.ChatMessage.Companion.toSystemMessage
import com.cjcrafter.openai.chat.ChatMessage.Companion.toUserMessage
import com.cjcrafter.openai.chat.ChatRequest


data class ChatGPTSystem1(val key: String, val profile: String, val model: String) : ISystem1 {
    val openai = OpenAI(key)
    val systemMessage = profile.toSystemMessage()

    override fun response(msgs: List<CoreMessage>): String {
        val messages = mutableListOf<ChatMessage>()
        messages.add(systemMessage)
        for (msg in msgs) {
            messages.add(if (msg.user) msg.message.toUserMessage() else msg.message.toAssistantMessage())
        }
        val request = ChatRequest(model, messages)
        val response = openai.createChatCompletion(request)
        return response[0].message.content
    }

    companion object : ExtensionBuilder<ChatGPTSystem1> {
        override fun invoke(p1: Configuration): ChatGPTSystem1 {
            val systemPrompt = p1[profileKey] as String?
            val securityKey = p1[openaiKey]!! as String
            val model : String? = p1[modelKey] as String?
            return ChatGPTSystem1(securityKey, systemPrompt ?: "", model ?: "gpt-3.5-turbo-0613")
        }

        const val profileKey = "SystemPrompt"
        const val openaiKey = "OpenAIKey"
        const val modelKey = "Model"
    }
}