package services.opencui.reservation


import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.`annotation`.JsonInclude.Include.NON_NULL
import io.opencui.core.*
import io.opencui.core.Annotation
import io.opencui.core.da.SlotNotifyFailure
import io.opencui.core.da.SlotRequest
import io.opencui.core.da.SlotRequestMore
import io.opencui.serialization.Json
import services.opencui.hours.IHours
import services.opencui.hours.TimeInterval
import java.time.*
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.reflect.KMutableProperty0


public data class ResourceType(
    @get:JsonIgnore
    public override var value: String
) : IEntity {
    public override var origValue: String? = null

    @JsonValue
    public override fun toString(): String = value

    public companion object {
        @JsonIgnore
        public val valueGood: ((String) -> Boolean)? = { true }
    }
}

public data class ResourceName(
    @get:JsonIgnore
    public override var value: String
) : IEntity {
    public override var origValue: String? = null

    @JsonValue
    public override fun toString(): String = value


    public companion object {
        @JsonIgnore
        public val valueGood: ((String) -> Boolean)? = { true }
    }
}

public data class LocationName(
    @get:JsonIgnore
    public override var value: String
) : IEntity {
    public override var origValue: String? = null

    @JsonValue
    public override fun toString(): String = value

    public companion object {
        @JsonIgnore
        public val valueGood: ((String) -> Boolean)? = { true }
    }
}



data class DateAvailability(val date: LocalDate, val slots: List<TimeInterval>)


@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface Resource : IFrame {
    public var id: String?

    public var type: ResourceType?

    public var name: ResourceName?

    public var durations: MutableList<Int>?

    public var timezone: ZoneId?
}

public data class Reservation(
  @JsonSetter(contentNulls=Nulls.SKIP)
  public override var session: UserSession? = null,
) : IFrame {
  @JsonProperty
  public var id: String? = null

  @JsonProperty
  public var resourceId: String? = null

  @JsonProperty
  public var resourceName: String? = null

  @JsonProperty
  public var userId: String? = null

  @JsonProperty
  public var start: OffsetDateTime? = null

  @JsonProperty
  public var end: OffsetDateTime? = null

  @JsonProperty
  public var offset: Int? = null

  @get:JsonIgnore
  public val reservationService: IReservation by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
    session!!.getExtension<IReservation>()!!
  }


  @JsonIgnore
  public fun getResourceInfo(): Resource? {
    return reservationService!!.getResourceInfo(resourceId!!)
  }

  public override fun annotations(path: String): List<Annotation> = when (path) {
    "id" -> listOf(NeverAsk())
    "resourceId" -> listOf(NeverAsk())
    "resourceName" -> listOf(NeverAsk())
    "userId" -> listOf(NeverAsk())
    "start" -> listOf(NeverAsk())
    "end" -> listOf(NeverAsk())
    "offset" -> listOf(NeverAsk())
    else -> listOf()
  }

  public override fun createBuilder(): FillBuilder = object : FillBuilder {
    public var frame: Reservation? = this@Reservation

    public override fun invoke(path: ParamPath): FrameFiller<Reservation> {
      val filler = FrameFiller({::frame}, path)
      filler.addWithPath(EntityFiller({filler.target.get()!!::id}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "kotlin.String")!!) as? kotlin.String})
      filler.addWithPath(EntityFiller({filler.target.get()!!::resourceId}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "kotlin.String")!!) as? kotlin.String})
      filler.addWithPath(EntityFiller({filler.target.get()!!::resourceName}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "kotlin.String")!!) as? kotlin.String})
      filler.addWithPath(EntityFiller({filler.target.get()!!::userId}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "kotlin.String")!!) as? kotlin.String})
      filler.addWithPath(EntityFiller({filler.target.get()!!::start}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "java.time.OffsetDateTime")!!) as?
          java.time.OffsetDateTime})
      filler.addWithPath(EntityFiller({filler.target.get()!!::end}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "java.time.OffsetDateTime")!!) as?
          java.time.OffsetDateTime})
      filler.addWithPath(EntityFiller({filler.target.get()!!::offset}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "kotlin.Int")!!) as? kotlin.Int})
      return filler
    }
  }

  public companion object {
    public val mappings: Map<String, Map<String, String>> = mutableMapOf<String,
        Map<String, String>>()

    public inline fun <reified S : IFrame> from(s: S): Reservation = Json.mappingConvert(s)
  }
}

