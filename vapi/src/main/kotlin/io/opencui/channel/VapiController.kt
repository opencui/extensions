package io.opencui.channel

import io.opencui.channel.IChannel
import io.opencui.core.*
import io.opencui.core.user.IUserIdentifier
import io.opencui.serialization.JsonObject
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.MediaType
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.util.*


// https://platform.openai.com/docs/api-reference/making-requests

data class CallInfo(
    val id: String?,
    val customer: CustomerInfo?
)

data class CustomerInfo(
    val number: String?
)

data class ChatMessage(val content: String, val role: String)

// There is no reason to have model.
data class RequestData(val messages: List<ChatMessage>, val call: CallInfo?, val model: String?=null, val stream: Boolean?=null)

data class Usage(
	val prompt_tokens: Int,
	val completion_tokens: Int,
	val total_tokens: Int,
	val completion_tokens_details: Map<String, Any>)

data class Choice(val index: Int, val messages: List<ChatMessage>, val logprobs: Any?, val finish_reason: String?)

data class CompletionResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val usage: Usage,
    val choices: List<Choice>
)

data class ChatCompletionChunk(
    val chatId: String,
    val role: String?,
    val content: String?
)


@RestController
class VapiController {

    fun processMessage(message: String, callId: String, customerPhoneNumber: String, isVoice: Boolean): Flux<ChatCompletionChunk> {
        // Call the assistant and generate the response in a streaming fashion
        return Flux.create { emitter ->
            val chatId = "chatcmpl-${UUID.randomUUID().toString().replace("-", "")}"

            // Emit the role chunk
            emitter.next(ChatCompletionChunk(chatId, "assistant", null))

            // Emit the content chunks
            /*
            val assistantResponse = getAssistantResponse(message, callId, customerPhoneNumber, isVoice)
            assistantResponse.forEach { chunk ->
                emitter.next(chunk)
            }
            */
            // Emit the finish chunk
            emitter.next(ChatCompletionChunk(chatId, null, "stop"))
            emitter.complete()
        }
    }



    @GetMapping(produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @PostMapping("/vapi/v1/chat/completions")
    fun chatCompletions(@RequestBody request: RequestData): ResponseEntity<Flux<ChatCompletionChunk>> {
        val streaming = request.stream ?: false

        val messages = request.messages ?: return ResponseEntity.badRequest()
            .body(Flux.error(IllegalArgumentException("messages field is required")))

        val lastMessage = messages.last().content
        val callId = request.call?.id ?: "default-voice-id"
        val customerPhoneNumber = request.call?.customer?.number?.replace("+1", "") ?: "webCall"

        val response = processMessage(lastMessage, callId, customerPhoneNumber, true)

        return ResponseEntity.ok().body(response)
    }


    @PostMapping("/vapi/v1/chat/completions")
    fun openaiAdvancedCustomLlmRoute(@RequestBody requestData: RequestData): ResponseEntity<out Any> {
        val streaming = requestData.stream ?: false
        val lastMessage = requestData.messages.lastOrNull()?.content

        if (lastMessage == null) {

        }
/**
        val promptTemplate = """
            Create a prompt which can act as a prompt template where I put the original prompt and it can modify it according to my intentions so that the final modified prompt is more detailed. You can expand certain terms or keywords.
            ----------
            PROMPT: ${lastMessage?.content}.
            MODIFIED PROMPT: 
        """.trimIndent()

        // Simulate a completion
        val modifiedPrompt = simulateCompletion(promptTemplate)

        val modifiedMessages = requestData.messages?.dropLast(1)?.plus(ChatMessage(listOf(TextContent(modifiedPrompt)), lastMessage?.role ?: ""))

        if (streaming) {
            val emitter = SseEmitter()
            val responseStream = simulateStreamingResponse(modifiedMessages ?: listOf())

            runBlocking {
                for (jsonData in responseStream) {
                    emitter.send(SseEmitter.event().data(jsonData))
                    delay(100)
                }
                emitter.complete()
            }

            return ResponseEntity(emitter, HttpHeaders(), 200)
        } else {
            val nonStreamingResponse = simulateNonStreamingResponse(RequestData(modifiedMessages, false.toString()))
            return ResponseEntity.ok(nonStreamingResponse)
        }
        */
        return ResponseEntity("", HttpHeaders(), 200)
    }

    private fun simulateStreamingResponse(messages: List<ChatMessage>): List<String> {
        // Simulate a stream of JSON responses
        return messages.map { message ->
            """
            {
                "content": "${message.content}"
            }
            """.trimIndent()
        }
    }

    private fun simulateNonStreamingResponse(requestData: RequestData): String {
        // Simulate a JSON response for non-streaming
        return """
        {
            "content": "${requestData.messages?.lastOrNull()?.content}"
        }
        """.trimIndent()
    }

    private fun simulateCompletion(prompt: String): String {
        // Simulate modifying the prompt
        return "$prompt [expanded details...]"
    }
}


// This is useful for create the outbound message.
class VapiChannel(override val info: Configuration) : IMessageChannel {

    companion object : ExtensionBuilder {
        val logger = LoggerFactory.getLogger(VapiChannel::class.java)
        const val messageType = "RESPONSE"
        const val PAGEACCESSTOKEN = "page_access_token"
        const val MARKSEEN = "mark_seen"
        const val TYPEON = "typing_on"
        const val OK = "ok"
        const val ChannelType = "MessengerChannel"

        override fun invoke(config: Configuration): IChannel {
            return VapiChannel(config)
        }
    }

    override fun sendSimpleText(
        uid: String,
        rawMessage: TextPayload,
        botInfo: BotInfo,
        source: IUserIdentifier?
    ): IChannel.Status {
        TODO("Not yet implemented")
    }

    override fun sendRichCard(
        uid: String,
        rawMessage: RichPayload,
        botInfo: BotInfo,
        source: IUserIdentifier?
    ): IChannel.Status {
        TODO("Not yet implemented")
    }

    override fun sendListText(
        uid: String,
        rawMessage: ListTextPayload,
        botInfo: BotInfo,
        source: IUserIdentifier?
    ): IChannel.Status {
        TODO("Not yet implemented")
    }

    override fun sendListRichCards(
        uid: String,
        rawMessage: ListRichPayload,
        botInfo: BotInfo,
        source: IUserIdentifier?
    ): IChannel.Status {
        TODO("Not yet implemented")
    }

    override fun getIdentifier(botInfo: BotInfo, id: String): IUserIdentifier? {
        TODO("Not yet implemented")
    }

    override fun sendRawPayload(
        uid: String,
        rawMessage: JsonObject,
        botInfo: BotInfo,
        source: IUserIdentifier?
    ): IChannel.Status {
        TODO("Not yet implemented")
    }
}
