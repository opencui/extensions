package services.opencui.reservation

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.`annotation`.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.databind.node.ObjectNode
import io.opencui.core.AEntityFiller
import io.opencui.core.AlwaysAsk
import io.opencui.core.Annotation
import io.opencui.core.EntityFiller
import io.opencui.core.FillBuilder
import io.opencui.core.FrameFiller
import io.opencui.core.IChatbot
import io.opencui.core.IEntity
import io.opencui.core.IFrame
import io.opencui.core.IService
import io.opencui.core.LazyAction
import io.opencui.core.MaxValueCheck
import io.opencui.core.MinMaxAnnotation
import io.opencui.core.MultiValueFiller
import io.opencui.core.NeverAsk
import io.opencui.core.ParamPath
import io.opencui.core.Prompts
import io.opencui.core.SlotConditionalPromptAnnotation
import io.opencui.core.SlotPromptAnnotation
import io.opencui.core.SlotValue
import io.opencui.core.UserSession
import io.opencui.core.ValueCheckAnnotation
import io.opencui.core.da.SlotNotifyFailure
import io.opencui.core.da.SlotRequest
import io.opencui.core.da.SlotRequestMore
import io.opencui.core.templateOf
import io.opencui.serialization.Json
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

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface Resource : IFrame {
    public var id: String?

    public var type: ResourceType?

    public var name: ResourceName?

    public var durations: MutableList<Int>?
}

// We need to figure out code gen.
public data class Reservation(
  @JsonSetter(contentNulls = Nulls.SKIP)
  public override var session: UserSession? = null
) : IFrame {
  @JsonProperty
  public var id: String? = null

  @JsonProperty
  public var resourceId: String? = null

  @JsonProperty
  public var userId: String? = null

  @JsonProperty
  public var start: OffsetDateTime? = null

  @JsonProperty
  public var end: OffsetDateTime? = null

  @get:JsonIgnore
  public val reservationService: IReservation
    public get() = session!!.getExtension<IReservation>()!!

  @JsonIgnore
  public fun getResourceInfo(): Resource? {
    return reservationService!!.getResourceInfo(resourceId!!)
  }

  public override fun annotations(path: String): List<Annotation> = when (path) {
    "id" -> listOf(NeverAsk())
    "resourceId" -> listOf(NeverAsk())
    "userId" -> listOf(NeverAsk())
    "start" -> listOf(NeverAsk())
    "end" -> listOf(NeverAsk())
    else -> listOf()
  }

  public override fun createBuilder(p: KMutableProperty0<out Any?>?): FillBuilder = object :
      FillBuilder {
    public var frame: Reservation? = this@Reservation

    public override fun invoke(path: ParamPath): FrameFiller<Reservation> {
      val filler = FrameFiller({(p as? KMutableProperty0<Reservation?>) ?: ::frame}, path)
      filler.addWithPath(EntityFiller({filler.target.get()!!::id}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "kotlin.String")!!) as? kotlin.String})
      filler.addWithPath(EntityFiller({filler.target.get()!!::resourceId}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "kotlin.String")!!) as? kotlin.String})
      filler.addWithPath(EntityFiller({filler.target.get()!!::userId}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "kotlin.String")!!) as? kotlin.String})
      filler.addWithPath(EntityFiller({filler.target.get()!!::start}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "java.time.OffsetDateTime")!!) as?
          java.time.OffsetDateTime})
      filler.addWithPath(EntityFiller({filler.target.get()!!::end}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "java.time.OffsetDateTime")!!) as?
          java.time.OffsetDateTime})
      return filler
    }
  }

  public companion object {
    public val mappings: Map<String, Map<String, String>> = mutableMapOf<String, Map<String, String>>()

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

  public override fun createBuilder(p: KMutableProperty0<out Any?>?): FillBuilder = object :
      FillBuilder {
    public var frame: Location? = this@Location

    public override fun invoke(path: ParamPath): FrameFiller<Location> {
      val filler = FrameFiller({(p as? KMutableProperty0<Location?>) ?: ::frame}, path)
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

    public override fun createBuilder(p: KMutableProperty0<out Any?>?): FillBuilder = object :
        FillBuilder {
        public var frame: ValidationResult? = this@ValidationResult

        public override fun invoke(path: ParamPath): FrameFiller<ValidationResult> {
            val filler = FrameFiller({(p as? KMutableProperty0<ValidationResult?>) ?: ::frame}, path)
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

public interface IReservation : IService {
    @JsonIgnore
    public fun makeReservation(
        userId: String,
        location: Location,
        resourceType: ResourceType,
        date: LocalDate?,
        time: LocalTime?,
        duration: Int,
        filter: List<SlotValue>?
    ): Reservation?

    @JsonIgnore
    public fun listReservation(
        userId: String,
        location: Location,
        resourceType: ResourceType
    ): List<Reservation>

    @JsonIgnore
    public fun cancelReservation(location: Location, reservation: Reservation): ValidationResult

    @JsonIgnore
    public fun resourceAvailable(
        location: Location,
        type: ResourceType,
        date: LocalDate?,
        time: LocalTime?,
        duration: Int,
        filter: List<SlotValue>?
    ): ValidationResult

    @JsonIgnore
    public fun reservationUpdatable(
        location: Location,
        reservation: Reservation,
        date: LocalDate?,
        time: LocalTime?,
        duration: Int,
        features: List<SlotValue>?
    ): ValidationResult

    @JsonIgnore
    public fun updateReservation(
        location: Location,
        reservation: Reservation,
        date: LocalDate?,
        time: LocalTime?,
        duration: Int,
        features: List<SlotValue>
    ): ValidationResult

    @JsonIgnore
    public fun reservationCancelable(location: Location, reservation: Reservation): ValidationResult

    @JsonIgnore
    public fun listLocation(): List<Location>

    @JsonIgnore
    public fun availableDates(
        location: Location,
        resourceType: ResourceType,
        time: LocalTime?,
        duration: Int,
        filter: List<SlotValue>?
    ): List<LocalDate>

    @JsonIgnore
    public fun availableTimes(
        location: Location,
        resourceType: ResourceType,
        date: LocalDate?,
        duration: Int,
        filter: List<SlotValue>?
    ): List<LocalTime>

    @JsonIgnore
    public fun getResourceInfo(resourceId: String): Resource?

    @JsonIgnore
    public fun listResource(
        location: Location,
        type: ResourceType,
        date: LocalDate?,
        time: LocalTime?,
        duration: Int,
        filter: List<SlotValue>?
    ): List<Resource>
}
