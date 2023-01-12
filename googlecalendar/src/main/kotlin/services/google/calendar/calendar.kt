package services.google.calendar

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventAttendee
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.directory.Directory
import com.google.api.services.directory.DirectoryScopes
import com.google.api.services.directory.model.CalendarResource
import kotlin.reflect.KMutableProperty0
import io.opencui.core.*
import io.opencui.core.da.*
import io.opencui.serialization.Json
import reservation.*
import services.opencui.reservation.*
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.Any

import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import java.time.LocalTime
import java.time.ZoneId

data class ImplResource(
    override var session: UserSession?, override var type: ResourceType?, override var name: String?
) : Resource {
    override var id: String? = null
    override fun createBuilder(p: KMutableProperty0<out Any?>?): FillBuilder {

        TODO("Not yet implemented")
    }

}

data class ReservationProvider(
    val config: Configuration,
    override var session: UserSession? = null,
) : IReservation, IProvider {
    val delegatedUser = config[DELEGATED_USER] as String
    val calendarId = config[CALENDAR_ID] as String
    val openHour = config[OPEN_HOUR].toString().toString().toInt()
    val closeHour = config[CLOSE_HOUR].toString().toInt()
    val open = LocalTime.of(openHour, 0)
    val close = LocalTime.of(closeHour, 0)
    val range = config[TIMERANGE].toString().toInt()
    val dayRange = config[DAYRANGE].toString().toInt()

    val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
    val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()
    val secrets_json = CLIENT_SECRET


    inline fun <reified S> buildService(): Calendar? {
        val credential = GoogleCredential.fromStream(secrets_json.byteInputStream(), HTTP_TRANSPORT, JSON_FACTORY)
            .createScoped(listOf(DirectoryScopes.ADMIN_DIRECTORY_RESOURCE_CALENDAR, CalendarScopes.CALENDAR))
            .createDelegated(delegatedUser)

        return Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName("Calendar API").build()
    }

    inline fun <reified S> buildAdminService(): Directory? {
        val credential = GoogleCredential.fromStream(secrets_json.byteInputStream(), HTTP_TRANSPORT, JSON_FACTORY)
            .createScoped(listOf(DirectoryScopes.ADMIN_DIRECTORY_RESOURCE_CALENDAR, CalendarScopes.CALENDAR))
            .createDelegated(delegatedUser)
        return Directory.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName("Calendar API").build()
    }


    override fun makeReservation(
        userId: String, resourceType: ResourceType, date: LocalDate?, time: LocalTime?, filter: List<ResourceFeature>?
    ): Reservation? {
        val first = findFirstAndBreak(
            resourceType, date, time, filter
        )
        if (first.isNullOrEmpty()) {
            return null
        } else {
            val calendar = buildService<Calendar>()
            val event = Event()
            val resource = first[0]
            event.summary = "Reservation"
            event.description = "Reservation"
            event.start = EventDateTime().setDateTime(DateTime(date.toString() + "T" + time.toString() + ":00+00:00"))
            event.end = EventDateTime().setDateTime(
                DateTime(
                    date.toString() + "T" + time?.plusHours(1).toString() + ":00+00:00"
                )
            )
            event.attendees = listOf(EventAttendee().setResource(true).setEmail(resource.resourceEmail))
            val createdEvent = calendar?.events()?.insert(calendarId, event)?.execute()
            println("createdEvent: ${createdEvent?.htmlLink}")
            val reservation = Reservation()
            val r = ImplResource(session = null, resourceType, name = resource.resourceName)
            r.id = createdEvent?.id
            reservation.id = createdEvent?.id
            reservation.duration = 1
            reservation.endDate = date
            reservation.endTime = time
            reservation.getResourceInfo()
            reservation.userId = userId
            reservation.resourceId = resource.resourceId
            reservation.startTime = time
            return reservation
        }

    }


    override fun listReservation(userId: String, resourceType: ResourceType): List<Reservation> {
        val service = buildService<Calendar>()
        val now = DateTime(System.currentTimeMillis())
        val reservations = mutableListOf<Reservation>()

        val events = service?.events()?.list("primary")?.setTimeMin(now)?.setOrderBy("startTime")?.setSingleEvents(true)
            ?.execute()?.items
        if (events != null) {
            for (event in events) {
                val reservation = Reservation()

                val rId = event.attendees?.get(0)?.email
                if (rId != null) {
                    getResourceInfo(rId)?.let { r ->
                        reservation.resourceId = r.id
                        reservation.getResourceInfo()
                    }
                }
                reservation.id = event.id
                reservation.userId = userId
                reservation.resourceId = event.attendees?.get(0)?.email
                reservation.startDate = Instant.ofEpochMilli(event.start?.dateTime?.value!!).atZone(ZoneId.systemDefault())
                    .toLocalDate()
                reservation.duration = range
                reservation.endTime = Instant.ofEpochMilli(event.start?.dateTime?.value!!).atZone(ZoneId.systemDefault())
                    .toLocalTime()
                reservation.getResourceInfo()
                reservations.add(reservation)
                return reservations
            }
        }
        return reservations

    }

    override fun cancelReservation(id: String): ValidationResult {
        val service = buildService<Calendar>()
        service?.events()?.delete(
            calendarId, id
        )?.execute()
        val result = ValidationResult()
        result.success = true
        result.message = "reservation canceled"
        return result
    }

    override fun resourceAvailable(
        type: ResourceType, date: LocalDate?, time: LocalTime?, filter: List<ResourceFeature>?
    ): ValidationResult {
        if (time != null) {
            if (time > close || time < open) {
                val validationResult = ValidationResult(session = null)
                validationResult.success = false
                validationResult.message = "Not in business hours"
                return validationResult
            }
        }
        val first = findFirstAndBreak(
            type, date, time, filter
        )
        if (first != null) {
            val validationResult = ValidationResult(session = null)
            validationResult.success = false
            validationResult.message = "No resources available"
            return validationResult
        } else {
            val validationResult = ValidationResult(session = null)
            validationResult.success = true
            validationResult.message = "Resources available"
            return validationResult
        }

    }
    override fun reservationUpdatable(
        reservationId: String, date: LocalDate, time: LocalTime, features: List<ResourceFeature>?
    ): ValidationResult {
        val first = findFirstAndBreak(resourceType = ResourceType("table"), date, time, features)
        if (first.isNullOrEmpty()) {
            val result = ValidationResult()
            result.success = true
            result.message = "Reservation can be updated"
            return result
        } else {
            val result = ValidationResult()
            result.success = false
            result.message = "Reservation cannot be updated"
            return result
        }
    }

    override fun updateReservation(
        reservationId: String, date: LocalDate?, time: LocalTime?, features: List<ResourceFeature>
    ): ValidationResult {
        val first = findFirstAndBreak(
            resourceType = ResourceType("table"), date, time, features
        )
        val service = buildService<Calendar>()
        val event = service?.events()?.get(
            calendarId, reservationId
        )?.execute()
        if (first.isNullOrEmpty()) {
            val result = ValidationResult()
            result.success = false
            result.message = "resource is not updatable"
            return result
        } else {

            event?.start = EventDateTime().setDateTime(DateTime(date.toString() + "T" + time.toString() + ":00+00:00"))
            event?.end = EventDateTime().setDateTime(
                DateTime(
                    date.toString() + "T" + time?.plusHours(1).toString() + ":00+00:00"
                )
            )
            event?.attendees = listOf(EventAttendee().setResource(true).setEmail(first[0].resourceEmail))
            service?.events()?.patch(
                calendarId, reservationId, event
            )?.execute()

            val validationResult = ValidationResult(session = null)
            validationResult.success = false
            validationResult.message = "Failed to update reservation"
            return validationResult
        }

    }
    override fun reservationCancelable(id: String): ValidationResult {
        val service = buildService<Calendar>()
        val now = Instant.now()
        val event = service?.Events()?.get("primary", id)?.execute()
        if (now.isAfter(Instant.parse(event?.start?.dateTime.toString()))) {
            val result = ValidationResult()
            result.success = false
            result.message = "Cannot cancel event"
            return result
        } else {
            val result = ValidationResult()
            result.success = true
            result.message = "Reservation can be cancelled"
            return result

        }
    }


    override fun listLocation(): List<Location> {
        val service = buildAdminService<Directory>()
        val resources = service?.resources()?.calendars()?.list("my_customer")?.execute()?.items
        val locations = mutableListOf<Location>()
        if (resources != null) {
            for (resource in resources) {
                val location = Location(session = null)
                location.id = resource.resourceId
                location.name = resource.resourceName
                location.type = ResourceType(resource.resourceType)

                locations.add(location)
            }
        }
        return locations

    }
    override fun pickLocation(locationId: String): Location? {
        val service = buildAdminService<Directory>()
        val resource = service?.resources()?.calendars()?.get("my_customer", locationId)?.execute()
        val location = Location(session = null)
        location.id = resource?.resourceId
        location.name = resource?.resourceName
        location.type = resource?.resourceType?.let { ResourceType(it) }
        return location
    }

    override fun availableDates(
        resourceType: ResourceType, time: LocalTime?, filter: List<ResourceFeature>?
    ): List<LocalDate> {
        val service = buildService<Calendar>()
        val availableDates = mutableListOf<LocalDate>()
        val now = LocalDate.now()
        val range = dayRange
        val first = findFirstAndBreak(resourceType, now, time, filter)
        first.forEach { cal ->
            for (i in 0..range) {
                val start = DateTime(now.toString() + "T" + time.toString() + ":00+00:00")
                val end = DateTime(now.toString() + "T" + time?.plusHours(1).toString() + ":00+00:00")
                val today = LocalDate.now().plusDays(i.toLong())
                val events = service?.events()?.list(cal.resourceEmail)?.setTimeMin(start)?.setTimeMax(end)
                    ?.setOrderBy("startTime")?.setSingleEvents(true)?.execute()?.items
                if (events.isNullOrEmpty()) {
                    print(today)
                    availableDates.add(today)
                }

            }
        }

        return availableDates
    }

    override fun availableTimeRanges(
        resourceType: ResourceType, date: LocalDate?, filter: List<ResourceFeature>?
    ): List<TimeRange> {
        val start = LocalTime.of(9, 0)
        return checkEvent(date, close, open, start, resourceType, filter)

    }


    override fun getResourceInfo(resourceId: String): Resource? {
        val service = buildAdminService<Directory>()
        val cal = service?.resources()?.calendars()?.get("my_customer", resourceId)?.execute()
        val resource = ImplResource(session = null, type = null, name = null)
        resource.id = cal?.resourceId
        resource.name = cal?.resourceName
        resource.type = ResourceType(cal?.resourceType.toString())
        return resource

    }
    fun findFirstAndBreak(
        resourceType: ResourceType, date: LocalDate?, time: LocalTime?, filter: List<ResourceFeature>?
    ): MutableList<CalendarResource> {
        val service = buildService<Calendar>()
        val adminService = buildAdminService<Directory>()
        val rs = mutableListOf<CalendarResource>()
        val availableResource = mutableListOf<CalendarResource>()

        val calendarswithType = adminService?.resources()?.calendars()?.list("my_customer")?.execute()?.items?.filter {
            it.resourceType == resourceType.value
        }

        calendarswithType?.forEach {
            val rf = Json.decodeFromString<ResourceFeature>(it.resourceDescription)
            if (rf.key == filter?.get(0)?.key && rf.value == filter?.get(0)?.value) {
                rs.add(it)

            }
            rs.forEach { resource ->
                println(resource)
                val start = DateTime(date.toString() + "T" + time.toString() + ":00+00:00")
                print(start)
                val end = DateTime(date.toString() + "T" + time?.plusHours(1).toString() + ":00+00:00")
                val events = service?.events()?.list(resource.resourceEmail)?.setTimeMin(start)?.setTimeMax(end)
                    ?.execute()?.items
                if (events.isNullOrEmpty()) {
                    availableResource.add(resource)
                }

            }

        }

        return availableResource


    }
    fun checkEvent(date: LocalDate?, close: LocalTime, open: LocalTime, start: LocalTime, resourceType: ResourceType, filter: List<ResourceFeature>?): MutableList<TimeRange> {

        val end = start.plusHours(range.toLong())
        val tr = TimeRange()
        tr.startTime = start
        tr.endTime = end
        val timeRanges = mutableListOf<TimeRange>()
        while (start>= open && end<=close) {
            val service = buildService<Calendar>()
            val adminService = buildAdminService<Directory>()
            val rs = mutableListOf<CalendarResource>()
            val calendarswithType = adminService?.resources()?.calendars()?.list("my_customer")?.execute()?.items?.filter {
                it.resourceType == resourceType.value
            }

            calendarswithType?.forEach {
                val rf = Json.decodeFromString<ResourceFeature>(it.resourceDescription)
                if (rf.key == filter?.get(0)?.key && rf.value == filter?.get(0)?.value) {
                    rs.add(it)

                }
            }
            rs.forEach { cal ->
                val s = DateTime(date.toString() + "T" + open.toString() + ":00+00:00")
                val e = DateTime(date.toString() + "T" + end.toString() + ":00+00:00")
                val events = service?.events()?.list(cal.resourceEmail)?.setTimeMin(s)?.setTimeMax(e)
                    ?.setOrderBy("startTime")?.setSingleEvents(true)?.execute()?.items
                if (events.isNullOrEmpty()) {
                    print(open)
                    timeRanges.add(tr)
                }
                checkEvent(date,close,open,start.plusHours(range.toLong()),resourceType,filter)
            }
            break
        }
        return timeRanges

    }

    companion object : ExtensionBuilder<IReservation> {

        const val CLIENT_SECRET = "client_secret"
        const val CALENDAR_ID = "calendar_id"
        const val DELEGATED_USER = "delegate_user"
        const val OPEN_HOUR = "open_hour"
        const val CLOSE_HOUR = "close_hour"
        const val TIMERANGE = "time_range"
        const val DAYRANGE = "day_range"
        override fun invoke(config: Configuration): IReservation {
            return ReservationProvider(config)
        }
    }
}



