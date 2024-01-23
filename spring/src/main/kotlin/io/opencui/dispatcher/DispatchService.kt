package io.opencui.dispatcher

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.opencui.core.*
import io.opencui.core.Dispatcher
import io.opencui.du.DucklingRecognizer
import io.opencui.du.RestNluService
import io.opencui.du.TfRestBertNLUModel
import io.opencui.du.dump
import io.opencui.sessionmanager.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import java.io.File
import kotlin.system.exitProcess


@Configuration
@SpringBootApplication(scanBasePackages = ["io.opencui"])
class DispatchService(
	@Value("\${du.duckling}") val duDuckling: String,
	@Value("\${du.host}") val duHost: String,
	@Value("\${du.port}") val duPort: String,
	@Value("\${du.protocol}") val duProtocol: String,
	@Value("\${bot.prefix:}") val botPrefix: String
) {

	@EventListener(ApplicationReadyEvent::class)
	fun init() {
		ObjectMapper().registerModule(KotlinModule())

		RuntimeConfig.put(DucklingRecognizer::class, duDuckling)
		// Use the same the format for new nlu service.
		RuntimeConfig.put(RestNluService::class, "$duProtocol:://${duHost}:${duPort}")

		Dispatcher.memoryBased = false
		Dispatcher.botPrefix = botPrefix
		val botInfo = master()
		// This make sure that we keep the existing index if we have it.
		Dispatcher.sessionManager = SessionManager(InMemorySessionStore(), InMemoryBotStore(botInfo))
		Dispatcher.botPrefix = botPrefix
		ChatbotLoader.init(File("./jardir/"), botPrefix)
		Dispatcher.logger.info("finish the builder initialization.")

	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			println("******************************** starting from spring...")
			runApplication<DispatchService>(*args)
		}
	}
}

