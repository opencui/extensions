package io.opencui.channel

import com.twilio.Twilio
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.inject.Inject
import com.twilio.type.PhoneNumber
import io.opencui.channel.IChannel
import io.opencui.core.BotInfo
import io.opencui.core.Configuration
import io.opencui.core.IMessageChannel
import com.fasterxml.jackson.annotation.JsonProperty
import io.opencui.core.*
import io.opencui.core.user.*
import io.opencui.serialization.*
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigInteger
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.security.MessageDigest

/**
 *
 */
data class TwilioRequest(
    val Body: String = "",
    val From: String = "",
    var To: String = ""
)


/**
 * Twilio channel type can only support one channel per agent.
 * For production, we should consider to use this for verify the incoming call.
 * https://www.twilio.com/docs/usage/webhooks/webhooks-security
 * how twilio call this webhook
 * https://www.twilio.com/docs/sms/twiml
 *
 */
@RestController
class TwilioResource{
    val logger: Logger = LoggerFactory.getLogger(TwilioResource::class.java)
    val md = MessageDigest.getInstance("MD5")

    @PostMapping(
        value = [
            "/IChannel/TwilioChannel/v1/{channelId}/{lang}",
            "/IChannel/io.opencui.channel.TwilioChannel/v1/{channelId}/{lang}",
            "/io.opencui.channel.IChannel/TwilioChannel/v1/{channelId}/{lang}",
            "/io.opencui.channel.IChannel/io.opencui.channel.TwilioChannel/v1/{channelId}/{lang}"])
    fun postResponse(
        @RequestParam body: TwilioRequest,
        @PathVariable lang: String,
        @PathVariable channelId: String): ResponseEntity<String> {
        val sessionId = body.From
        val userInfo = UserInfo(CHANNELTYPE, sessionId, channelId)
        val txt = body.Body
        val msgTimestemp = "$body.From}:${DateTimeFormatter.ISO_INSTANT.format(Instant.now())}"
        val msgId = BigInteger(1, md.digest(msgTimestemp.toByteArray())).toString(16).padStart(32, '0')
        Dispatcher.process(userInfo, master(lang), textMessage(txt, msgId))
        return ResponseEntity("EVENT_RECEIVED", HttpStatus.OK)
    }

    companion object {
        const val CHANNELTYPE = "twilio"
        const val DEFAULTCHANNEL = "restful"
    }
}



/**
 * Documented at
 * https://www.twilio.com/docs/chat/rest
 * https://www.twilio.com/docs/conversations/inbound-autocreation
 */
data class TwilioChannel(override val info: Configuration) : IMessageChannel {
    init {
        Twilio.init(info[ACCOUNTSID]!! as String, info[AUTHTOKEN]!! as String)
    }

    // We assume id is the target number for twilio channel.
    override fun sendSimpleText(
        uid: String,
        rawMessage: TextPayload,
        botInfo: BotInfo,
        source: IUserIdentifier?
    ): IChannel.Status {
        val message = com.twilio.rest.api.v2010.account.Message
            .creator(
                PhoneNumber(uid),  // to
                PhoneNumber(info[FROMNUMBER]!! as String),  // from
                rawMessage.text
            )
            .create()
        return IChannel.Status("send to ${message.sid}")
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

    override fun sendRawPayload(
        uid: String,
        rawMessage: JsonObject,
        botInfo: BotInfo,
        source: IUserIdentifier?
    ): IChannel.Status {
        TODO("Not yet implemented")
    }

    override fun getProfile(botInfo: BotInfo, id: String): IUserIdentifier? {
        TODO("Not yet implemented")
    }

    companion object : ExtensionBuilder<IChannel> {
        const val ACCOUNTSID = "account_sid"
        const val AUTHTOKEN = "auth_token"
        const val FROMNUMBER = "from_number"
        override fun invoke(p1: Configuration): IChannel {
            return TwilioChannel(p1)
        }
    }
}
