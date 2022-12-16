package io.opencui.payment

import com.fasterxml.jackson.`annotation`.JsonIgnore
import com.stripe.Stripe
import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Event
import com.stripe.model.PaymentIntent
import com.stripe.model.PaymentMethod
import com.stripe.net.Webhook
import com.stripe.param.PaymentIntentCreateParams
import io.opencui.core.*
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.util.logging.Logger
import kotlin.Int
import kotlin.String

data class paymentPayload (
    var currency      : String?           = null,
    var amount        : Int?              = null,
    var paymentMethod : ArrayList<String> = arrayListOf()
)

public interface IPayment_1940 : IService {
    @JsonIgnore
    public fun getPaymentLink(amount: Int, currency: String): String?
}
data class StripeProvider(
    val config : Configuration,
    override var session: UserSession? = null
): IPayment_1940, IProvider{
    override fun getPaymentLink(amount: Int, currency: String): String {
        val baseurl = config[BASEURL] as String
        val label =  config[LABEL] as String
        val lang  =  config[LANG] as String
        val provider = config[PROVIDER] as String

        return "$baseurl/IPayment_1940/v1/$provider/$label/payment/$currency/$amount/card/$lang"
    }
    companion object: ExtensionBuilder<IPayment_1940> {
        const val APIKEY="api_secret"
        const val ENDPOINTSECRET= "webhook_secret"
        const val PUBLISHABLEKEY="publishable_key"
        const val LABEL = "label"
        const val PROVIDER="provider"
        const val LANG = "lang"
        const val BASEURL = "baseurl"
        override fun invoke(config: Configuration): IPayment_1940 {
            return StripeProvider(config)
        }
    }
}


@RestController
class StripeResource(){
    /*
    * Webhook is used to listen to events from stripe. This is used to listen to payment intent events.
    * The events are used to update the payment intent status.
    * When an event subscribed to is triggered, stripe sends a post request to the webhook endpoint.
    * This allows you to set up functions that are triggered when a payment intent is created, updated or canceled.
    * For more on this read the stripe documentation on webhooks(https://stripe.com/docs/webhooks)
    * */

    @PostMapping(
        value = [
            "/IPayment_1940/v1/{provider}/{label}/webhook/{lang}",
        ]
    )
    fun handleWebhook(
        @RequestBody payload: String,
        @PathVariable provider: String,
        @PathVariable label: String,
        @PathVariable lang: String,
        @RequestHeader("Stripe-Signature") sigHeader: String

    ): Any {
        Logger.getLogger("StripeResource").info("payload: $payload")
        val botInfo = BotInfo("", "", lang)

        val info = Dispatcher.getChatbot(botInfo).getConfiguration<IPayment_1940>(label)
            ?: return ResponseEntity("No longer active", HttpStatus.NOT_FOUND)

        var event: Event? = null
        try {
            event = Webhook.constructEvent(payload, sigHeader, info[ENDPOINTSECRET] as String?)
        }catch (e: SignatureVerificationException){
            Logger.getLogger("StripeResource").warning("Invalid signature")
        }
        when(event?.type){
            "payment_intent.succeeded" -> {
                val paymentIntent = event.data as PaymentIntent
                Logger.getLogger("StripeResource").info("PaymentIntent was successful!")
            }
            "payment_method.attached" -> {
                val paymentMethod = event.data as PaymentMethod
                Logger.getLogger("StripeResource").info("PaymentMethod was attached to a Customer!")
            }
            /*
            * Stripe  has more intent methods that can be subscribed to. These are just a few of them.
            * For more on this read the stripe documentation on webhooks(https://stripe.com/docs/webhooks)
            * */
        }
        /*
        * This is the endpoint that is used to create a payment intent.
        * It takes a payload of type paymentPayload as a parameter.
        * The payload contains the payment information.
        * For more on this read the stripe documentation on payment intents(https://stripe.com/docs/payments/payment-intents)
        * */
        @PostMapping(
            value = ["IPayment_1940/v1/{provider}/{label}/create-payment-intent/{lang}"]
        )
        fun createPaymentIntent(
            @RequestBody payload: paymentPayload,
            @PathVariable provider: String,
            @PathVariable label: String,
            @PathVariable lang: String

        ): ResponseEntity<Any> {

            val botInfo = BotInfo("", "", lang)

            val info = Dispatcher.getChatbot(botInfo).getConfiguration<IPayment_1940>(label)
                ?: return ResponseEntity("No longer active", HttpStatus.NOT_FOUND)

            Logger.getLogger("Configuration meta").info("payload: $info")

            Logger.getLogger("StripeResource").info("payload: $payload")
            Stripe.apiKey = info[APIKEY] as String
            val paymentIntentParams = PaymentIntentCreateParams.builder()
                .setCurrency(payload.currency)
                .setAmount(payload.amount?.toLong())
                .addPaymentMethodType("card")
                .build()

            val paymentIntent = PaymentIntent.create(paymentIntentParams)
            val body = mapOf("clientSecret" to paymentIntent.clientSecret)
            return ResponseEntity(body, HttpStatus.OK)
        }
        return ResponseEntity("OK", HttpStatus.OK)

    }
    //    get publishable key endpoint
    @GetMapping("/IPayment_1940/v1/{provider}/{label}/publishable-key/{lang}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getPublishableKey(
        @PathVariable provider: String,
        @PathVariable label: String,
        @PathVariable lang: String
    ): ResponseEntity<Any> {
        val botInfo = BotInfo("", "", lang)
        val info = Dispatcher.getChatbot(botInfo).getConfiguration<IPayment_1940>(label)
            ?: return ResponseEntity("No longer active", HttpStatus.NOT_FOUND)
        Logger.getLogger("Configuration meta").info("payload: $info")
        val res = mapOf("publishableKey" to info[PUBLISHABLEKEY])
        return ResponseEntity(res, HttpStatus.OK)
    }


    companion object{
        const val APIKEY="api_secret"
        const val ENDPOINTSECRET= "webhook_secret"
        const val PUBLISHABLEKEY="publishable_key"
        const val LABEL = "label"
        const val PROVIDER="provider"
        const val LANG = "lang"
    }
}


@Controller
@RequestMapping
class StripeController() {
    @GetMapping("IPayment_1940/v1/{provider}/{label}/payment/{currency}/{amount}/{paymentMethod}/{lang}")
    fun stripe(): String {
        return "payment"
    }
}


