package io.opencui.system1

import io.opencui.core.Configuration
import io.opencui.core.ExtensionBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

data class OpenAIMessage(val role: String, val content: String)
fun List<CoreMessage>.convert(): List<OpenAIMessage> {
    return this.map { OpenAIMessage(if (it.user) "user" else "assistant", it.message) }
}


// Feedback is only useful when turns is empty.
data class System1Request(val prompt: String, val turns: List<OpenAIMessage>, val feedback: Map<String, Any>? = null)

data class System1Reply(val reply: String)

data class ChatGPTSystem1(val url: String, val prompt: String, val model: String? = null) : ISystem1 {
    val client = WebClient.builder()
      .baseUrl(url)
      .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .build()

    override fun response(msgs: List<CoreMessage>, feedback: Map<String, Any>?): String {
        val request = System1Request(prompt, msgs.convert(), feedback)
        val response = client.post()
            .uri("query")
            .body(Mono.just(request), System1Request::class.java)
            .retrieve()
            .bodyToMono(System1Reply::class.java)
        return response.block()!!.reply
    }

    companion object : ExtensionBuilder {
        override fun invoke(p1: Configuration): ISystem1 {
            val url = p1[urlKey]!! as String
            val systemPrompt = p1[profileKey] as String?
            val model : String? = p1[modelKey] as String?
            return ChatGPTSystem1(url, systemPrompt ?: "", model ?: "gpt-3.5-turbo-0613")
        }

        const val profileKey = "systemPrompt"
        const val modelKey = "model"
        const val urlKey = "url"
    }
}
