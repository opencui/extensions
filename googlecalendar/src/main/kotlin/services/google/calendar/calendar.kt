package services.google.calendar


import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
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
import org.slf4j.LoggerFactory
import services.opencui.reservation.*
import java.time.Duration
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

    private val delegatedUser = config[DELEGATED_USER] as String

    private val secrets_json = config[CLIENT_SECRET] as String

    private val customerName = config[CUSTOMERNAME] as String? ?: "my_customer"

    private val HTTP_TRANSPORT: NetHttpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()


    private val client = buildClient()
    private val admin = buildAdmin()

    override fun cloneForSession(userSession: UserSession): IExtension {
        return this.copy(session = userSession)
    }

    /**
     *This function returns a Calendar API client object that is authorized to make requests on behalf
     *  of the delegated user.
     * */
    private fun buildClient(): Calendar? {
        val credential = GoogleCredential.fromStream(secrets_json.byteInputStream(), HTTP_TRANSPORT, JSON_FACTORY)
            .createScoped(listOf(DirectoryScopes.ADMIN_DIRECTORY_RESOURCE_CALENDAR, CalendarScopes.CALENDAR))
            .createDelegated(delegatedUser)

        return Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName("Calendar API").build()
    }

    /**
     * This function creates a Google Directory API client using the provided credentials and scopes,
     * and sets the delegated user to be used when making requests.
     * https://developers.google.com/admin-sdk/directory/v1/api-lib/java
     * */
    private fun buildAdmin(): Directory? {
        val credential = GoogleCredential.fromStream(secrets_json.byteInputStream(), HTTP_TRANSPORT, JSON_FACTORY)
            .createScoped(listOf(DirectoryScopes.ADMIN_DIRECTORY_RESOURCE_CALENDAR, CalendarScopes.CALENDAR))
            .createDelegated(delegatedUser)
        return Directory.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName("Calendar API").build()
    }

    /**
     * The makeReservation function creates a new reservation for a resource. The resource is selected based on the
     * given location, resourceType, and optionally, filter.
     * The function checks the availability of the selected resource for the given date, time, and duration and creates a new reservation if the resource is available.
     * The function returns a Reservation object representing the new reservation, or null if no resource is available or
     * if an error occurs.
     * https://developers.google.com/calendar/api/concepts/events-calendars
     * https://developers.google.com/calendar/api/guides/create-events
     * */
    override fun makeReservation(
        userId: String,
        location: Location,
        resourceType: ResourceType,
        date: LocalDate?,
        time: LocalTime?,
        duration: Int,
        filter: List<SlotValue>?
    ): Reservation? {
        val timeZone = location.timezone!!.id
        val reservation = Reservation(session)
        val listOfResources = mutableListOf<CalendarResource>()
        // First populate the listOfResource.
        if (filter == null) {
            val resources = getResourcesWhenFilterIsNull(location, resourceType)
            resources?.forEach {
                val event = checkSlotAvailability(location, date!!, time!!, it.resourceEmail, resourceType, duration)
                if (event) {
                    listOfResources.add(it)
                }
            }
        } else {
            val resources = getResourcesWhenFilterIsNotNull(location, resourceType, filter)
            resources?.forEach {
                val event = checkSlotAvailability(location, date!!, time!!, it.resourceEmail, resourceType, duration)
                if (event) {
                    listOfResources.add(it)
                }
            }
        }

        // Then create reservation based on the first one.
        if (listOfResources.isNotEmpty()) {
            val calendar = client
            val event = Event()
            val resource = listOfResources[0]
            event.summary = "Reservation for $userId"
            event.description = "Reservation booked for ${resource.resourceName}"
            val startTime = localDateTimeToDateTime(date!!, time!!, timeZone)
            val endTime = localDateTimeToDateTime(date, time.plusSeconds(duration.toLong()), timeZone)
            event.start = EventDateTime().setDateTime(startTime)
            event.end = EventDateTime().setDateTime(endTime)
            println("The start $startTime, endTime $endTime")
            //https://developers.google.com/calendar/api/concepts/sharing
            event.attendees = listOf(
                EventAttendee().setResource(true).setEmail(resource.resourceEmail).setId(resource.resourceId)
            )
            val createdEvent = calendar?.events()?.insert(resource.resourceEmail, event)?.execute()
            reservation.id = createdEvent?.id
            reservation.duration = duration
            reservation.endDate = date
            reservation.endTime = time
            reservation.userId = userId
            reservation.resourceId = resource.resourceId
            reservation.startTime = time
            return reservation
        } else {
            return null
        }
    }

    // For assume the caching is the provider's responsibility. This will simplify
    // how it is used, because implementation knows whether something need to be cached.
    override fun listReservation(userId: String, location: Location, resourceType: ResourceType): List<Reservation> {
        val timeZone = location.timezone!!.id
        return cachedListReservation(userId, timeZone, resourceType)
    }

    val cachedListReservation = CachedMethod3(this::listReservationImpl, values)

    // TODO: Why is we use userId
    fun listReservationImpl(userId: String, timeZone: String, resourceType: ResourceType): List<Reservation> {
        val start = System.currentTimeMillis()
        logger.debug("Entering list Reservation")
        val now = localDateTimeToDateTime(LocalDate.now(), LocalTime.now(), timeZone)
        val reservations = mutableListOf<Reservation>()
        val events = mutableListOf<Event>()
        val admin = admin
        // This is not good at all, we should be list of
        val resources = admin?.resources()?.calendars()?.list(customerName)?.execute()?.items
        resources?.forEach {
            val e = client?.events()?.list(it.resourceEmail)?.setTimeMin(now)?.setQ(userId)?.execute()?.items
            if (e != null) {
                events.addAll(e)
            }
        }

        for (event in events) {
            if (event.summary?.contains(userId) == true) {
                val reservation = Reservation(session).apply {
                    id = event.id
                    this.userId = userId
                    resourceId = event.attendees[0].id
                    startDate = Instant.ofEpochMilli(event.start?.dateTime?.value!!).atZone(ZoneId.of(timeZone))
                        .toLocalDate()
                    endDate =
                        Instant.ofEpochMilli(event.end?.dateTime?.value!!).atZone(ZoneId.of(timeZone)).toLocalDate()
                    startTime = convertFromDateTime(event.start.dateTime, timeZone)
                    endTime =
                        Instant.ofEpochMilli(event.end?.dateTime?.value!!).atZone(ZoneId.of(timeZone)).toLocalTime()
                    duration = (event.end?.dateTime?.value!! - event.start?.dateTime?.value!!).toInt()

                }
                reservations.add(reservation)
            }
        }
        logger.debug("Existing listReservation with ${System.currentTimeMillis() - start}")
        return reservations
    }

    /**
     * This is the implementation of a method named cancelReservation that takes in a Location
     * object and a Reservation object as parameters. It cancels the reservation associated with
     * the provided reservation object and returns a ValidationResult object with a success flag
     * and a message indicating whether the operation was successful.
     * The method also logs the cancellation and sets the time zone for the location.
     * https://developers.google.com/calendar/api/v3/reference/events/delete?hl=en
     * */
    override fun cancelReservation(location: Location, reservation: Reservation): ValidationResult {
        logger.info("cancel Reservation for ${getResource(reservation.resourceId!!)?.resourceEmail} and ${reservation.id}")
        client?.events()?.delete(getResource(reservation.resourceId!!)?.resourceEmail, reservation.id)?.execute()
        return ValidationResult().apply { success = true;message = "reservation canceled" }
    }

    /**
     * https://developers.google.com/admin-sdk/directory/reference/rest/v1/resources.calendars/get
     * */
    private fun getResource(resourceId: String): CalendarResource? {
        return admin?.resources()?.calendars()?.get(customerName, resourceId)?.execute()
    }

    /**
     * The function resourceAvailable checks the availability of a resource of a specific type at a
     * given time and location. It first checks if the requested time is in the future.
     * Then, it filters the resources based on the provided filter, if any, and availability for
     * the requested date and time.
     * Finally, it returns a ValidationResult indicating whether the resource is available or not.
     * */
    override fun resourceAvailable(
        location: Location,
        type: ResourceType,
        date: LocalDate?,
        time: LocalTime?,
        duration: Int,
        filter: List<SlotValue>?
    ): ValidationResult {
        val now = LocalDateTime.now()
        val dateTime = if (time != null) date?.atTime(time) else null
        if (dateTime?.isBefore(now) == true) {
            return ValidationResult(session).apply { success = false; message = NotAvailable }
        }
        var resources =
            if (filter == null) getResourcesWhenFilterIsNull(location, type) else getResourcesWhenFilterIsNotNull(
                location, type, filter
            )
        if (resources.isNullOrEmpty()) {
            return ValidationResult(session).apply { success = false;message = NotAvailable }
        }
        if (date != null) {
            resources = resources.filter {
                !makeFreeBusyRequest(location, date, it.resourceEmail).isNullOrEmpty()
            }

        }
        logger.debug("Resource after filter of date is $resources")
        if (time != null) {
            resources = resources.filter {
                checkSlotAvailability(location, date!!, time, it.resourceEmail, type, duration)
            }
        }
        logger.debug("Resource after filter of date and time is $resources")

        return if (!resources.isNullOrEmpty()) {
            ValidationResult(session).apply {
                success = true
                message = Available
            }
        } else {
            ValidationResult(session).apply {
                success = false
                message = NotAvailable
            }
        }
    }

    /**
     * This function checks whether a reservation can be updated or not by checking the availability
     * of the resource and whether the reservation exists or not. It also checks if any features
     * are requested to be updated, which is not allowed. The function returns a ValidationResult object
     * indicating whether the operation was successful or not with a corresponding message.
     * */
    override fun reservationUpdatable(
        location: Location,
        reservation: Reservation,
        date: LocalDate?,
        time: LocalTime?,
        duration: Int,
        features: List<SlotValue>?
    ): ValidationResult {
        val resourceEmail = getResource(reservation.resourceId!!)!!.resourceEmail
        val type = ResourceType(getResource(reservation.resourceId!!)!!.resourceType)

        val validationResult = ValidationResult()

        val event = client?.events()?.get(getResource(reservation.resourceId!!)?.resourceEmail, reservation.id)?.execute()

        return if (event.isNullOrEmpty()) {
            validationResult.apply {
                success = false
                message = "Reservation not found"
            }
        } else if (features != null) {
            validationResult.apply { success = false; message = "Reservation not updatable" }
        } else {
            validationResult.apply {
                success = checkSlotAvailability(location, date!!, time!!, resourceEmail, type, duration)
                message = if (success as Boolean) "Reservation can be updated" else "Reservation cannot be updated"
            }
        }
    }

    override fun updateReservation(
        location: Location,
        reservation: Reservation,
        date: LocalDate?,
        time: LocalTime?,
        duration: Int,
        features: List<SlotValue>
    ): ValidationResult {
        val validationResult = ValidationResult()
        val listResources = mutableListOf<CalendarResource>()

        val resources = admin?.resources()?.calendars()?.list(customerName)?.execute()?.items
        val event = client?.Events()?.get(getResource(reservation.resourceId!!)?.resourceEmail, reservation.id)?.execute()
        if (event.isNullOrEmpty()) {
            validationResult.message = "cannot update"
            validationResult.success = false
        }
        if (resources.isNullOrEmpty()) {
            validationResult.message = "cannot update"
            validationResult.success = false
        } else {
            resources.forEach {
                val events = client?.events()?.list(it.resourceEmail)?.setTimeMax(event?.start?.dateTime)
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

    override fun reservationCancelable(location: Location, reservation: Reservation): ValidationResult {
        val now = Instant.now()
        val event = client?.Events()?.get(getResource(reservation.resourceId!!)?.resourceEmail, reservation.id)?.execute()
        return if (now.isAfter(Instant.parse(event?.start?.dateTime.toString()))) {
            val result = ValidationResult()
            result.success = false
            result.message = "Cannot cancel event"
            result
        } else {
            val result = ValidationResult()
            result.success = true
            result.message = "Reservation can be cancelled"
            result
        }
    }

    /**
     *This function retrieves a list of locations from an admin(google)
     * and then creates a list of Location objects from the retrieved data.
     * The function loops through the retrieved buildings, extracts the relevant
     * data for each building, and creates a Location object with that data.
     * The resulting list of Location objects is returned.
     * */
    override fun listLocation(): List<Location> {
        val resources = admin?.resources()?.Buildings()?.list(customerName)?.execute()?.buildings
        logger.debug("The resoures are :: $resources")
        val locations = mutableListOf<Location>()
        if (resources != null) {
            for (resource in resources) {
                val location = Location(session)
                location.id = resource.buildingId
                location.name = LocationName(resource.buildingName)
                location.timezone = ZoneId.of(ObjectMapper().readValue(resource.description, Map::class.java)["timezone"] as String)
                locations.add(location)
            }
        }
        return locations
    }

    /**
     * This function returns a list of available dates for a given location and resource,
     * based on whether the user has specified a time or not. If a time is specified,
     * the function will check if the given time is available on each date, and if it is,
     * it will add that date to the list of available dates. If no time is specified, the function
     * will check if there are any available time slots on each day within a range of five days,
     * and if there are, it will add that date to the list of available dates. The filter parameter
     * is used to filter resources based on specific criteria, if it is not null.
     * */
    override fun availableDates(
        location: Location,
        resourceType: ResourceType,
        time: LocalTime?,
        duration: Int,
        filter: List<SlotValue>?
    ): List<LocalDate> {
        val availableDates = mutableListOf<LocalDate>()
        val now = LocalDate.now()
        val resources = if (filter == null) getResourcesWhenFilterIsNull(
            location, resourceType
        ) else getResourcesWhenFilterIsNotNull(
            location, resourceType, filter
        )
        val range = 0..5

        resources?.forEach {

            if (time == null) {
                range.forEach { i ->
                    val events = availableTimes(location, resourceType, now.plusDays(i.toLong()), duration, filter)
                    if (events.isNotEmpty() && !availableDates.contains(now.plusDays(i.toLong()))) {
                        availableDates.add(now.plusDays(i.toLong()))
                    }
                }
            } else {
                range.forEach { i ->
                    val event = checkSlotAvailability(location, now, time, it.resourceEmail, resourceType, duration)
                    if (event) availableDates.add(now.plusDays(i.toLong()))
                }
            }
        }
        return availableDates
    }

    /**
     * This function is used to find available time slots for a specific resource type at a given location
     * on a particular day. It takes in several parameters including the location object, resource type, date,
     * duration of the meeting, and filter values. The function first sets the time zone of the location and retrieves
     * the resources that match the specified filter or all resources if no filter is given.
     *
     * If there are no resources found, the function returns an empty list.
     * Otherwise, it calls the makeFreeBusyRequest function for each resource to get the
     * free/busy information for the specified date. It then flattens the list of free times for all
     * resources and removes any duplicates before returning the sorted list of available times. The returned
     * times are in the LocalTime format.
     * */
    override fun availableTimes(
        location: Location,
        resourceType: ResourceType,
        date: LocalDate?,
        duration: Int,
        filter: List<SlotValue>?
    ): List<LocalTime> {
        val resources = when {
            filter == null -> getResourcesWhenFilterIsNull(location, resourceType)
            else -> getResourcesWhenFilterIsNotNull(location, resourceType, filter)
        }

        if (resources.isNullOrEmpty()) return emptyList()

        return resources.flatMap { makeFreeBusyRequest(location, date ?: LocalDate.now(), it.resourceEmail) }.distinct()
            .sorted()
    }

    /**
     * This function takes in a Location, LocalDate, and calendarId and returns a list of free time
     * slots on that date for the given calendar. It uses the Google Calendar API to make a free-busy
     * request to determine when the calendar is busy and then calculates the free time slots in between
     * those busy intervals. The free time slots are returned as a list of LocalTime objects. The function
     * also handles some edge cases, such as starting the search from the current time if the search date
     * is today and not returning any free time slots if the search date is in the past
     * https://developers.google.com/calendar/api/v3/reference/freebusy
     * */
    private fun makeFreeBusyRequest(location: Location, date: LocalDate, calendarId: String): MutableList<LocalTime> {
        val timeZone = location.timezone!!.id
        val freeRanges = mutableListOf<LocalTime>()

        var timeMinimum = localDateTimeToDateTime(date, LocalTime.of(0, 0), timeZone)
        var timeMaximum = localDateTimeToDateTime(date, LocalTime.of(23, 59), timeZone)

        val today = LocalDate.now().atTime(LocalTime.now())
        if (date == LocalDate.now()) {
            // For today, we always start from now.
            timeMinimum = localDateTimeToDateTime(date, LocalTime.now(), timeZone)
        }

        if (date.atTime(convertFromDateTime(timeMaximum, timeZone)).isBefore(today)) {
            return freeRanges
        }

        val freeBusyRequest = FreeBusyRequest().apply {
            timeMin = timeMinimum
            timeMax = timeMaximum
            items = listOf(FreeBusyRequestItem().apply {
                id = calendarId
            })
        }
        logger.debug("Free busy request for $calendarId is $freeBusyRequest")

        var localTimesPair = mutableListOf<Pair<LocalTime, LocalTime>>()

        val response = client?.freebusy()?.query(freeBusyRequest)?.execute()
        var currentStart = timeMinimum
        val busyIntervals = response?.calendars?.get(calendarId)!!.busy
        busyIntervals.forEach {
            if (convertFromDateTime(it.start, timeZone).isAfter(convertFromDateTime(currentStart, timeZone))) {
                localTimesPair.add(Pair(convertFromDateTime(currentStart, timeZone), convertFromDateTime(it.start, timeZone)))
            }
            currentStart = it.end
        }
        if (convertFromDateTime(currentStart, timeZone).isBefore(convertFromDateTime(timeMaximum, timeZone))) {
            localTimesPair.add(Pair(convertFromDateTime(currentStart, timeZone), convertFromDateTime(timeMaximum, timeZone)))
        }
        localTimesPair.forEach {
            freeRanges.add(it.first)
        }
        return freeRanges
    }

    override fun listResource(
        location: Location,
        type: ResourceType,
        date: LocalDate?,
        time: LocalTime?,
        duration: Int,
        filter: List<SlotValue>?
    ): List<Resource> {
        var calendarResources =
            if (filter == null) getResourcesWhenFilterIsNull(location, type) else getResourcesWhenFilterIsNotNull(
                location, type, filter
            )

        val resources = mutableListOf<Resource>()
        if (calendarResources.isNullOrEmpty()) {
            return resources
        }
        if (date != null) {
            calendarResources = calendarResources.filter {
                !makeFreeBusyRequest(location, date, it.resourceEmail).isNullOrEmpty()
            }
        }
        if (time != null) {
            calendarResources = calendarResources.filter {
                checkSlotAvailability(location, date!!, time, it.resourceEmail, type, duration)
            }
        }
        calendarResources.forEach {
            val resource = Json.decodeFromString<Resource>(
                it.resourceDescription, ChatbotLoader.findClassLoader(session!!.botInfo)
            )
            resources.add(resource)
        }
        return resources
    }

    override fun getResourceInfo(resourceId: String): Resource? {
        val calendar = admin?.resources()?.calendars()?.get(customerName, resourceId)?.execute()
        val resource = calendar?.let {
            Json.decodeFromString<Resource>(
                it.resourceDescription, ChatbotLoader.findClassLoader(session!!.botInfo)
            )
        }
        return resource
    }

    /**
     * This function retrieves the list of calendar resources for a given location and resource type.
     * If no filters are specified, it returns all calendar resources of the given type in the specified location.
     * */
    private fun getResourcesWhenFilterIsNull(location: Location, resourceType: ResourceType): List<CalendarResource>? {
        val resources = admin?.resources()?.calendars()?.list(customerName)?.execute()?.items?.filter {
            it.resourceType == resourceType.value
        }
        return resources
    }

    /**
     * This function retrieves the resources for a given location and resource type based
     * on a list of filters, each consisting of a slot and a value. It checks the resource
     * description to see if it contains the slot and value, and adds the resource to a list of matching resources.
     * */
    private fun getResourcesWhenFilterIsNotNull(
        location: Location, resourceType: ResourceType, filter: List<SlotValue>
    ): List<CalendarResource>? {
        val cals = mutableListOf<CalendarResource>()
        val resources = getResourcesWhenFilterIsNull(location, resourceType)

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

    /**
     * This is a function that converts a LocalDateTime object to a DateTime object,
     * which is a class in the Google Calendar API. The function takes a LocalDate and a LocalTime,
     * converts them to a ZonedDateTime object with the timezone defined in the class property timeZone,
     * and finally creates a DateTime object using the toInstant() method of ZonedDateTime and the timezone
     * offset.
     * */
    private fun localDateTimeToDateTime(date: LocalDate, time: LocalTime, timeZone: String): DateTime {
        val zoneId = ZoneId.of(timeZone)
        val dateTime = ZonedDateTime.of(date, time, zoneId)
        val offset = zoneId.rules.getOffset(Instant.now()).totalSeconds
        logger.debug("date time : ${DateTime(dateTime.toInstant().toEpochMilli(), (offset.toDouble() / 60).toInt())}")
        return DateTime(dateTime.toInstant().toEpochMilli(), (offset.toDouble() / 60).toInt())
    }

    /**
     * This function converts a DateTime object to a LocalTime object in a specified time zone.
     * */
    private fun convertFromDateTime(dateTime: DateTime, timeZone: String): LocalTime {
        val dT = dateTime.value
        val zoneId = ZoneId.of(timeZone)
        val localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(dT), zoneId)
        return localDateTime.toLocalTime()
    }

    /**
     * This function checks if a time slot is available for a given location, date, time, calendar ID,
     * resource type, and duration by querying the calendar
     * events within the specified time range. It returns a boolean indicating the availability of the time slot.
     * */
    private fun checkSlotAvailability(
        location: Location, date: LocalDate, time: LocalTime, calendarId: String, resourceType: ResourceType, duration: Int
    ): Boolean {
        val client = buildClient()
        val timeZone = location.timezone!!.id

        val timeMin = localDateTimeToDateTime(date, time, timeZone)
        val timeMax = localDateTimeToDateTime(date, time.plusSeconds(duration.toLong())!!, timeZone)

        val events = client?.events()?.list(calendarId)?.setTimeMin(timeMin)?.setTimeMax(timeMax)?.execute()?.items
        return events.isNullOrEmpty()
    }

    companion object : ExtensionBuilder<IReservation> {
        val logger = LoggerFactory.getLogger(ReservationProvider::class.java)
        const val CLIENT_SECRET = "client_secret"
        const val DELEGATED_USER = "delegated_user"
        const val CUSTOMERNAME = "customer_name"
        const val NotAvailable = "Resource Not Available"
        const val Available = "Resource Available"
        override fun invoke(config: Configuration): IReservation {
            return ReservationProvider(config)
        }

        private val values = mutableMapOf<Triple<String, String, ResourceType>, Pair<List<Reservation>, LocalDateTime>>()
    }
}



