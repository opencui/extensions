package io.opencui.channel

import io.opencui.core.*
import io.opencui.core.user.IUserIdentifier
import io.opencui.core.user.UserInfo
import io.opencui.serialization.Json
import io.opencui.serialization.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import kotlinx.coroutines.reactor.asFlux

// https://platform.openai.com/docs/api-reference/making-requests

data class Message(
    val role: String,
    val content: String
)

data class Parameters(
    val type: String,
    val properties: Map<String, Any>
)


data class Function(
    val name: String,
    val description: String,
    val parameters: Parameters? = null
)


data class Tool(
    val type: String,
    val function: Function
)

data class Customer(
    val number: String
)

data class Call(
    val id: String,
    val orgId: String,
    val createdAt: String,
    val updatedAt: String,
    val type: String,
    val status: String,
    val assistantId: String,
    val customer: Customer? = null,
    val phoneNumberId: String? = null,
    val phoneCallProvider: String? = null,
    val phoneCallProviderId: String? = null,
    val phoneCallTransport: String? = null
)


data class PhoneNumber(
    val id: String,
    val orgId: String,
    val number: String,
    val createdAt: String,
    val updatedAt: String,
    val twilioAccountSid: String,
    val twilioAuthToken: String,
    val name: String,
    val provider: String
)


data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Float,
    val tools: List<Tool>,
    val stream: Boolean,
    val maxTokens: Int,
    val call: Call,
    val phoneNumber: PhoneNumber? = null,
    val customer: Customer? = null,
    val metadata: Map<String, Any>
)

// For no streaming, not verified.
data class Usage(
	val prompt_tokens: Int,
	val completion_tokens: Int,
	val total_tokens: Int,
	val completion_tokens_details: Map<String, Any>)



data class Choice(val index: Int, val messages: String, val logprobs: Any?, val finish_reason: String?)

data class CompletionResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val usage: Usage,
    val choices: List<Choice>
)

// according to: https://www.perplexity.ai/search/how-do-curl-with-post-BQr_SbXnRSCQZQdbIFoWcA
/*
{
  "id": "chatcmpl-123",
  "object": "chat.completion.chunk",
  "created": 1694268190,
  "model": "gpt-4o-mini",
  "choices": [
    {
      "index": 0,
      "delta": {
        "content": "partial content"
      },
      "finish_reason": null
    }
  ]
}
 */

// For streaming, not verified.
data class ChoiceDelta(val content: String?=null)

data class StreamChoice(
    val index: Int,
    val delta: ChoiceDelta,
    val finish_reason: String? = null,
    val logprobs: Float? = null
)


data class ChatCompletionChunk(
      val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val usage: Usage,
    val choices: List<StreamChoice>
)

// https://cookbook.openai.com/examples/how_to_stream_completions
// [Choice(delta=ChoiceDelta(content='', function_call=None, role='assistant', tool_calls=None), finish_reason=None, index=0, logprobs=None)]

// This is based on this:  https://github.com/VapiAI/advanced-concepts-custom-llm/blob/master/app/main.py
//  This is for inbound traffic.
@RestController
class VapiController {

    private val logger = LoggerFactory.getLogger(VapiController::class.java)

    @PostMapping(
        value = [
            "/IChannel/VapiChannel/v1/{label}/{lang}",
            "/IChannel/io.opencui.channel.VapiChannel/v1/{label}/{lang}",
            "/io.opencui.channel.IChannel/VapiChannel/v1/{label}/{lang}",
            "/io.opencui.channel.IChannel/io.opencui.channel.VapiChannel/v1/{label}/{lang}" ],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun chatCompletions(
        @PathVariable lang: String,
        @PathVariable label: String,
        @RequestBody request: ChatRequest): Flux<String> {

        val botInfo = master(lang)
        logger.info("got body: $request")
        val info = Dispatcher.getChatbot(botInfo).getConfiguration(label)
        if (info == null) {
            logger.info("could not find configure for $ChannelType/$label")
            return Flux.just("data: {'reason': No longer active}\n\n")
        }

        val userId = request.call.customer?.number ?: return Flux.just("data: {'reason': No phone number}\n\n")

        val utterance = request.messages.last().content

        // Before we process incoming message, we need to create user session.
        val userInfo = UserInfo(ChannelType, userId, label, true)

        val typeSink = TypeSink(ChannelType)

        val resultFlow : Flow<String> = Dispatcher.processInboundFlow(userInfo, master(lang), textMessage(utterance, userId), typeSink)

        return resultFlow.map {
            content : String -> fakeStreamOutput(content)
        }.asFlux().concatWith(Flux.just(fakeStreamOutput(null, true)))
    }

    companion object{
        const val ChannelType = "VapiChannel"

        fun fakeStreamOutput(content: String?, finish: Boolean = false, usage: Usage? = null) : String {
            val result = mapOf(
                "id" to  "bethere-123",   // what is this used by vapi for?
                "object" to  "chat.completion.chunk",  // what is this used by vapi for?
                "created" to System.currentTimeMillis(), // what is this used by vapi for?
                "model" to "compound-ai",
                "choices" to listOf(
                    mapOf(
                        "index" to 0,
                        "delta" to mapOf("content" to content),
                        "finish_reason" to if (finish) "stop" else null,
                    )
                ),
                "usage" to usage
            )
            return "data: {${Json.encodeToString(result)}}\n\n"
        }
    }
}


// This is useful for create the outbound message.
class VapiChannel(override val info: Configuration, val number: String) : IMessageChannel {

    companion object : ExtensionBuilder {
        val logger = LoggerFactory.getLogger(VapiChannel::class.java)
        const val messageType = "RESPONSE"
        const val PAGEACCESSTOKEN = "page_access_token"
        const val MARKSEEN = "mark_seen"
        const val TYPEON = "typing_on"
        const val NUMBER = "number"
        const val OK = "ok"
        const val ChannelType = "VapiChannel"

        override fun invoke(config: Configuration): IChannel {
            val number: String =  config[NUMBER]!! as String
            return VapiChannel(config, number)
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
