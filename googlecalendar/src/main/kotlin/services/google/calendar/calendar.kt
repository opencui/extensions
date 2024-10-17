package services.google.calendar


import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.*
import com.google.api.services.directory.Directory
import com.google.api.services.directory.DirectoryScopes
import com.google.api.services.directory.model.CalendarResource
import io.opencui.core.*
import io.opencui.serialization.Json
import io.opencui.serialization.JsonObject
import io.opencui.sessionmanager.ChatbotLoader
import org.slf4j.LoggerFactory
import services.opencui.hours.BusinessHours
import services.opencui.hours.TimeInterval
import services.opencui.reservation.*
import java.time.*
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.log


/**
 * We need to convert local data time to Google DateTime, and we assume
 * Datetime is always timeZoned. but local data and time are always related
 * to that timeZone, which controlled by what is on location and resource.
 * For Google calendar provider, we assume the userId will be user email.
 */
fun LocalDateTime.toDateTime(zoneId: ZoneId): DateTime {
    val dateTime = ZonedDateTime.of(this, zoneId)
    val offset = zoneId.rules.getOffset(LocalDateTime.now()).totalSeconds
    return DateTime(dateTime.toInstant().toEpochMilli(), offset.div(60))
}

fun DateTime.toLocalDateTime(): LocalDateTime {
    return LocalDateTime.parse(this.toStringRfc3339(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}

fun DateTime.toOffsetDateTime(): OffsetDateTime {
    println("Input DateTime: ${this.toStringRfc3339()}")
    return OffsetDateTime.parse(this.toStringRfc3339(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}

fun DateTime.toLocalTime(): LocalTime {
    return toLocalDateTime().toLocalTime()
}

fun DateTime.toLocalDate(): LocalDate {
    return toLocalDateTime().toLocalDate()
}

fun Resource.update(presource: CalendarResource) {
    id = presource.resourceId
    type = ResourceType(presource.resourceType)
    name = ResourceName(presource.resourceName)
}

fun timeInterval(stt: LocalDateTime, end: LocalDateTime): TimeInterval {
    val result = TimeInterval()
    result.startTime = stt.toLocalTime()
    result.endTime = end.toLocalTime()
    return result
}

data class ReservationProvider(
    val config: Configuration,
    override var session: UserSession? = null,
) : IReservation, IProvider {



    // This for the business
    private val businessName = config[CUSTOMERNAME]!! as String

    @Transient private val HTTP_TRANSPORT: NetHttpTransport = GoogleNetHttpTransport.newTrustedTransport()
    @Transient private val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()

    private val delegatedUser = (config[DELEGATED_USER] as String?) ?: "primary"
    val reservationCalendarId = (config[RESERVATION_ID] as String?)!!
    private val freeBusyCalendarId = (config[FREEBUSY_ID] as String?)!!
    @Transient private val client = buildClient()
    @Transient val admin = buildAdmin()
    
    private val zoneId: ZoneId
        get() = ZoneId.of((client!!.calendarList().get("primary").execute() as CalendarListEntry).timeZone)

    override fun cloneForSession(userSession: UserSession): IExtension {
        return this.copy(session = userSession)
    }

    private fun getCredential() : GoogleCredential {
        // We support two different authentication now.
        // 1. secrets_json
        // 2. client id, secret, access refresh token.
        val accessToken = config[ACCESSTOKEN] as String?
        return if (accessToken == null || accessToken == "") {
            val secrets_json = config[CLIENT_SECRET] as String
            GoogleCredential.fromStream(secrets_json.byteInputStream(), HTTP_TRANSPORT, JSON_FACTORY)
                .createScoped(listOf(DirectoryScopes.ADMIN_DIRECTORY_RESOURCE_CALENDAR, CalendarScopes.CALENDAR))
                .createDelegated(delegatedUser)
        } else {
            val refreshToken = config[REFRESHTOKEN]!! as String
            val rootClientSecret = config[ROOT_CLIENT_SECRET]!! as String
            val rootClientId = config[ROOT_CLIENT_ID]!! as String
            val credentials = GoogleCredential.Builder()
                .setClientSecrets(rootClientId, rootClientSecret)
                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(JacksonFactory.getDefaultInstance())
                .build()
                .createScoped(listOf(DirectoryScopes.ADMIN_DIRECTORY_RESOURCE_CALENDAR, CalendarScopes.CALENDAR))
                .setAccessToken(accessToken as String)
                .setRefreshToken(refreshToken)

            // Forget refresh token.
            credentials.refreshToken()

            credentials
        }
    }

    /**
     *This function returns a Calendar API client object that is authorized to make requests on behalf
     *  of the delegated user.
     * */
    private fun buildClient(): Calendar? {
        val credential = getCredential()
        return Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName("Calendar API").build()
    }

    /**
     * This function creates a Google Directory API client using the provided credentials and scopes,
     * and sets the delegated user to be used when making requests.
     * https://developers.google.com/admin-sdk/directory/v1/api-lib/java
     * */
    private fun buildAdmin(): Directory? {
        val credential = getCredential()
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
        duration: Int,
        presource: Resource,
        date: LocalDate?,
        time: LocalTime?,
        title: String?,
        userName: PersonName?,
    ): Reservation? {
        val timeZone = presource.timezone!!
        val resource = getCalendarResource(presource.id!!)!!
        val service = client
        val event = Event()
        event.summary = title ?: "$userName and $businessName"
        event.description = "${resource.resourceName} is booked at $businessName"
        val startTime = date!!.atTime(time!!).toDateTime(timeZone)
        val endTime = date.atTime(time).plusSeconds(duration.toLong()).toDateTime(timeZone)
        event.start = EventDateTime().setDateTime(startTime)
        event.end = EventDateTime().setDateTime(endTime)

        //https://developers.google.com/calendar/api/concepts/sharing
        event.attendees = listOf(
            EventAttendee()  // This is for resource.
                .setResource(true)
                .setEmail(resource.resourceEmail)
                .setDisplayName(resource.resourceName)
                .setId(resource.resourceId),
            EventAttendee()   // this is for end user.
                .setEmail(userId)
                .setDisplayName(userName?.toString())
                .setId(userId)
        )

        val createdEvent = service?.events()?.insert(reservationCalendarId, event)?.execute()

        if (createdEvent == null) {
            logger.info("Could not create event!!!")
            return null
        }

        val reservation = Reservation(null)
        reservation.id = createdEvent.id
        reservation.end = endTime.toOffsetDateTime()
        reservation.userId = userId
        reservation.resourceId = resource.resourceId
        reservation.start = startTime.toOffsetDateTime()
        
        return reservation
    }

    private fun getReservationKey(userId: String): String {
        return "$userId:googlecalendar:reservation"
    }

    // For assume the caching is the provider's responsibility. This will simplify
    // how it is used, because implementation knows whether something need to be cached.
    override fun listReservation(userId: String, location: Location?, resourceType: ResourceType?): List<Reservation> {
        return cachedListReservation(userId, location, resourceType)
    }

    @Transient val cachedListReservation = CachedMethod3(this::listReservationImpl)


    fun listReservationImpl(userId: String, location: Location?, resourceType: ResourceType?): List<Reservation> {
        logger.info("ListReservation for ${userId}, $location and $resourceType")

        val reservations = mutableListOf<Reservation>()
        val now = DateTime(ZonedDateTime.now(zoneId).toInstant().toEpochMilli())
        var pageToken: String? = null

        val criteria = mutableListOf<String>()
        criteria.add(userId)
        if (location != null) {criteria.add(location.toString())}
        if (resourceType != null) {criteria.add(resourceType.toString())}
        val query = criteria.joinToString(",")

        // use pagetoken to get all the events.
        do {
            val events = client?.events()?.list(reservationCalendarId)
                ?.setTimeMin(now)
                ?.setPageToken(pageToken)
                ?.setQ(query)?.execute()

            if (events.isNullOrEmpty()) {
                pageToken = null
            } else {
                logger.info("got ${events.items.size} events for $query")
                for (event in events.items) {
                    val reservation = Reservation()
                    reservation.id = event.id
                    reservation.userId = userId
                    reservation.resourceName = event.attendees.firstOrNull { it.isResource }!!.displayName
                    reservation.end = event.end.dateTime.toOffsetDateTime()
                    reservation.start = event.start.dateTime.toOffsetDateTime()
                    reservation.resourceId = event.attendees.firstOrNull { it.isResource }!!.id
                    // remember to add the list.
                    reservations.add(reservation)
                }
                pageToken = events.nextPageToken
            }
        } while(pageToken != null)

        reservations.map{ it.session = session }
        return reservations.sortedBy {  it.start }
    }

    /**
     * This is the implementation of a method named cancelReservation that takes in a Location
     * object and a Reservation object as parameters. It cancels the reservation associated with
     * the provided reservation object and returns a ValidationResult object with a success flag
     * and a message indicating whether the operation was successful.
     * The method also logs the cancellation and sets the time zone for the location.
     * https://developers.google.com/calendar/api/v3/reference/events/delete?hl=en
     * */
    override fun cancelReservation(reservation: Reservation): ValidationResult {
        reservation.session = null
        client?.events()?.delete(reservationCalendarId, reservation.id)?.execute()
        return ValidationResult().apply { success = true; message = "reservation canceled" }
    }

    /**
     * https://developers.google.com/admin-sdk/directory/reference/rest/v1/resources.calendars/get
     * */
    private fun getCalendarResource(resourceId: String): CalendarResource? {
        return admin?.resources()?.calendars()?.get(businessName, resourceId)?.execute()
    }

    /**
     * The function resourceAvailable checks the availability of a resource of a specific type at a
     * given time and location. It first checks if the requested time is in the future.
     * Then, it filters the resources based on the provided filter, if any, and availability for
     * the requested date and time.
     * Finally, it returns a ValidationResult indicating whether the resource is available or not.
     * */
    override fun resourceAvailable(
        date: LocalDate?,
        time: LocalTime?,
        duration: Int,
        presource: Resource
    ): ValidationResult {
        val zoneId = presource.timezone!!
        val now = LocalDateTime.now(zoneId)
        val resource = getCalendarResource(presource.id!!)

        // Make sure time it not passed in both datetime, and date.
        if (time != null && date?.atTime(time)?.isBefore(now) == true) {
            return ValidationResult(session).apply {
                success = false;
                message = TimePassed
            }
        }

        if (date?.isBefore(now.toLocalDate()) == true) {
            return ValidationResult(session).apply {
                success = false;
                message = TimePassed
            }
        }

        val available =  freeBusyRequestByDate(zoneId, date!!, listOf(resource!!.resourceEmail))[0]
        if (available.isEmpty()) {
            return  ValidationResult(session).apply {
                success = false;
                message = NotAvailable
            }
        }

        logger.debug("Resource after filter of date is $resource")
        return if(checkSlotAvailability(zoneId, date!!, time!!, resource.resourceEmail, duration)){
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
        reservation: Reservation,
        date: LocalDate?,
        time: LocalTime?,
        duration: Int,
        resource: Resource
    ): ValidationResult {
        val resourceEmail = getCalendarResource(reservation.resourceId!!)!!.resourceEmail
        val validationResult = ValidationResult()

        val event = client?.events()?.get(getCalendarResource(reservation.resourceId!!)?.resourceEmail, reservation.id)?.execute()

        return if (event.isNullOrEmpty()) {
            validationResult.apply {
                success = false
                message = "Reservation not found"
            }
        } else {
            validationResult.apply {
                success = checkSlotAvailability(resource.timezone!!, date!!, time!!, resourceEmail, duration)
                message = if (success as Boolean) "Reservation can be updated" else "Reservation cannot be updated"
            }
        }
    }

    override fun updateReservation(
        reservation: Reservation,
        date: LocalDate?,
        time: LocalTime?,
        duration: Int,
        resource: Resource
    ): ValidationResult {
        val validationResult = ValidationResult()

        val listResources = mutableListOf<CalendarResource>()

        val resources = admin?.resources()?.calendars()?.list(businessName)?.execute()?.items
        val event = client?.Events()?.get(getCalendarResource(reservation.resourceId!!)?.resourceEmail, reservation.id)?.execute()
        if (event.isNullOrEmpty()) {
            validationResult.message = NotAvailable
            validationResult.success = false
        }
        if (resources.isNullOrEmpty()) {
            validationResult.message = NotAvailable
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
                validationResult.message = NotAvailable
                validationResult.success = false
            } else {
                val calendar = client
                val e = Event()
                e.summary = event?.summary
                e.description = event?.description
                e.start = event?.start
                e.end = event?.end
                e.attendees = event?.attendees
                calendar?.events()?.insert(listResources[0].resourceEmail, event)?.execute()
                validationResult.success = true
                validationResult.message = ResourceUpdated
            }
        }
        return validationResult
    }

    override fun reservationCancelable(reservation: Reservation): ValidationResult {
        val now = Instant.now()
        val event = client?.Events()?.get(getCalendarResource(reservation.resourceId!!)?.resourceEmail, reservation.id)?.execute()
        return if (now.isAfter(Instant.parse(event?.start?.dateTime.toString()))) {
            ValidationResult(false)
        } else {
            ValidationResult(true)
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
        val buildings = admin?.resources()?.Buildings()?.list(businessName)?.execute()?.buildings ?: emptyList()
        logger.debug("The locations are :: $buildings")
        val locations = mutableListOf<Location>()

        for (resource in buildings) {
            val location = Location(session)
            location.id = resource.buildingId
            location.name = LocationName(resource.buildingName)
            location.timezone = ZoneId.of(ObjectMapper().readValue(resource.description, Map::class.java)["timezone"] as String)
            locations.add(location)
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
    fun availableDatesImpl(
        presource: List<Resource>,
        start: Int = 0,
        numOfDays: Int = 7
    ): List<DateAvailability> {
        if (presource.isEmpty()) return emptyList()

        val timezone = presource[0].timezone!!
        val today = LocalDate.now(timezone)

        // TODO: improve this soon.
        val end = start + numOfDays
        val startDate = today.plusDays(start.toLong())
        val endDate = today.plusDays(end.toLong())

        var timeMinimum = startDate.atTime(LocalTime.of(0, 0)).toDateTime(timezone)
        val timeMaximum = endDate.atTime(LocalTime.of(23, 59)).toDateTime(timezone)

        // We should always use LocalDateTime.now(ZoneId.of(timeZone)
        if (startDate == LocalDate.now(timezone)) {
            // For today, we always start from now.
            timeMinimum = LocalDateTime.now(timezone).toDateTime(timezone)
        }

        val resources = presource.map { getCalendarResource(it.id!!)!!.resourceEmail }
        val freeIntervalss = requestFreeBusy(timeMinimum, timeMaximum, timezone, resources)

        val freeIntervals = freeIntervalss.flatten().toSet().toList().sortedBy { it.first }
        val freeByDates = freeIntervals.groupBy { it.first.toLocalDate() }
        return freeByDates.map { DateAvailability(it.key, it.value.map { timeInterval(it.first, it.second) })}
    }

    @Transient val cachedAvailableDates = CachedMethod3(this::availableDatesImpl)

    override fun availableDates(
        startOffset: Int,
        numOfDays: Int,
        resources: List<Resource>,
    ): List<DateAvailability> {
        return cachedAvailableDates(resources, startOffset, numOfDays)
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
    fun availableTimesImpl(
        date: LocalDate?,
        presource: List<Resource>): List<TimeInterval> {
        if (presource.isEmpty()) return emptyList()
        val timeZone = presource[0].timezone!!
        val resources = presource.map { getCalendarResource(it.id!!)!!.resourceEmail }
        val freeSlots = freeBusyRequestByDate(timeZone, date ?: LocalDate.now(timeZone), resources)
        return freeSlots.flatten().toSet().toList().sortedBy { it.first }.map { timeInterval(it.first, it.second) }
    }

    @Transient val cachedAvailableTimes = CachedMethod2(this::availableTimesImpl)

    override fun availableTimes(
        date: LocalDate?,
        resources: List<Resource>): List<TimeInterval> {
        return cachedAvailableTimes(date, resources)
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
    private fun requestFreeBusy(timeMinimum: DateTime, timeMaximum: DateTime, zoneId: ZoneId, calendarIds: List<String>): List<List<Pair<LocalDateTime, LocalDateTime>>> {
        val freeBusyRequest = FreeBusyRequest().apply {
            timeMin = timeMinimum
            timeMax = timeMaximum
            timeZone = zoneId.id
            items = calendarIds.map { FreeBusyRequestItem().apply { id = it } }
        }

        val response = client?.freebusy()?.query(freeBusyRequest)?.execute()
        var currentStart = timeMinimum
        var freeIntervalss = mutableListOf<MutableList<Pair<LocalDateTime, LocalDateTime>>>()
        for (calendarId in calendarIds) {
            val freeIntervals = mutableListOf<Pair<LocalDateTime, LocalDateTime>>()
            val busyIntervals = response?.calendars?.get(calendarId)!!.busy
            logger.info("Free busy request for $calendarIds is $freeBusyRequest")
            logger.info("busyInterval is: $busyIntervals")

            busyIntervals.forEach {
                if (it.start.toLocalDateTime().isAfter(currentStart.toLocalDateTime())) {
                    freeIntervals.add(Pair(currentStart.toLocalDateTime(), it.start.toLocalDateTime()))
                    currentStart = it.end
                }
                if (currentStart.toLocalDateTime().isBefore(it.end.toLocalDateTime())) {
                    currentStart = it.end
                }
            }
            if (currentStart.toLocalTime().isBefore(timeMaximum.toLocalTime())) {
                freeIntervals.add(Pair(currentStart.toLocalDateTime(), timeMaximum.toLocalDateTime()))
            }
            // Now we add result to outer list.
            freeIntervalss.add(freeIntervals)
        }
        return freeIntervalss
    }

    private fun freeBusyRequestByDate(zoneId: ZoneId, date: LocalDate, calendarId: List<String>): List<List<Pair<LocalDateTime, LocalDateTime>>> {
        var timeMinimum = date.atTime(LocalTime.of(0, 0)).toDateTime(zoneId)
        var timeMaximum = date.atTime(LocalTime.of(23, 59)).toDateTime(zoneId)

        // We should always use LocalDateTime.now(ZoneId.of(timeZone)
        if (date == LocalDate.now(zoneId)) {
            // For today, we always start from now.
            timeMinimum = LocalDateTime.now(zoneId).toDateTime(zoneId)
        }

        if (date.atTime(timeMaximum.toLocalTime()).isBefore(LocalDateTime.now(zoneId))) {
            return emptyList()
        }

        val freeIntervalss = requestFreeBusy(timeMinimum, timeMaximum, zoneId, calendarId)
        return freeIntervalss
    }


    override fun getHoursByDay(date: LocalDate): BusinessHours {
        val calendar = client!!.calendars().get(freeBusyCalendarId).execute()
        val timeZone = calendar.timeZone
        val zoneId = ZoneId.of(timeZone)

        val timeMinimum = date.atTime(LocalTime.of(0, 0)).toDateTime(zoneId)
        val timeMaximum = date.atTime(LocalTime.of(23, 59)).toDateTime(zoneId)

        val freeIntervals = requestFreeBusy(timeMinimum, timeMaximum, zoneId, listOf(freeBusyCalendarId))[0]
        val timeIntervals = freeIntervals.map{
            TimeInterval().apply {
                startTime = it.first.toLocalTime()
                endTime = it.second.toLocalTime()
            }
        }
        return BusinessHours().apply {
            this.date = date
            this.openings = timeIntervals.toMutableList()
        }
    }


    override fun getHoursByWeek(): List<BusinessHours> {
        val calendar = client!!.calendars().get(freeBusyCalendarId).execute()
        val timeZone = calendar.timeZone
        val zoneId = ZoneId.of(timeZone)
        val date = LocalDate.now(zoneId)
        
        val timeMinimum = date.atTime(LocalTime.of(0, 0)).toDateTime(zoneId)
        val timeMaximum = date.plusDays(7).atTime(LocalTime.of(23, 59)).toDateTime(zoneId)

        val freeIntervals = requestFreeBusy(timeMinimum, timeMaximum, zoneId, listOf(freeBusyCalendarId))[0]
        val freeIntervalMap = freeIntervals.groupBy{ it.first.toLocalDate() }
        val dates = freeIntervalMap.map {it.key}.toList().sorted()
        val results = mutableListOf<BusinessHours>()
        for (date_ in dates) {
            val timeIntervals = freeIntervalMap[date_]?.map {
                TimeInterval().apply {
                    startTime = it.first.toLocalTime()
                    endTime = it.second.toLocalTime()
                }
            } ?: emptyList()

            results.add(
                BusinessHours().apply {
                    this.date = date_
                    this.openings = timeIntervals.toMutableList()
            })
        }
        return results
    }

    override fun listResource(
        location: Location,
        type: ResourceType,
        date: LocalDate?,
        time: LocalTime?,
        duration: Int
    ): List<Resource> {
        var calendarResources = getResourcesByLocationAndType(location, type)
        if (calendarResources.isNullOrEmpty()) return emptyList()

        val resources = mutableListOf<Resource>()

        if (date != null) {
            calendarResources = calendarResources.filter {
                freeBusyRequestByDate(location.timezone!!, date, listOf(it.resourceEmail))[0].isNotEmpty()
            }
        }
        if (time != null) {
            calendarResources = calendarResources.filter {
                checkSlotAvailability(location.timezone!!, date!!, time, it.resourceEmail,  duration)
            }
        }

        calendarResources.forEach {
            val resource = Json.decodeFromString<Resource>(
                it.resourceDescription, ChatbotLoader.findClassLoader(session!!.botInfo)
            )
            resource.update(it)
            resource.timezone = location.timezone
            // TODO: it might be better to include email as human readable identity.
            resources.add(resource)
        }
        return resources
    }

    override fun getResourceInfo(resourceId: String): Resource? {
        val calendar = admin?.resources()?.calendars()?.get(businessName, resourceId)?.execute() ?: return null
        val resource = Json.decodeFromString<Resource>(
                calendar.resourceDescription, ChatbotLoader.findClassLoader(session!!.botInfo))

        resource?.update(calendar)
        calendar.buildingId
        return resource
    }

    /**
     * This function retrieves the list of calendar resources for a given location and resource type.
     * If no filters are specified, it returns all calendar resources of the given type in the specified location.
     * */
    private fun getResourcesByLocationAndType(location: Location, resourceType: ResourceType): List<CalendarResource>? {
        val resources = admin?.resources()?.calendars()?.list(businessName)?.execute()?.items?.filter {
            it.resourceType == resourceType.value && it.buildingId == location.id
        }
        return resources
    }

    /**
     * This function checks if a time slot is available for a given location, date, time, calendar ID,
     * resource type, and duration by querying the calendar
     * events within the specified time range. It returns a boolean indicating the availability of the time slot.
     * */
    private fun checkSlotAvailability(
        timeZone: ZoneId,
        date: LocalDate,
        time: LocalTime,
        calendarId: String,
        duration: Int
    ): Boolean {
        val client = buildClient()
        val timeMin = date.atTime(time).toDateTime(timeZone)
        val timeMax = date.atTime(time).plusSeconds(duration.toLong())!!.toDateTime(timeZone)

        val events = client?.events()?.list(calendarId)?.setTimeMin(timeMin)?.setTimeMax(timeMax)?.execute()?.items
        return events.isNullOrEmpty()
    }

    /**
     * This function will be triggered by google calendar. The goal of this function should be updating
     * Opening table, and ScheduledNotification table. Both of these can be done by restful.
     */
    public fun handleCancelled() {
        // We only care about cancelled event for now.
        // we need to get an initial sync token somehow.
        val botStore = Dispatcher.sessionManager.botStore!!

        var oldSyncToken = botStore.get(SYNCTOKEN)
        var newSyncToken : String? = null
        var changes: Events? = null

        logger.info("Existing sync token $oldSyncToken")
        try {
            changes = client?.events()?.list(reservationCalendarId)?.setSyncToken(oldSyncToken)?.execute()
            newSyncToken = changes!!.nextPageToken
            logger.info("Happy path: There are ${changes?.items?.size} changes for sync token $oldSyncToken : $newSyncToken")
        } catch (e: Exception) {
            logger.info(e.printStackTrace().toString())
            logger.info("Had to get new token.")
            changes = client?.events()?.list(reservationCalendarId)?.execute()
            newSyncToken = changes!!.nextPageToken
            oldSyncToken = null
            logger.info("Unhappy path: There are ${changes?.items?.size} changes for sync token $oldSyncToken : $newSyncToken")
        }

        // this save the synctoken.
        logger.info("Set new token: $newSyncToken")
        botStore.set(SYNCTOKEN, newSyncToken!!)

        val moduleName = config[InsertOpeningModuleName] as String?
        if (moduleName == null) {
            logger.error("Missing value for $InsertOpeningModuleName")
            return
        }

        val funcName = config[InsertOpeningFuncName] as String?
        if (funcName == null) {
            logger.error("Missing value for $InsertOpeningFuncName")
            return
        }
        
        // Only if we have created baseline. For now, always handle changes.
        if (changes?.items != null) {
            logger.info("Should forward to $moduleName:$funcName")
            val cancelled = mutableListOf<JsonObject>()
            for (item in changes.items) {
                if (item.status != CANCELLED) continue

                // if it is canceled, we need to set up openings and notification.
                val event_id = item.id
                val event = client?.events()?.get(reservationCalendarId, event_id)?.execute() ?: continue

                // now we need to push into openings and notifications.
                val email = event.attendees.firstOrNull { it.isResource }?.email
                if (email == null) {
                    logger.info("Can not find resource email for event $event")
                    continue
                }

                val resources = admin?.resources()?.calendars()?.list(email)?.execute()
                if (resources == null || resources.items.isEmpty()) {
                    logger.info("Can not find resource using email $email")
                    continue
                }

                val openslot = mapOf(
                    "start" to event.start.dateTime.toLocalDateTime(),
                    "end" to event.end.dateTime.toLocalDateTime(),
                    "resourceName" to resources.items[0].resourceName,
                    "resourceType" to resources.items[0].resourceType,
                    "resourceId" to resources.items[0].resourceId,
                    "resourceDescription" to Json.parseToJsonElement(resources.items[0].resourceDescription)
                )
                cancelled.add(Json.encodeToJsonElement(openslot) as JsonObject)
            }

            val sessionManager = Dispatcher.sessionManager
            val botInfo = master()
            val bot = sessionManager.getAgent(botInfo)

            // We expect the function takes a single parameter call slots with List<JsonObject> type.
            bot.executeByInterface(moduleName, funcName, mapOf("slots" to cancelled))
        }
    }

    companion object : ExtensionBuilder {
        val logger = LoggerFactory.getLogger(ReservationProvider::class.java)
        const val CLIENT_SECRET = "client_secret"
        const val DELEGATED_USER = "delegated_user"
        const val FREEBUSY_ID = "freebusy_calendar"
        const val RESERVATION_ID = "reservation_calendar"
        const val CUSTOMERNAME = "customer_name"   // This is for business (customer for platform), not end user.
        const val NotAvailable = "Resource Not Available"
        const val ResourceUpdated = "Resource updated"
        const val TimePassed = "Time Passed"
        const val Available = "Resource Available"
        const val CANCELLED = "cancelled"
        const val SYNCTOKEN = "google_calendar_synctoken"
        const val RESOURCES = "resourses"  // comma separated resource ids.

        const val ACCESSTOKEN = "access_token"
        const val REFRESHTOKEN = "refresh_token"

        const val ROOT_CLIENT_ID = "root_client_id"
        const val ROOT_CLIENT_SECRET = "root_client_secret"

        const val InsertOpeningModuleName = "change_hook_interface"
        const val InsertOpeningFuncName = "change_hook_func"

        override fun invoke(config: Configuration): IReservation {
            return ReservationProvider(config)
        }
    }
}