package io.opencui.dispatcher

import io.opencui.core.*
import io.opencui.serialization.Json
import io.opencui.core.Dispatcher
import io.opencui.core.user.UserInfo
import io.opencui.du.ClojureInitializer
import io.opencui.du.TfRestBertNLUModel
import io.opencui.sessionmanager.*
import kotlinx.coroutines.runBlocking

/**
 * This dispatcher is designed to be used for debugging the chatbot in the commandline.
 */
class CMDDispatcher {
	companion object {
        @JvmStatic
        fun init(
            botPrefix: String,
            duDuckling: String = "http://127.0.0.1:8000/parse",
            duHost: String = "127.0.0.1",
            duPort: Int = 3001,
            duProtocol: String = "http"
        ) {
            Dispatcher.setBotPrefix(botPrefix)

            val botInfo = Dispatcher.master()
            ClojureInitializer.init(listOf("en", "zh"), listOf("./core/libs/duckling-0.4.24-standalone.jar"))

            RuntimeConfig.put(TfRestBertNLUModel::class, Triple(duHost, duPort, duProtocol))
            RuntimeConfig.put(ChatbotLoader::class, InMemoryBotStore(botInfo))

            // TODO: for persistent, we need more extra coding and deployment of storage.
            val sessionManager = SessionManager(InMemorySessionStore(), InMemoryBotStore(botInfo))

            // This make sure that we keep the existing index if we have it.
            // I think the dispatcher can not be used as is.
            Dispatcher.sessionManager = sessionManager

            // ChatbotLoader.init(File("./jardir/"), botPrefix)
            Dispatcher.logger.info("finish the builder initialization.")
        }

		@JvmStatic
		fun main(args: Array<String>) {
			// val botInfo = botInfo("me.test", "frameVR_0222")
			// val botInfo = botInfo("me.test", "foodOrderingAppListOf")
			// val botInfo = botInfo("me.test", "slotupdate0724")
			val botInfo = botInfo("ai.bethere", "reservationCopilot")
            // {"type":"BuildReservationModule","slots": [{"value": "\"help me build a reservation module\"", "attribute": "rawUserInput"}], "packageName":"ai.bethere.builder"}
			init(botInfo.fullName)
			val userInfo = UserInfo("test_channel", "test_user", null)
			val sessionManager = Dispatcher.sessionManager

			val firstSession = sessionManager.createUserSession(userInfo, botInfo)
			firstSession.sessionId = "DummySessionIdForTesting"
			val mainEvent = FrameEvent("Main", emptyList(), emptyList(), "${botInfo.fullName}")
			var responses = runBlocking { sessionManager.getReplySync(firstSession, "", userInfo.channelType!!, listOf(mainEvent)) }

			while (true) {
				println(Json.encodeToJsonElement(responses).toPrettyString())
				print("Enter text or event in json format:")

				var line = readLine()?.trim()
				
				// line is not blank now.
				while (line.isNullOrEmpty()) {
					println("Input is empty, please enter text or event in json format:")
					line = readLine()?.trim()
				}

				if (!line.startsWith("{")) {
					println("Your input is: ${line?.trim()}")

					val session: UserSession = sessionManager.getUserSession(userInfo, botInfo)!!
					responses = runBlocking {  sessionManager.getReplySync(session, line!!, userInfo.channelType!!) }
				} else {
					val event = Json.decodeFromString<FrameEvent>(line)
					responses = runBlocking {  sessionManager.getReplySync(firstSession, "", userInfo.channelType!!, listOf(event)) }
				}
			}
		}
	}
}

