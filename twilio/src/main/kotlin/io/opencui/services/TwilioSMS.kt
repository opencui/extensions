package io.opencui.services

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import services.opencui.smsSender.ISmsSender


// This is used to send the SMS
class TwilioSMSSender(val info: Configuration) : ISmsSender {

    init {
        Twilio.init(info[SID]!!, info[ACCESSTOKEN]!!)
    }

    public fun sendSms(phoneNumer: String, content: String): Unit {
        val to = com.twilio.type.PhoneNumber(phoneNumer);
        val from = com.twilio.type.PhoneNumber(info[SOURCE]!!)
        message = Message.creator(to, from, content).create()
    }

    companion object : ExtensionBuilder<ISmsSender> {
        val logger = LoggerFactory.getLogger(WhatsappChannel::class.java)
        const val ACCESSTOKEN = "access_token"
        const val SOURCE = "number"
        const val SID = "account_sid"
        override fun invoke(config: Configuration): ISmsSender  {
            return TwilioSMSSender(config)
        }
    }

}


