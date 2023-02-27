package io.opencui.dispatcher

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.opencui.channel.IChannel
import io.opencui.core.*
import io.opencui.core.Dispatcher
import io.opencui.core.user.IUserIdentifier
import io.opencui.core.user.UserInfo
import io.opencui.du.DucklingRecognizer
import io.opencui.du.TfRestBertNLUModel
import io.opencui.serialization.JsonObject
import io.opencui.sessionmanager.*
import java.io.File
import java.util.logging.Logger

data class CMDChannel(override val info: Configuration?) : IChannel {
	override fun getProfile(botInfo: BotInfo, id: String): IUserIdentifier? {
		return UserInfo("cmd", id, "master")
	}

	override fun sendWhitePayload(
		id: String,
		rawMessage: IWhitePayload,
		botInfo: BotInfo,
		source: IUserIdentifier?
	): IChannel.Status {
		println(rawMessage.toString())
		return IChannel.Status("everything is good.")
	}

	override fun sendRawPayload(
		uid: String,
		rawMessage: JsonObject,
		botInfo: BotInfo,
		source: IUserIdentifier?
	): IChannel.Status {
		println(rawMessage.toPrettyString())
		return IChannel.Status("everything is good.")
	}

	companion object : ExtensionBuilder<IChannel> {
        private val logger = Logger.getLogger(CMDChannel::class.java.name)
        private const val EXCEPTION_WAS_THROWN = "an exception was thrown"
        private const val CLIETTOKEN = "client_token"
        private const val CREDENTIAL = "credential"

        val channelType = "cmd"

        override fun invoke(config: Configuration): IChannel {
            return CMDChannel(config)
        }
    }
}


/**
 * This dispatcher is designed to be used for debugging the chatbot in the commandline.
 */
class CMDDispatcher(
	val botPrefix: String,
	val duDuckling: String = "http://127.0.0.1:8000/parse",
	val duHost: String =  "127.0.0.1",
	val duPort: Int = 8501,
	val duProtocol: String = "http"
) {

	init {
		ObjectMapper().registerModule(KotlinModule())
		Dispatcher.botPrefix = botPrefix

		val botInfo = master()

		RuntimeConfig.put(DucklingRecognizer::class, duDuckling)
		RuntimeConfig.put(TfRestBertNLUModel::class, Triple(duHost, duPort, duProtocol))
 		RuntimeConfig.put(ChatbotLoader::class, InMemoryBotStore(botInfo))

		// TODO: for persistent, we need more extra coding and deployment of storage.
		val sessionManager = SessionManager(InMemorySessionStore(), InMemoryBotStore(botInfo))

		// This make sure that we keep the existing index if we have it.
		// I think the dispatcher can not be used as is.
		Dispatcher.sessionManager = sessionManager
		Dispatcher.botPrefix = botPrefix
		// ChatbotLoader.init(File("./jardir/"), botPrefix)
		Dispatcher.logger.info("finish the builder initialization.")
	}

	companion object {
		@JvmStatic
		fun init(
			botPrefix: String,
			duDuckling: String = "http://127.0.0.1:8000/parse",
			duHost: String =  "127.0.0.1",
			duPort: Int = 8501,
			duProtocol: String = "http") {
			ObjectMapper().registerModule(KotlinModule())
			Dispatcher.botPrefix = botPrefix

			val botInfo = master()

			RuntimeConfig.put(DucklingRecognizer::class, duDuckling)
			RuntimeConfig.put(TfRestBertNLUModel::class, Triple(duHost, duPort, duProtocol))
			RuntimeConfig.put(ChatbotLoader::class, InMemoryBotStore(botInfo))

			// TODO: for persistent, we need more extra coding and deployment of storage.
			val sessionManager = SessionManager(InMemorySessionStore(), InMemoryBotStore(botInfo))

			// This make sure that we keep the existing index if we have it.
			// I think the dispatcher can not be used as is.
			Dispatcher.sessionManager = sessionManager
			Dispatcher.botPrefix = botPrefix
			// ChatbotLoader.init(File("./jardir/"), botPrefix)
			Dispatcher.logger.info("finish the builder initialization.")
		}

		@JvmStatic
		fun main(args: Array<String>) {
			val botInfo = botInfo("me.test", "frameVR_0222")
			init(botInfo.fullName)
			val userInfo = UserInfo("test_channel", "test_user", null)
			val sessionManager = Dispatcher.sessionManager

			val firstSession = sessionManager.createUserSession(userInfo, botInfo)
			val mainEvent = FrameEvent("Main", emptyList(), emptyList(), "${botInfo.fullName}")
			var responses = sessionManager.getReply(firstSession, "", listOf(userInfo.channelType!!), listOf(mainEvent))

			while (true) {
				println(Json.encodeToJsonElement(responses).toPrettyString())
				print("Enter text: ")
    			        val line = readLine()
				
				// line is not blank now.
				if (line.isNullOrEmpty()) {
					println("Input is empty.")
					print("Enter text")
				}
				println("Your input is: ${line?.strip()}")
				val session: UserSession = sessionManager.getUserSession(userInfo, botInfo)!!
				responses = sessionManager.getReply(session, line!!, listOf(userInfo.channelType!!))
			}
		}
	}
}

