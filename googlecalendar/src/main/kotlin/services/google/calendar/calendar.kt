package services.google.calendar


import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.*
import com.google.api.services.directory.Directory
import com.google.api.services.directory.DirectoryScopes
import com.google.api.services.directory.model.CalendarResource
import io.opencui.core.*
import io.opencui.serialization.Json
import io.opencui.sessionmanager.ChatbotLoader
import org.jetbrains.kotlin.psi.psiUtil.replaceFileAnnotationList
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
import java.time.ZonedDateTime


data class ReservationProvider(
    val config: Configuration,
    override var session: UserSession? = null,
) : IReservation, IProvider {

    val delegatedUser = config[DELEGATED_USER] as String

    val calendarId = config[CALENDAR_ID] as String

    val secrets_json = config[CLIENT_SECRET] as String

    val customerName = config[CUSTOMERNAME] as String? ?: "my_customer"

    val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
    val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()


    val client = buildClient()
    val admin = buildAdmin()
    val timeZone = getCalendarSettings()

    override fun cloneForSession(userSession: UserSession): IExtension {
        return this.copy(session = userSession)
    }

    fun buildClient(): Calendar? {
        val credential = GoogleCredential.fromStream(secrets_json.byteInputStream(), HTTP_TRANSPORT, JSON_FACTORY)
            .createScoped(listOf(DirectoryScopes.ADMIN_DIRECTORY_RESOURCE_CALENDAR, CalendarScopes.CALENDAR))
            .createDelegated(delegatedUser)

        return Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName("Calendar API").build()
    }

    fun buildAdmin(): Directory? {
        val credential = GoogleCredential.fromStream(secrets_json.byteInputStream(), HTTP_TRANSPORT, JSON_FACTORY)
            .createScoped(listOf(DirectoryScopes.ADMIN_DIRECTORY_RESOURCE_CALENDAR, CalendarScopes.CALENDAR))
            .createDelegated(delegatedUser)
        return Directory.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName("Calendar API").build()
    }


    override fun makeReservation(
        userId: String, resourceType: ResourceType, date: LocalDate?, time: LocalTime?, filter: List<SlotValue>?
    ): Reservation? {
        val reservation = Reservation(session)
        val listOfResources = mutableListOf<CalendarResource>()
        if (filter == null) {
            val resources = getResourcesWhenFilterIsNull(resourceType)
            if (resources.isNullOrEmpty()) {
                return Reservation(session)
            } else {
                resources.forEach {
                    val event = checkSlotAvailability(date!!, time!!, it.resourceEmail)
                    if (event) {
                        listOfResources.add(it)
                    }
                }
                if (listOfResources.isNotEmpty()) {
                    val calendar = client
                    val event = Event()
                    val resource = listOfResources[0]
                    event.summary = "Reservation for $userId"
                    event.description = "Reservation booked for ${resource.resourceName}"
                    val startTime = localDateTimeToDateTime(date!!, time!!)
                    val endTime = localDateTimeToDateTime(date, time.plusHours(1))
                    event.start = EventDateTime().setDateTime(startTime)

                    event.end = EventDateTime().setDateTime(endTime)
                    println("The start $startTime, endTime $endTime")

                    event.attendees = listOf(
                        EventAttendee().setResource(true).setEmail(resource.resourceEmail)
                            .setDisplayName(resource.resourceId)
                    )
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
                val event = checkSlotAvailability(date!!, time!!, it.resourceEmail)
                if (event) {
                    listOfResources.add(it)
                }
            }
            if (listOfResources.isNotEmpty()) {
                val calendar = client
                val event = Event()
                val resource = listOfResources[0]
                event.summary = "Reservation for $userId"
                event.description = "Reservation booked for ${resource.resourceName}"
                val startTime = localDateTimeToDateTime(date!!, time!!)
                val endTime = localDateTimeToDateTime(date, time.plusHours(1))
                event.start =
                    EventDateTime().setDateTime(startTime)
                event.end = EventDateTime().setDateTime(
                    endTime
                )
                event.attendees = listOf(
                    EventAttendee().setResource(true).setEmail(resource.resourceEmail)
                        .setDisplayName(resource.resourceId)
                )
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
        val now = localDateTimeToDateTime(LocalDate.now(), LocalTime.now())
        val reservations = mutableListOf<Reservation>()

        val events = client?.events()?.list(calendarId)?.setTimeMin(now)?.execute()?.items
        if (events != null) {
            for (event in events) {
                if (event?.summary?.contains(userId) == true) {
                    val reservation = Reservation(session).apply {
                        id = event.id
                        this.userId = userId
                        resourceId = event.attendees?.get(0)?.displayName
                        startDate = Instant.ofEpochMilli(event.start?.dateTime?.value!!)
                            .atZone(ZoneId.of(timeZone))
                            .toLocalDate()
                        duration = 1
                        endDate =
                            Instant.ofEpochMilli(event.end?.dateTime?.value!!).atZone(ZoneId.of(timeZone))
                                .toLocalDate()
                        startTime = convertFromDateTime(event.start.dateTime)
                        endTime =
                            Instant.ofEpochMilli(event.end?.dateTime?.value!!).atZone(ZoneId.of(timeZone))
                                .toLocalTime()
                    }
                    reservations.add(reservation)
                }
            }
        }
        return reservations
    }

    override fun cancelReservation(id: String): ValidationResult {
        client?.events()?.delete(
            calendarId, id
        )?.execute()
        val result = ValidationResult()
        result.success = true
        result.message = "reservation canceled"
        return result
    }

    override fun resourceAvailable(
        type: ResourceType, date: LocalDate?, time: LocalTime?, filter: List<SlotValue>?
    ): ValidationResult {
        val now = LocalDateTime.now()
        val dateTime = if (time != null) date?.atTime(time) else null
        if (dateTime?.isBefore(now) == true) {
            return ValidationResult().apply { success = false; message = NotAvailable }
        }

        val resources = getResources(type, filter)
        logger.debug("The resources are $resources")
        if (resources.isNullOrEmpty()) {
            return ValidationResult().apply {
                message = NotAvailable
                success = false
            }
        }
        val ltime =  time ?: LocalTime.now()
        val ldate = date ?: LocalDate.now()
        return findSlotInResources(resources, ldate, ltime)
    }

    private fun findSlotInResources(
        resources: List<CalendarResource>,
        date: LocalDate,
        time: LocalTime
    ): ValidationResult {
        val events = mutableListOf<String>()
        resources.forEach {
            val event = checkSlotAvailability(date, time, it.resourceEmail)
            if (event) {
                events.add(it.resourceEmail)
            }
        }
        if (events.isNullOrEmpty()) {
            return ValidationResult().apply {
                message = NotAvailable
                success = false
            }
        } else {
            return ValidationResult().apply {
                message = Available
                success = true
            }
        }
    }

    override fun reservationUpdatable(
        reservationId: String, date: LocalDate, time: LocalTime, features: List<SlotValue>?
    ): ValidationResult {
        val validationResult = ValidationResult()
        validationResult.success = false
        validationResult.message = "Reservation not updatable"
        val event = client?.events()?.get(calendarId, reservationId)?.execute()
        val onDay = checkSlotAvailability(date!!, time!!, calendarId)
        if (features == null) {
            if (event.isNullOrEmpty()) {
                validationResult.success = false
                validationResult.message = "Reservation not found"
                return validationResult
            } else {
                if (onDay) {
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
        reservationId: String, date: LocalDate?, time: LocalTime?, features: List<SlotValue>
    ): ValidationResult {
        val validationResult = ValidationResult()

        val listResources = mutableListOf<CalendarResource>()

        val resources = getResourcesWhenFilterIsNotNull(resourceType = ResourceType("table"), features)
        val event = client?.Events()?.get(calendarId, reservationId)?.execute()
        if (event.isNullOrEmpty()) {
            validationResult.message = "cannot update"
            validationResult.success = false
        }
        if (resources.isNullOrEmpty()) {
            validationResult.message = "cannot update"
            validationResult.success = false
        } else {
            resources.forEach {
                val events = client?.events()?.list(calendarId)?.setTimeMax(event?.start?.dateTime)
                    ?.setTimeMin(event?.end?.dateTime)?.execute()?.items
                if (events.isNullOrEmpty()) {
                    listResources.add(it)
                }
            }
            if (listResources.isEmpty()) {
                validationResult.message = "cannot update"
                validationResult.success = false
            } else {
                val calendar = client
                val e = Event()
                e.summary = event?.summary
                e.description = event?.description
                e.start = event?.start
                e.attendees = event?.attendees
                calendar?.events()?.insert(listResources[0].resourceEmail, event)?.execute()
                validationResult.success = true
                validationResult.message = "updated resource"
            }
        }
        return validationResult
    }

    override fun reservationCancelable(id: String): ValidationResult {
        val now = Instant.now()
        val event = client?.Events()?.get(calendarId, id)?.execute()
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
        val resources = admin?.resources()?.Buildings()?.list(customerName!!)?.execute()?.buildings
        val locations = mutableListOf<Location>()
        if (resources != null) {
            for (resource in resources) {
                val location = Location(session)
                location.id = resource.buildingId
                location.name = resource.buildingName
                location.type = ResourceType(resource.description)
                locations.add(location)
            }
        }
        return locations
    }

    override fun pickLocation(location: Location): Boolean {
        val resource = admin?.resources()?.buildings()?.get(customerName, location.toString())?.execute()
        val location = Location(session)
        location.id = resource?.buildingId
        location.name = resource?.buildingName
        location.type = resource?.description?.let { ResourceType(it) }
        return true
    }

    override fun availableDates(
        resourceType: ResourceType, time: LocalTime?, filter: List<SlotValue>?
    ): List<LocalDate> {
        val availableDates = mutableListOf<LocalDate>()
        val now = LocalDate.now()

        //I hard coded this for now but will be moved once a parameter is provided
        if (time == null) {
            for (i in 0..5) {
                val events = availableTimes(resourceType, now.plusDays(i.toLong()), filter)
                if (events.isNotEmpty()) {
                    if (availableDates.contains(now.plusDays(i.toLong()))) {
                    } else {
                        availableDates.add(now.plusDays(i.toLong()))
                    }
                }
            }
        } else {
            val resource = getResources(resourceType, filter)
            resource?.forEach {
                for (i in 0..5) {
                    val event = checkSlotAvailability(now, time, calendarId)
                    if (event) {
                        availableDates.add(now.plusDays(i.toLong()))
                    }
                }
            }
        }
        return availableDates
    }

    override fun availableTimes(
        resourceType: ResourceType, date: LocalDate?, filter: List<SlotValue>?
    ): List<LocalTime> {
        val availableTimesList = mutableListOf<LocalTime>()
        val resources = getResources(resourceType, filter)
        val ldate = date ?: LocalDate.now()
        if (resources.isNullOrEmpty()) {
            return mutableListOf()
        }

        resources.forEach {
            availableTimesList.addAll(makeFreeBusyRequest(ldate, it.resourceEmail))
        }
        return availableTimesList.distinct().sorted()
    }

    fun makeFreeBusyRequest(date: LocalDate, calendarId: String): MutableList<LocalTime> {
        val freeRanges = mutableListOf<LocalTime>()

        var timeMinimum = localDateTimeToDateTime(date, LocalTime.of(0, 0))

        var timeMaximum = localDateTimeToDateTime(date, LocalTime.of(23, 59))
        val today = LocalDate.now().atTime(LocalTime.now())
        if (date == LocalDate.now()) {
            if (LocalTime.now().isAfter(LocalTime.of(0, 0))) {
                logger.debug("Local time is obtained here on current time ${LocalTime.now()}")
                timeMinimum = localDateTimeToDateTime(date, LocalTime.now())
            }
        }
        if (date.atTime(convertFromDateTime(timeMaximum)).isBefore(today)) {
            return freeRanges
        }
        val freeBusyRequest = FreeBusyRequest().apply {
            timeMin = timeMinimum
            timeMax = timeMaximum
            items = listOf(FreeBusyRequestItem().apply {
                id = calendarId
            })
        }
        val service = client
        var localTimesPair = mutableListOf<Pair<LocalTime, LocalTime>>()

        val response = service?.freebusy()?.query(freeBusyRequest)?.execute()
        var currentStart = timeMinimum
        val busyIntervals = response?.calendars?.get(calendarId)!!.busy
        busyIntervals.forEach {
            if (convertFromDateTime(it.start) > convertFromDateTime(currentStart)) {
                localTimesPair.add(Pair(convertFromDateTime(currentStart), convertFromDateTime(it.start)))
            }
            currentStart = it.end

        }
        if (convertFromDateTime(currentStart) < convertFromDateTime(timeMaximum)) {
            localTimesPair.add(Pair(convertFromDateTime(currentStart), convertFromDateTime(timeMaximum)))
        }

        localTimesPair.forEach {
            var startPoint = it.first
            while (startPoint < it.second) {
                freeRanges.add(startPoint)
                startPoint = startPoint.plusHours(1)
            }
        }
        logger.debug("Free ranges on $date is $freeRanges")

        return freeRanges

    }

    override fun getResourceInfo(resourceId: String): Resource? {
        val calendar = admin?.resources()?.calendars()?.get(customerName, resourceId)?.execute()
        val resource = calendar?.let {
            Json.decodeFromString<Resource>(
                it.resourceDescription,
                ChatbotLoader.findClassLoader(session!!.botInfo)
            )
        }
        return resource
    }

    fun getResourcesWhenFilterIsNull(resourceType: ResourceType): List<CalendarResource>? {
        val resources = admin?.resources()?.calendars()?.list(customerName)?.execute()?.items?.filter {
            it.resourceType == resourceType.value
        }
        return resources
    }

    fun getResourcesWhenFilterIsNotNull(
        resourceType: ResourceType, filter: List<SlotValue>
    ): List<CalendarResource>? {
        val cals = mutableListOf<CalendarResource>()
        val resources = getResourcesWhenFilterIsNull(resourceType)

        filter.map { criterion ->
            resources?.forEach {
                val mapper = ObjectMapper()
                val filterItems = mapper.readValue(it.resourceDescription, Map::class.java)
                if (filterItems[criterion.slot] == criterion.value) {
                    cals.add(it)
                }
            }
        }
        return cals
    }
    fun getResources(
        resourceType: ResourceType, filter: List<SlotValue>?
    ): List<CalendarResource>?  {
        return if (filter == null) getResourcesWhenFilterIsNull(resourceType) else getResourcesWhenFilterIsNotNull(resourceType, filter)
    }

    fun localDateTimeToDateTime(date: LocalDate, time: LocalTime): DateTime {
        val zoneId = ZoneId.of(timeZone)
        val dateTime = ZonedDateTime.of(date, time, zoneId)
        val offset = zoneId.rules.getOffset(Instant.now()).totalSeconds
        logger.debug("date time : ${DateTime(dateTime.toInstant().toEpochMilli(), (offset.toDouble() / 60).toInt())}")
        return DateTime(dateTime.toInstant().toEpochMilli(), (offset.toDouble() / 60).toInt())
    }

    fun convertFromDateTime(dateTime: DateTime): LocalTime {
        val dT = dateTime.value
        val zoneId = ZoneId.of(timeZone)
        val localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(dT), zoneId)
        return localDateTime.toLocalTime()
    }

    fun checkSlotAvailability(date: LocalDate, time: LocalTime, calendarId: String): Boolean {
        val client = buildClient()
        val timeMin = localDateTimeToDateTime(date!!, time!!)
        val timeMax = localDateTimeToDateTime(date!!, time.plusHours(1)!!)
        val Events = client?.events()?.list(calendarId)?.setTimeMin(timeMin)?.setTimeMax(timeMax)?.execute()?.items
        return Events.isNullOrEmpty()

    }

    fun getCalendarSettings(): String? {
        val calendar = client
        val settings = calendar?.calendars()?.get(calendarId)?.execute()
        return settings?.timeZone
    }

    companion object : ExtensionBuilder<IReservation> {
        val logger = LoggerFactory.getLogger(ReservationProvider::class.java)
        const val CLIENT_SECRET = "client_secret"
        const val CALENDAR_ID = "calendar_id"
        const val DELEGATED_USER = "delegated_user"
        const val CUSTOMERNAME = "customer_name"
        const val NotAvailable = "Resource Not Available"
        const val Available = "Resource Available"
        override fun invoke(config: Configuration): IReservation {
            return ReservationProvider(config)
        }
    }
}



