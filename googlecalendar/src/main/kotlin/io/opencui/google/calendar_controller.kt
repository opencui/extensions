package io.opencui.google

import io.opencui.core.Dispatcher
import io.opencui.core.master
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import services.google.calendar.ReservationProvider
import services.opencui.reservation.IReservation
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Instead of create provider specific endpoint, here we create a provider independent endpoint
 * for triggering function.
 * So we specify module qualified name in the path, and
 */
@RestController
class GoogleCalendarEventWatcher() {
	@PostMapping("/hook/v1/google_calendar/update")
	fun trigger(
		@RequestHeader("X-Goog-Channel-Id") channelId: String
	): String {
		// clear thread local logs
		logger.info("Event watcher got triggered for $channelId.")
		// To get pull events, we need to figure out calendar id.
		val botInfo = master()
		val chatbot = Dispatcher.getChatbot(botInfo)

		// We know what service google calendar is implementing.
		val extension = chatbot.getExtension<IReservation>()
			?: return "Bad, can not find google calendar extension."

		// For now, we only handle cancelled here if the provider is ReservationProvider defined here.
		if (extension is ReservationProvider) {
			extension.handleCancelled()
		}
		return "Ok"
	}

	init {
		logger.info("Init the GoogleCalendarEventWatcher...")
	}

	companion object {
		val logger: Logger = LoggerFactory.getLogger(GoogleCalendarEventWatcher::class.java)
	}
}
