package services.google.calendar


import com.fasterxml.jackson.databind.ObjectMapper
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
import io.opencui.core.*
import io.opencui.serialization.Json
import io.opencui.sessionmanager.ChatbotLoader
import org.slf4j.LoggerFactory
import services.opencui.reservation.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import java.time.LocalTime
import java.time.ZoneId
import java.util.logging.Logger



data class ReservationProvider(
    val config: Configuration,
    override var session: UserSession? = null,
) : IReservation, IProvider {

    val delegatedUser = config[DELEGATED_USER] as String

    val calendarId  =  config[CALENDAR_ID] as String

    val openHour =  config[OPEN_HOUR].toString().toInt()

    val closeHour =  config[CLOSE_HOUR].toString().toInt()

    val open = LocalTime.of(openHour, 0)

    val close = LocalTime.of(closeHour, 0)

    val range = config[TIMERANGE].toString().toInt()

    val dayRange = config[DAYRANGE].toString().toInt()

    val timezone = config[TIMEZONE] as String

    val secrets_json = config[CLIENT_SECRET] as String




    val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
    val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()


    override fun cloneForSession(userSession: UserSession): IExtension {
        return this.copy(session=userSession)
    }

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
        userId: String, resourceType: ResourceType, date: LocalDate?, time: LocalTime?, filter: List<Criterion>?
    ): Reservation? {
        val reservation = Reservation()

        val listOfResources = mutableListOf<CalendarResource>()

        if (filter == null) {
            val resources = getResourcesWhenFilterIsNull(resourceType)
            if (resources.isNullOrEmpty()) {
                return Reservation()

            } else {
                resources.forEach {
                    val event = getOneEvent(date!!, it.resourceEmail, time!!, time.plusHours(range.toLong()))
                    if (event.isNullOrEmpty()) {
                        listOfResources.add(it)
                    }
                }
                if (listOfResources.isNotEmpty()) {
                    val calendar = buildService<Calendar>()
                    val event = Event()
                    val resource = listOfResources[0]
                    event.summary = "Reservation for $userId"
                    event.description = "Reservation booked for ${resource.resourceName}"
                    val startTime = localDateTimeToDateTime(date!!, time!!)
                    val endTime = localDateTimeToDateTime(date, time.plusHours(range.toLong()))

                    event.start = EventDateTime().setDateTime(startTime).setTimeZone(timezone)

                    event.end = EventDateTime().setDateTime(endTime).setTimeZone(timezone)

                    event.attendees = listOf(EventAttendee().setResource(true).setEmail(resource.resourceEmail))
                    val createdEvent = calendar?.events()?.insert(calendarId, event)?.execute()
                    reservation.id = createdEvent?.id
                    reservation.duration = 1
                    reservation.endDate = date
                    reservation.endTime = time
                    reservation.userId = userId
                    reservation.resourceId = resource.resourceId
                    reservation.startTime = time
                    return reservation

                }

            }
        } else {
            val resources = getResourcesWhenFilterIsNotNull(resourceType, filter)
            resources?.forEach {
                val event = getOneEvent(date!!, it.resourceEmail, time!!, time.plusHours(range.toLong()))
                if (event.isNullOrEmpty()) {
                    listOfResources.add(it)
                }
            }
            if (listOfResources.isNotEmpty()) {
                val calendar = buildService<Calendar>()
                val event = Event()
                val resource = listOfResources[0]
                event.summary = "Reservation for $userId"
                event.description = "Reservation booked for ${resource.resourceName}"
                event.start =
                    EventDateTime().setDateTime(DateTime(date.toString() + "T" + time.toString() + ":00+00:00"))
                event.end = EventDateTime().setDateTime(
                    DateTime(
                        date.toString() + "T" + time?.plusHours(1).toString() + ":00+00:00"
                    )
                )
                event.attendees = listOf(EventAttendee().setResource(true).setEmail(resource.resourceEmail))
                val createdEvent = calendar?.events()?.insert(calendarId, event)?.execute()
                reservation.id = createdEvent?.id
                reservation.duration = 1
                reservation.endDate = date
                reservation.endTime = time
                reservation.userId = userId
                reservation.resourceId = resource.resourceId
                reservation.startTime = time
                return reservation
            }

        }
        return reservation
    }

    override fun listReservation(userId: String, resourceType: ResourceType): List<Reservation> {
        val service = buildService<Calendar>()
        val now = DateTime(System.currentTimeMillis())
        val reservations = mutableListOf<Reservation>()
        val events = service?.events()?.list(calendarId)?.execute()?.items
        if (events != null) {
            for (event in events) {
                if (event?.summary?.contains(userId) == true) {
                    val reservation = Reservation()
                    reservation.id = event.id
                    reservation.userId = userId
                    reservation.resourceId = event.attendees?.get(0)?.email
                    reservation.startDate =
                        Instant.ofEpochMilli(event.start?.dateTime?.value!!).atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    reservation.duration = range
                    reservation.startTime = convertFromDateTime(event.start.dateTime)
                    reservation.endTime =
                        Instant.ofEpochMilli(event.end?.dateTime?.value!!).atZone(ZoneId.systemDefault())
                            .toLocalTime()
                    reservations.add(reservation)
                }

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
        type: ResourceType, date: LocalDate?, time: LocalTime?, filter: List<Criterion>?
    ): ValidationResult {
        val today = LocalDate.now().plusDays(dayRange.toLong())
        if (date?.isAfter(today)!!) {
            val result = ValidationResult()
            result.success = false
            result.message = "Not available"
            return result
        }
        if (time != null) {
            if (time > close || time < open) {
                val validationResult = ValidationResult(session = null)
                validationResult.success = false
                validationResult.message = "Not in business hours"
                return validationResult
            } else {
                val event = getOneEvent(date, calendarId, time, time.plusHours(range.toLong()))
                if (event?.isNotEmpty() == true) {
                    val validationResult = ValidationResult(session = null)
                    validationResult.success = false
                    validationResult.message = "Not available"
                    return validationResult

                } else {
                    val validationResult = ValidationResult(session = null)
                    validationResult.success = true
                    validationResult.message = "Resource available"
                    return validationResult
                }
            }
        }
        if (date == null) {
            val today = LocalDate.now().plusDays(1)
            val result = availableTimeRanges(type, today, filter)


            if (result.isEmpty()) {
                val validationResult = ValidationResult(session)
                validationResult.success = false
                validationResult.message = "Resource not available"
                return validationResult
            } else {
                val validationResult = ValidationResult(session)
                validationResult.success = true
                validationResult.message = "Resource is available"
                return validationResult
            }

        } else {
            val result = availableTimeRanges(type, date, filter)

            if (result.isEmpty()) {
                val validationResult = ValidationResult(session)
                validationResult.success = false
                validationResult.message = "Resource not available"
                return validationResult
            } else {
                val validationResult = ValidationResult(session)
                validationResult.success = true
                validationResult.message = "Resource is available"
                return validationResult
            }
        }


    }

    override fun reservationUpdatable(
        reservationId: String, date: LocalDate, time: LocalTime, features: List<Criterion>?
    ): ValidationResult {
        val validationResult = ValidationResult()
        validationResult.success = false
        validationResult.message = "Reservation not updatable"
        val service = buildService<Calendar>()
        val event = service?.events()?.get(calendarId, reservationId)?.execute()
        val onDay = getOneEvent(date, calendarId, time, time.plusHours(range.toLong()))
        if (features == null) {
            if (event.isNullOrEmpty()) {
                validationResult.success = false
                validationResult.message = "Reservation not found"
                return validationResult
            } else {
                if (onDay.isNullOrEmpty()) {
                    validationResult.success = true
                    validationResult.message = "Reservation can be updated"
                    return validationResult
                } else {
                    validationResult.success = false
                    validationResult.message = "Reservation cannot be updated"
                    return validationResult
                }
            }
        } else {
            return validationResult

        }

    }

    override fun updateReservation(
        reservationId: String, date: LocalDate?, time: LocalTime?, features: List<Criterion>
    ): ValidationResult {
        val calendar = buildService<Calendar>()
        val validationResult = ValidationResult()

        val listResources = mutableListOf<CalendarResource>()

        val resources = getResourcesWhenFilterIsNotNull(resourceType = ResourceType("table"), features)
        val event = calendar?.Events()?.get(calendarId, reservationId)?.execute()
        if (event.isNullOrEmpty()) {
            validationResult.message = "cannot update"
            validationResult.success = false
        }
        if (resources.isNullOrEmpty()) {
            validationResult.message = "cannot update"
            validationResult.success = false
        } else {
            resources.forEach {
                val service = buildService<Calendar>()
                val events = service?.events()?.list(calendarId)?.setTimeMax(event?.start?.dateTime)
                    ?.setTimeMin(event?.end?.dateTime)?.execute()?.items
                if (events.isNullOrEmpty()) {
                    listResources.add(it)
                }
            }
            if (listResources.isEmpty()) {
                validationResult.message = "cannot update"
                validationResult.success = false
            } else {
                val calendar = buildService<Calendar>()
                val e = Event()
                val resource = listResources[0]
                e.summary = event?.summary
                e.description = event?.description
                e.start = event?.start
                e.attendees = event?.attendees
                val createdEvent = calendar?.events()?.insert(listResources[0].resourceEmail, event)?.execute()
                validationResult.success = true
                validationResult.message = "updated resource"

            }
        }
        return validationResult

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
        val resources = service?.resources()?.Buildings()?.list("my_customer")?.execute()?.buildings
        val locations = mutableListOf<Location>()
        if (resources != null) {
            for (resource in resources) {
                val location = Location(session = null)
                location.id = resource.buildingId
                location.name = resource.buildingName
                location.type = ResourceType(resource.description)

                locations.add(location)
            }
        }
        return locations
    }

    override fun pickLocation(locationId: String): Location? {
        val service = buildAdminService<Directory>()
        val resource = service?.resources()?.buildings()?.get("my_customer", locationId)?.execute()
        val location = Location(session = null)
        location.id = resource?.buildingId
        location.name = resource?.buildingName
        location.type = resource?.description?.let { ResourceType(it) }
        return location
    }

    override fun availableDates(
        resourceType: ResourceType, time: LocalTime?, filter: List<Criterion>?
    ): List<LocalDate> {
        val service = buildService<Calendar>()
        val availableDates = mutableListOf<LocalDate>()
        val now = LocalDate.now().plusDays(1)

        val range = dayRange
        if (time == null) {
            if (filter == null) {
                for (i in 0..dayRange) {

                    val events = availableTimeRanges(resourceType, now.plusDays(i.toLong()), null)
                    if (events.isNotEmpty()) {
                        if (availableDates.contains(now.plusDays(i.toLong()))) {
                        } else {
                            availableDates.add(now.plusDays(i.toLong()))
                        }
                    }

                }

            } else {
                for (i in 0..dayRange) {
                    val events = availableTimeRanges(resourceType, now.plusDays(i.toLong()), filter)

                    if (events.isNotEmpty()) {
                        if (availableDates.contains(now.plusDays(i.toLong()))) {
                        } else {
                            availableDates.add(now.plusDays(i.toLong()))
                        }
                    }

                }
            }
        } else {
            if (filter == null) {
                val resource = getResourcesWhenFilterIsNull(resourceType)
                resource?.forEach {
                    for (i in 0..dayRange) {
                        val events = getOneEvent(now, calendarId, time, time.plusHours(range.toLong()))
                        if (events?.isEmpty() == true) {
                            availableDates.add(now.plusDays(i.toLong()))
                        }
                    }

                }

            } else {
                val resource = getResourcesWhenFilterIsNotNull(resourceType, filter)
                resource?.forEach {
                    for (i in 0..dayRange) {
                        val events = getOneEvent(now, calendarId, time, time.plusHours(range.toLong()))
                        if (events?.isEmpty() == true) {
                            availableDates.add(now.plusDays(i.toLong()))
                        }
                    }

                }

            }

        }
        return availableDates
    }

    override fun availableTimeRanges(
        resourceType: ResourceType, date: LocalDate?, filter: List<Criterion>?
    ): List<TimeRange> {
        logger.debug("entering available timeRanges")
        val timeRanges = mutableListOf<TimeRange>()
        if (date == null) {
            if (filter == null) {
                val today = LocalDate.now().plusDays(1)
                return checkIfIsAfter(today, open)
            }
        } else {
            if (filter == null) {

                return checkIfIsAfter(date, open)
            } else {
                return checkIfIsAfter(date, open)
            }

        }
        return timeRanges
    }

    private fun checkIfIsAfter(date: LocalDate, start: LocalTime): MutableList<TimeRange> {
        var TimeRanges = mutableListOf<TimeRange>()
        val events = getAllEventsOn(date, calendarId)?.sortedBy {
            it.start.dateTime.value
        }

        var current = open
        if (events.isNullOrEmpty()) {
            val nullTr = TimeRange()
            nullTr.startTime = open
            nullTr.endTime = close
            TimeRanges.add(nullTr)
        } else {

            for (i in 0 until events.size) {

                val logger = LoggerFactory.getLogger(ReservationProvider::class.java)

                val start = convertFromDateTime(events[i].start.dateTime)

                if (start.isAfter(open)&& start!=current) {


                    val timeRange = TimeRange()

                    timeRange.startTime = current
                    timeRange.endTime = start.minusHours(range.toLong())

                    TimeRanges.add(timeRange)

                }
                val end = convertFromDateTime(events[i].end.dateTime)
                if (i < events.size - 1) {

                    val nextStart = convertFromDateTime(events[i + 1].start.dateTime)
                    if (nextStart.isAfter(open)&& start != current) {

                        if (end.isBefore(nextStart) ) {

                            val timeRange = TimeRange()
                                timeRange.startTime = end
                                timeRange.endTime = nextStart.minusHours(range.toLong())
                                TimeRanges.add(timeRange)


                            current = nextStart



                        } else if (end.isAfter(nextStart)) {


                            current = nextStart

                        } else if (end == nextStart) {

                            current = end

                        }
                    } else {

                        current = end

                    }
                } else {


                    current = end
                }
            }


            if (current.isBefore(close) && start !== current) {

                val timeRange = TimeRange()
                timeRange.startTime = current
                timeRange.endTime = close.minusHours(range.toLong())
                TimeRanges.add(timeRange)
            }
        }

        return TimeRanges
    }

    override fun getResourceInfo(resourceId: String): Resource? {
        val service = buildAdminService<Directory>()
        val calendar = service?.resources()?.calendars()?.get("my_customer", resourceId)?.execute()
        val resource = calendar?.let { Json.decodeFromString<Resource>(it.resourceDescription, ChatbotLoader.findClassLoader(session!!.botInfo)) }
        return resource
    }

    fun getResourcesWhenFilterIsNull(resourceType: ResourceType): List<CalendarResource>? {
        val adminService = buildAdminService<Directory>()
        val resources = adminService?.resources()?.calendars()?.list("my_customer")?.execute()?.items?.filter {
            it.resourceType == resourceType.value
        }
        return resources
    }

    fun getResourcesWhenFilterIsNotNull(
        resourceType: ResourceType, filter: List<Criterion>
    ): List<CalendarResource>? {
        val cals = mutableListOf<CalendarResource>()
        val resources = getResourcesWhenFilterIsNull(resourceType)

        filter.map { criterion ->
            resources?.forEach {
                val mapper = ObjectMapper()
                val filterItems = mapper.readValue(it.resourceDescription, Map::class.java)
                if (criterion.operator == ComparationOperator("=")) {
                    if (filterItems[criterion.key] == criterion.value) {
                        cals.add(it)
                    }
                } else if (criterion.operator == ComparationOperator("!=")) {
                    if (filterItems[criterion.key] != criterion.value) {
                    }

                }

            }
        }

        return cals
    }

    fun getAllEventsOn(date: LocalDate, calendarId: String): MutableList<Event>? {

        val service = buildService<Calendar>()
        val TimeMin = localDateTimeToDateTime(date, open)
        val TimeMax = localDateTimeToDateTime(date, close)
        val events = service?.events()?.list(calendarId)?.setTimeMin(TimeMin)?.setTimeMax(TimeMax)?.execute()
        return events?.items
    }

    fun getOneEvent(date: LocalDate, calendarId: String, start: LocalTime, end: LocalTime): MutableList<Event>? {
        val service = buildService<Calendar>()
        val TimeMin = localDateTimeToDateTime(date, start)
        val TimeMax = localDateTimeToDateTime(date, start.plusHours(range.toLong()))
        val events = service?.events()?.list(calendarId)?.setTimeMax(TimeMax)?.setTimeMin(TimeMin)?.execute()?.items
        return events
    }

    fun localDateTimeToDateTime(date: LocalDate, time: LocalTime): DateTime {
        val atZone = ZoneId.of(timezone)
        val dateTime = LocalDateTime.of(date, time).atZone(atZone).toInstant().toEpochMilli()
        return DateTime(dateTime)
    }

    fun convertFromDateTime(dateTime: DateTime): LocalTime {
        val dT = dateTime.value
        val localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(dT), ZoneId.of(timezone))
        return localDateTime.toLocalTime()
    }


    companion object : ExtensionBuilder<IReservation> {
        val logger = LoggerFactory.getLogger(ReservationProvider::class.java)
        const val CLIENT_SECRET = "client_secret"
        const val CALENDAR_ID = "calendar_id"
        const val DELEGATED_USER = "delegated_user"
        const val OPEN_HOUR = "open_hour"
        const val CLOSE_HOUR = "close_hour"
        const val TIMERANGE = "time_range"
        const val DAYRANGE = "day_range"
        const val TIMEZONE = "time_zone"
        override fun invoke(config: Configuration): IReservation {
            return ReservationProvider(config)
        }
    }
}



