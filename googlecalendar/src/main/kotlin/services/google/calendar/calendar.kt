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
import services.opencui.reservation.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
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

    val openHour = config[OPEN_HOUR].toString().toInt()

    val closeHour = config[CLOSE_HOUR].toString().toInt()

    val open = LocalTime.of(openHour, 0)

    val close = LocalTime.of(closeHour, 0)

    val range = config[TIMERANGE].toString().toInt()

    val dayRange = config[DAYRANGE].toString().toInt()

    val secrets_json = config[CLIENT_SECRET] as String

    val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
    val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()


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
                    event.start =
                        EventDateTime().setDateTime(DateTime(date.toString() + "T" + time.toString() + ":00+00:00"))
                    event.end = EventDateTime().setDateTime(
                        DateTime(
                            date.toString() + "T" + time?.plusHours(1).toString() + ":00+00:00"
                        )
                    )
                    event.attendees = listOf(EventAttendee().setResource(true).setEmail(resource.resourceEmail))
                    val createdEvent = calendar?.events()?.insert(calendarId, event)?.execute()
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
        } else {
            val resources = getResourcesWhenFilterIsNotNull(resourceType, filter)
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
                event.start =
                    EventDateTime().setDateTime(DateTime(date.toString() + "T" + time.toString() + ":00+00:00"))
                event.end = EventDateTime().setDateTime(
                    DateTime(
                        date.toString() + "T" + time?.plusHours(1).toString() + ":00+00:00"
                    )
                )
                event.attendees = listOf(EventAttendee().setResource(true).setEmail(resource.resourceEmail))
                val createdEvent = calendar?.events()?.insert(calendarId, event)?.execute()
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
        return reservation
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
                reservation.id = event.id
                reservation.userId = userId
                reservation.resourceId = event.attendees?.get(0)?.email
                reservation.startDate =
                    Instant.ofEpochMilli(event.start?.dateTime?.value!!).atZone(ZoneId.systemDefault())
                        .toLocalDate()
                reservation.duration = range
                reservation.endTime =
                    Instant.ofEpochMilli(event.start?.dateTime?.value!!).atZone(ZoneId.systemDefault())
                        .toLocalTime()
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
        val first = availableDates(type,time, filter)
        if (first.isNullOrEmpty()) {
            val validationResult = ValidationResult(session = null)
            validationResult.success = true
            validationResult.message = "Resources available"
            return validationResult

        } else {
            val validationResult = ValidationResult(session = null)
            validationResult.success = false
            validationResult.message = "No resources available"
            return validationResult

        }

    }

    override fun reservationUpdatable(
        reservationId: String, date: LocalDate, time: LocalTime, features: List<ResourceFeature>?
    ): ValidationResult {
        val validationResult = ValidationResult()
        if(features == null){
            val resources = getResourcesWhenFilterIsNull(ResourceType("table"))
            if(resources.isNullOrEmpty()){
                validationResult.message="not updatable"
                validationResult.success=false
            }else{
                resources.forEach{
                    val service = buildService<Calendar>()
                    val event = service?.events()?.get(it.resourceEmail,reservationId)
                    if(event.isNullOrEmpty()){
                        validationResult.message="reservation is not updatable"
                        validationResult.success=false
                    }else{
                        validationResult.message="reservation is not updatable"
                        validationResult.success=false
                    }
                }
            }
        }else{
            val resources = getResourcesWhenFilterIsNotNull(ResourceType("table"), features)
            if(resources.isNullOrEmpty()){
                validationResult.message="not updatable"
                validationResult.success=false
            }else{
                resources.forEach{
                    val service = buildService<Calendar>()
                    val event = service?.events()?.get(it.resourceEmail,reservationId)
                    if(event.isNullOrEmpty()){
                        validationResult.message="reservation is not updatable"
                        validationResult.success=false
                    }else{
                        validationResult.message="reservation is not updatable"
                        validationResult.success=false
                    }
                }
            }

        }
        return validationResult

    }

    override fun updateReservation(
        reservationId: String, date: LocalDate?, time: LocalTime?, features: List<ResourceFeature>
    ): ValidationResult {
        val calendar = buildService<Calendar>()
        val validationResult = ValidationResult()
        val listResources = mutableListOf<CalendarResource>()
        if(features == null){
            val resources = getResourcesWhenFilterIsNull(resourceType = ResourceType("table"))
            val event = calendar?.Events()?.get(calendarId, reservationId)?.execute()
            if(event.isNullOrEmpty()){
                validationResult.message ="cannot update"
                validationResult.success=false
            }
            if(resources.isNullOrEmpty()){
                validationResult.message ="cannot update"
                validationResult.success=false
            }else{
                resources.forEach{
                    val service = buildService<Calendar>()
                    val events = service?.events()?.list(calendarId)?.setTimeMax(event?.start?.dateTime)?.setTimeMin(event?.end?.dateTime)?.execute()?.items
                    if(events.isNullOrEmpty()){
                        listResources.add(it)
                    }
                }
                if(listResources.isEmpty()){
                    validationResult.message="cannot update"
                    validationResult.success=false
                }else{
                    val calendar = buildService<Calendar>()
                    val e = Event()
                    val resource = listResources[0]
                    e.summary = event?.summary
                    e.description = event?.description
                    e.start = event?.start
                    e.attendees = event?.attendees
                    val createdEvent = calendar?.events()?.insert(listResources[0].resourceEmail, event)?.execute()
                    validationResult.success=true
                    validationResult.message="updated resource"

                }
            }


        }else{
            val resources =getResourcesWhenFilterIsNotNull(resourceType = ResourceType("table"), features)
            val event = calendar?.Events()?.get(calendarId, reservationId)?.execute()
            if(event.isNullOrEmpty()){
                validationResult.message ="cannot update"
                validationResult.success=false
            }
            if(resources.isNullOrEmpty()){
                validationResult.message ="cannot update"
                validationResult.success=false
            }else{
                resources.forEach{
                    val service = buildService<Calendar>()
                    val events = service?.events()?.list(calendarId)?.setTimeMax(event?.start?.dateTime)?.setTimeMin(event?.end?.dateTime)?.execute()?.items
                    if(events.isNullOrEmpty()){
                        listResources.add(it)
                    }
                }
                if(listResources.isEmpty()){
                    validationResult.message="cannot update"
                    validationResult.success=false
                }else{
                    val calendar = buildService<Calendar>()
                    val e = Event()
                    val resource = listResources[0]
                    e.summary = event?.summary
                    e.description = event?.description
                    e.start = event?.start
                    e.attendees = event?.attendees
                    val createdEvent = calendar?.events()?.insert(listResources[0].resourceEmail, event)?.execute()
                    validationResult.success=true
                    validationResult.message="updated resource"

                }
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
        if (time == null) {
            if (filter == null) {
                val resources = getResourcesWhenFilterIsNull(resourceType)
                resources?.forEach { cal ->
                    for (i in 0..dayRange) {
                        val events = getAllEventsOn(now, cal.resourceEmail)
                        val noOfEvents = (closeHour - openHour) / range
                        if (events?.size!! < noOfEvents) {
                            if (availableDates.contains(now.plusDays(i.toLong()))) {
                            } else {
                                availableDates.add(now.plusDays(i.toLong()))
                            }
                        }

                    }
                }

            } else {
                val resources = getResourcesWhenFilterIsNotNull(resourceType, filter)
                resources.forEach { cal ->

                    for (i in 0..dayRange) {
                        val events = getAllEventsOn(now, cal.resourceEmail)

                        val noOfEvents = (closeHour - openHour) / range

                        if (events?.size!! < noOfEvents) {
                            if (availableDates.contains(now.plusDays(i.toLong()))) {
                            } else {
                                availableDates.add(now.plusDays(i.toLong()))
                            }
                        }

                    }
                }
            }
        } else {
            if (filter == null) {
                val resource = getResourcesWhenFilterIsNull(resourceType)
                resource?.forEach {
                    for (i in 0..dayRange) {
                        val events = getOneEvent(now, it.resourceEmail, time, time.plusHours(range.toLong()))
                        if (events?.isEmpty() == true) {
                            availableDates.add(now.plusDays(i.toLong()))
                        }
                    }

                }

            }else{
                val resource = getResourcesWhenFilterIsNotNull(resourceType, filter)
                resource.forEach {
                    for (i in 0..dayRange) {
                        val events = getOneEvent(now, it.resourceEmail, time, time.plusHours(range.toLong()))
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
        resourceType: ResourceType, date: LocalDate?, filter: List<ResourceFeature>?
    ): List<TimeRange> {
        val timeRanges = mutableListOf<TimeRange>()
        if (date == null) {
            val availableTime = TimeRange()
            availableTime.startTime = open
            availableTime.endTime = close
        } else {
            if (filter == null) {
                val resources = getResourcesWhenFilterIsNull(resourceType)
                resources?.forEach {
                    val slots = getTimeRangesOneCanBook()
                    if (slots.isNotEmpty()) {
                        slots.forEach { slot ->
                            val event = getOneEvent(date, it.resourceEmail, slot.startTime!!, slot.endTime!!)
                            if (event?.isEmpty() == true) {
                                timeRanges.add(slot)
                            }
                        }
                    }
                }
            } else {
                val resources = getResourcesWhenFilterIsNotNull(resourceType, filter)
                resources.forEach {
                    val slots = getTimeRangesOneCanBook()
                    if (slots.isNotEmpty()) {
                        slots.forEach { slot ->
                            val event = getOneEvent(date, it.resourceEmail, slot.startTime!!, slot.endTime!!)
                            if (event?.isEmpty() == true) {
                                timeRanges.add(slot)
                            }

                        }
                    }
                }
            }
//

        }
        return timeRanges

    }

    fun getTimeRangesOneCanBook(): MutableList<TimeRange> {
        val numberOfEvents = (closeHour - openHour) / range
        val startTime = open
        val endTime = open.plusHours(range.toLong())
        val timeRanges = mutableListOf<TimeRange>()
        var i = 0
        for (i in i..numberOfEvents) {
            var timeRange = TimeRange()
            timeRange.startTime = startTime.plusHours(i.toLong())
            timeRange.endTime = endTime.plusHours(i.toLong())
            timeRanges.add(timeRange)
        }

        return timeRanges

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


    fun getResourcesWhenFilterIsNull(resourceType: ResourceType): List<CalendarResource>? {
        val adminService = buildAdminService<Directory>()
        val resources = adminService?.resources()?.calendars()?.list("my_customer")?.execute()?.items?.filter {
            it.resourceType == resourceType.value
        }
        return resources

    }

    fun getResourcesWhenFilterIsNotNull(
        resourceType: ResourceType,
        filter: List<ResourceFeature>?
    ): MutableList<CalendarResource> {
        val resources = getResourcesWhenFilterIsNull(resourceType)
        val filteredResources = mutableListOf<CalendarResource>()
        if (resources.isNullOrEmpty()) {
            resources?.forEach {
                val rf = Json.decodeFromString<ResourceFeature>(it.resourceDescription)
                if (rf.key == filter?.get(0)?.key && rf.value == filter?.get(0)?.value) {
                    filteredResources.add(it)

                }
            }

        }
        return filteredResources
    }

    fun getAllEventsOn(date: LocalDate, calendarId: String): MutableList<Event>? {
        val service = buildService<Calendar>()
        val TimeMin = localDateTimeToDateTime(date, open)
        val TimeMax = localDateTimeToDateTime(date, open)
        val events = service?.events()?.list(calendarId)?.setTimeMax(TimeMax)?.setTimeMin(TimeMin)?.execute()?.items
        return events
    }

    fun getOneEvent(date: LocalDate, calendarId: String, start: LocalTime, end: LocalTime): MutableList<Event>? {
        val service = buildService<Calendar>()
        val TimeMin = localDateTimeToDateTime(date, start)
        val TimeMax = localDateTimeToDateTime(date, start)
        val events = service?.events()?.list(calendarId)?.setTimeMax(TimeMax)?.setTimeMin(TimeMin)?.execute()?.items
        return events

    }

    fun localDateTimeToDateTime(date: LocalDate, time: LocalTime): DateTime {
        return DateTime(date.toString() + "T" + time.toString() + ":00+00:00")
    }


    companion object : ExtensionBuilder<IReservation> {

        const val CLIENT_SECRET = "client_secret"
        const val CALENDAR_ID = "calendar_id"
        const val DELEGATED_USER = "delegated_user"
        const val OPEN_HOUR = "open_hour"
        const val CLOSE_HOUR = "close_hour"
        const val TIMERANGE = "time_range"
        const val DAYRANGE = "day_range"
        override fun invoke(config: Configuration): IReservation {
            return ReservationProvider(config)
        }
    }
}



