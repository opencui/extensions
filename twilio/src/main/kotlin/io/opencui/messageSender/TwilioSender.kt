package io.opencui.messageSender

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.opencui.core.*
import org.slf4j.LoggerFactory

// This is used to send the SMS
class TwilioSender(val info: Configuration) : IMessageSender {
    init {
        Twilio.init(info[SID]!! as String, info[ACCESSTOKEN]!! as String)
    }

    public override fun send(phoneNumber: String, content: String): Unit {
        val to = PhoneNumber(phoneNumber);
        val from = PhoneNumber(info[SOURCE]!! as String)
        Message.creator(to, from, content).create()
    }

    companion object : ExtensionBuilder<IMessageSender> {
        val logger = LoggerFactory.getLogger(TwilioSender::class.java)
        const val ACCESSTOKEN = "access_token"
        const val SOURCE = "number"
        const val SID = "account_sid"
        override fun invoke(config: Configuration): IMessageSender  {
            return TwilioSender(config)
        }
    }
}