public data class Location(
  @JsonInclude(NON_NULL)
  public override var session: UserSession? = null
) : IFrame {
  @JsonProperty
  public var id: String? = null

  @JsonProperty
  public var name: LocationName? = null

  @JsonProperty
  public var timezone: ZoneId? = null

  public override fun annotations(path: String): List<Annotation> = when (path) {
    "id" -> listOf(NeverAsk())
    "name" -> listOf(SlotPromptAnnotation(LazyAction{SlotRequest("name",
        "services.opencui.reservation.LocationName", listOf(this), templateOf("restful" to
        Prompts()))}), AlwaysAsk())
    "timezone" -> listOf(NeverAsk())
    else -> listOf()
  }

  public override fun createBuilder(): FillBuilder = object :
      FillBuilder {
    public var frame: Location? = this@Location

    public override fun invoke(path: ParamPath): FrameFiller<Location> {
      val filler = FrameFiller({::frame}, path)
      filler.addWithPath(EntityFiller({filler.target.get()!!::id}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "kotlin.String")!!) as? kotlin.String})
      filler.addWithPath(EntityFiller({filler.target.get()!!::name}, {s: String? ->
          name?.origValue = s}) {s, t -> Json.decodeFromString(s, session!!.findKClass(t ?:
          "services.opencui.reservation.LocationName")!!) as?
          services.opencui.reservation.LocationName})
      filler.addWithPath(EntityFiller({filler.target.get()!!::timezone}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "java.time.ZoneId")!!) as?
          java.time.ZoneId})
      return filler
    }
  }

  public companion object {
    public val mappings: Map<String, Map<String, String>> = mutableMapOf<String, Map<String, String>>()
    public inline fun <reified S : IFrame> from(s: S): Location = Json.mappingConvert(s)
  }
}

