package io.opencui.services

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import services.opencui.messageSender.IMessageSender


// This is used to send the SMS
class TwilioSender(val info: Configuration) : IMessageSender {
    init {
        Twilio.init(info[SID]!!, info[ACCESSTOKEN]!!)
    }

    public fun send(phoneNumer: String, content: String): Unit {
        val to = com.twilio.type.PhoneNumber(phoneNumer);
        val from = com.twilio.type.PhoneNumber(info[SOURCE]!!)
        Message.creator(to, from, content).create()
    }

    companion object : ExtensionBuilder<IMessageSender> {
        val logger = LoggerFactory.getLogger(WhatsappChannel::class.java)
        const val ACCESSTOKEN = "access_token"
        const val SOURCE = "number"
        const val SID = "account_sid"
        override fun invoke(config: Configuration): IMessageSender  {
            return TwilioSender(config)
        }
    }
}


