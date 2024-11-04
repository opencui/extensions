package io.opencui.channel

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import reactor.core.publisher.Flux
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.asFlux
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.concurrent.Executors

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

// This demonstrates two different ways to serve SSE.
// file this up, and use: curl -X POST -d "limit=5"  http://localhost:8080/test
// to test.


@RestController
class FlowFluxController {

    // Sample endpoint that converts Flow<String> to Flux<String> for SSE
    @GetMapping(value = ["/flux"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamFlowAsFlux(): Flux<String> {
        // Sample Flow of String values
        val flow: Flow<String> = flow {
            emit("Message 1")
            kotlinx.coroutines.delay(1000)
            emit("Message 2")
            kotlinx.coroutines.delay(1000)
            emit("Message 3")
        }

        // Convert Flow to Flux for SSE
        return flow.asFlux()
    }

    // Sample endpoint that converts Flow<String> to Flux<String> for SSE
    @PostMapping(value = ["/test"], produces = ["text/event-stream"])
    fun streamPostFlowAsFlux(
            @RequestParam(value = "limit") limit: Int = 3,
    ): Flux<String> {
        var count = 0
        // Sample Flow of String values
        val flow: Flow<String> = flow {
            while (count < limit) {
                emit("Message $count")
                kotlinx.coroutines.delay(1000)
                count++
            }
        }

        // Convert Flow to Flux for SSE
        return flow.asFlux()
    }
}

@RestController
class SseController {

    private val executor = Executors.newSingleThreadScheduledExecutor()

    @GetMapping("/sse")
    fun sse(): SseEmitter {
        val emitter = SseEmitter()
        executor.execute {
            var count = 0
            while (count < 30) {
                try {
                    emitter.send("Event $count")
                    count++
                    Thread.sleep(1000)
                } catch (e: Exception) {
                    emitter.completeWithError(e)
                    break
                }
            }
            emitter.complete()
        }

        return emitter
    }
}