public data class ValidationResult(
    @JsonInclude(NON_NULL)
    public override var session: UserSession? = null
) : IFrame {
    constructor(flag: Boolean) : this() {
        success = flag
    }

    constructor(flag: Boolean, msg: String) : this() {
        success = flag
        message = msg
    }


    @JsonProperty
    public var success: Boolean? = null

    @JsonProperty
    public var invalidFeatureKeys: MutableList<String>? = null

    @JsonProperty
    public var message: String? = null

    public override fun annotations(path: String): List<Annotation> = when (path) {
        "success" -> listOf(NeverAsk())
        "invalidFeatureKeys" ->
            listOf(SlotConditionalPromptAnnotation(listOf(LazyAction({if(invalidFeatureKeys.isNullOrEmpty())
                LazyAction({SlotRequestMore("invalidFeatureKeys", "kotlin.String", listOf(this),
                    templateOf("restful" to Prompts()))}) else LazyAction({SlotRequestMore("invalidFeatureKeys",
                "kotlin.String", listOf(this), templateOf("restful" to Prompts()))})}))),
                MinMaxAnnotation(1, {SlotNotifyFailure(invalidFeatureKeys, "invalidFeatureKeys",
                    "kotlin.String", io.opencui.core.da.FailType.MIN, listOf(this), templateOf("restful" to
                            Prompts()))}, 99, {SlotNotifyFailure(invalidFeatureKeys, "invalidFeatureKeys",
                    "kotlin.String", io.opencui.core.da.FailType.MAX, listOf(this), templateOf("restful" to
                            Prompts()))}), ValueCheckAnnotation({MaxValueCheck(session, {invalidFeatureKeys}, 99,
                    {SlotNotifyFailure(invalidFeatureKeys, "invalidFeatureKeys", "kotlin.String",
                        io.opencui.core.da.FailType.MAX, listOf(this), templateOf("restful" to Prompts()))})},
                    switch = {invalidFeatureKeys != null && invalidFeatureKeys!!.size > 99}), NeverAsk())
        "invalidFeatureKeys._item" ->
            listOf(SlotPromptAnnotation(LazyAction{SlotRequestMore("invalidFeatureKeys",
                "kotlin.String", listOf(this), templateOf("restful" to Prompts()))}))
        "message" -> listOf(NeverAsk())
        else -> listOf()
    }

    public override fun createBuilder(): FillBuilder = object :
        FillBuilder {
        public var frame: ValidationResult? = this@ValidationResult

        public override fun invoke(path: ParamPath): FrameFiller<ValidationResult> {
            val filler = FrameFiller({::frame}, path)
            filler.addWithPath(EntityFiller({filler.target.get()!!::success}, null) {s, t ->
                Json.decodeFromString(s, session!!.findKClass(t ?: "kotlin.Boolean")!!) as?
                        kotlin.Boolean})
            filler.addWithPath(MultiValueFiller({filler.target.get()!!::invalidFeatureKeys}, fun(p:
                                                                                                 KMutableProperty0<String?>): AEntityFiller {return EntityFiller({p}, null) {s, t ->
                Json.decodeFromString(s, session!!.findKClass(t ?: "kotlin.String")!!) as?
                        kotlin.String}}))
            filler.addWithPath(EntityFiller({filler.target.get()!!::message}, null) {s, t ->
                Json.decodeFromString(s, session!!.findKClass(t ?: "kotlin.String")!!) as? kotlin.String})
            return filler
        }
    }

    public companion object {
        public val mappings: Map<String, Map<String, String>> = mutableMapOf<String, Map<String,
                String>>()

        public inline fun <reified S : IFrame> from(s: S): ValidationResult = Json.mappingConvert(s)
    }
}

public interface IReservation : IHours, IService {
  @JsonIgnore
  public fun makeReservation(
      userId: String,
      duration: Int,
      resource: Resource,
      date: LocalDate?,
      time: LocalTime?,
      title: String?,
      userName: PersonName?,
  ): Reservation?

  @JsonIgnore
  public fun listReservation(
    userId: String,
    location: Location?,
    resourceType: ResourceType?
  ): List<Reservation>

  @JsonIgnore
  public fun cancelReservation(reservation: Reservation): ValidationResult

  @JsonIgnore
  public fun resourceAvailable(
    date: LocalDate?,
    time: LocalTime?,
    duration: Int,
    resource: Resource
  ): ValidationResult

  @JsonIgnore
  public fun reservationUpdatable(
    reservation: Reservation,
    date: LocalDate?,
    time: LocalTime?,
    duration: Int,
    resource: Resource
  ): ValidationResult

  @JsonIgnore
  public fun updateReservation(
    reservation: Reservation,
    date: LocalDate?,
    time: LocalTime?,
    duration: Int,
    resource: Resource
  ): ValidationResult

  @JsonIgnore
  public fun reservationCancelable(reservation: Reservation): ValidationResult

  @JsonIgnore
  public fun listLocation(): List<Location>

  @JsonIgnore
  public fun availableDates(
    startOffset: Int,
    numOfDays: Int,
    resources: List<Resource>
  ): List<DateAvailability>

  @JsonIgnore
  public fun availableTimes(
    date: LocalDate?,
    resources: List<Resource>
  ): List<TimeInterval>

  @JsonIgnore
  public fun getResourceInfo(resourceId: String): Resource?

  @JsonIgnore
  public fun listResource(
    location: Location,
    type: ResourceType,
    date: LocalDate?,
    time: LocalTime?,
    duration: Int
  ): List<Resource>
}