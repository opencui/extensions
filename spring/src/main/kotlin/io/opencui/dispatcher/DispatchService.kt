package io.opencui.dispatcher

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.opencui.core.*
import io.opencui.core.Dispatcher
import io.opencui.du.DucklingRecognizer
import io.opencui.du.TfRestBertNLUModel
import io.opencui.du.dump
import io.opencui.sessionmanager.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
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
	@Value("\${bot.prefix:}") val botPrefix: String,
	@Value("\${indexing:}") val indexing: String
) {

	@EventListener(ApplicationReadyEvent::class)
	fun init() {
		ObjectMapper().registerModule(KotlinModule())

		RuntimeConfig.put(DucklingRecognizer::class, duDuckling)
		RuntimeConfig.put(TfRestBertNLUModel::class, Triple(duHost, duPort.toInt(), duProtocol))
		Dispatcher.memoryBased = false
		Dispatcher.botPrefix = botPrefix
		val botInfo = master()
		if (indexing.toBoolean()) {
			try {
				ChatbotLoader.init(File("./jardir/"), botPrefix)
				// now we need to create files for python code.
				val agent = ChatbotLoader.findChatbot(botInfo).duMeta
				val path = "./dumeta/${agent.getOrg()}_${agent.getLabel()}_${agent.getLang()}_${agent.getBranch()}"
				agent.dump(path)
			} catch (e: Exception) {
				e.printStackTrace()
			} finally {
				Dispatcher.logger.info("finish the indexing, exit...")
				exitProcess(0)
			}
		} else {
			// This make sure that we keep the existing index if we have it.
			val sessionManager = SessionManager(InMemorySessionStore(), InMemoryBotStore(botInfo))
			Dispatcher.sessionManager = sessionManager
			Dispatcher.botPrefix = botPrefix
			ChatbotLoader.init(File("./jardir/"), botPrefix)
			Dispatcher.logger.info("finish the builder initialization.")
		}
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			println("******************************** starting from spring...")
			runApplication<DispatchService>(*args)
		}
	}
}

