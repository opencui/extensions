package io.opencui.dispatcher

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.opencui.core.*
import io.opencui.core.Dispatcher
import io.opencui.du.RestNluService
import io.opencui.sessionmanager.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import java.io.File
import io.opencui.du.ClojureInitializer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController


@RestController
@Configuration
@SpringBootApplication(scanBasePackages = ["io.opencui"])
class DispatchService(
	@Value("\${du.duckling}") val duDuckling: String,
	@Value("\${du.host}") val duHost: String,
	@Value("\${du.port}") val duPort: String,
	@Value("\${du.protocol}") val duProtocol: String,
	@Value("\${bot.prefix:}") val botPrefix: String
) {

	// We try to find the agent jars here.
	val jarDirStr: String = "./jardir/"

	@EventListener(ApplicationReadyEvent::class)
	fun init() {
		ObjectMapper().registerModule(KotlinModule())

		// Use the same the format for new nlu service.
		RuntimeConfig.put(RestNluService::class, "$duProtocol://${duHost}:${duPort}")

		val languageStr: String = "en;zh"
		// We assume the launch directory structure and agent-{lang}.jar
		val languages = languageStr.split(";")

		val clojureInit = GlobalScope.async {
			// Start init in a different thread/coroutine
			ClojureInitializer.init(languages, listOf(duDuckling))
		}

		if (botPrefix.isNotEmpty()) {
			loadAgent(botPrefix)
			Dispatcher.logger.info("finish the builder initialization.")
		} else {
			Dispatcher.logger.info("finish the builder initialization without bot.")
		}

		runBlocking {
			// Wait for the clojure init to be done.
			clojureInit.join()
		}
	}

	@GetMapping("/botprefix/get")
    fun committed(): String {
        return Dispatcher.getBotPrefix() ?: ""
    }

	@GetMapping("/botprefix/set/{botPrefix}/")
    fun commit(
		@PathVariable botPrefix: String
	): ResponseEntity<String> {
		if (Dispatcher.getBotPrefix() != null && Dispatcher.getBotPrefix() != botPrefix)
			return ResponseEntity.badRequest().build()

		loadAgent(botPrefix)
		Dispatcher.logger.info("finish the builder initialization with bot.")
		return ResponseEntity.ok().build()
    }
	
	fun loadAgent(botPrefix: String) {
		Dispatcher.memoryBased = false
		Dispatcher.setBotPrefix(botPrefix)
		val botInfo = Dispatcher.master()
		// This make sure that we keep the existing index if we have it.
		Dispatcher.sessionManager = SessionManager(InMemorySessionStore(), InMemoryBotStore(botInfo))
		ChatbotLoader.init(File(jarDirStr), botPrefix)
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			println("******************************** starting from spring...")
			runApplication<DispatchService>(*args)
		}
	}
}

