package io.opencui.dispatcher

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.opencui.core.*
import io.opencui.serialization.Json
import io.opencui.core.Dispatcher
import io.opencui.core.user.UserInfo
import io.opencui.du.DucklingRecognizer
import io.opencui.du.TfRestBertNLUModel
import io.opencui.sessionmanager.*

/**
 * This dispatcher is designed to be used for debugging the chatbot in the commandline.
 */
class CMDDispatcher {
	companion object {
		@JvmStatic
		fun init(
			botPrefix: String,
			duDuckling: String = "http://127.0.0.1:8000/parse",
			duHost: String =  "127.0.0.1",
			duPort: Int = 3001,
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
			// val botInfo = botInfo("me.test", "frameVR_0222")
			// val botInfo = botInfo("me.test", "foodOrderingAppListOf")
			val botInfo = botInfo("me.test", "tableReservationBot")

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

