package io.opencui.system1

import io.opencui.core.Configuration
import io.opencui.core.ExtensionBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

data class System1Request(val prompt: String, val turns: List<CoreMessage>)
data class System1Reply(val reply: String)

data class ChatGPTSystem1(val url: String, val prompt: String, val key: String? = null, val model: String? = null) : ISystem1 {

    val client = WebClient.builder()
      .baseUrl(url)
      .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .build()

    override fun response(msgs: List<CoreMessage>): String {
        val request = System1Request(prompt, msgs)
        val response = client.post()
            .body(Mono.just(request), System1Request::class.java)
            .retrieve()
            .bodyToMono(System1Reply::class.java)
        return response.block()!!.reply
    }

    companion object : ExtensionBuilder<ChatGPTSystem1> {
        override fun invoke(p1: Configuration): ChatGPTSystem1 {
            val url = p1[urlKey]!! as String
            val systemPrompt = p1[profileKey] as String?
            val securityKey = p1[openaiKey]!! as String
            val model : String? = p1[modelKey] as String?
            return ChatGPTSystem1(url,systemPrompt ?: "", securityKey,model ?: "gpt-3.5-turbo-0613")
        }

        const val profileKey = "SystemPrompt"
        const val openaiKey = "OpenAIKey"
        const val modelKey = "Model"
        const val urlKey = "URL"
    }
}
