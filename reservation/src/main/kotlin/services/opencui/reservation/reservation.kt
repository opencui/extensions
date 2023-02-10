package services.opencui.reservation

import com.fasterxml.jackson.`annotation`.JsonIgnore
import com.fasterxml.jackson.`annotation`.JsonInclude
import com.fasterxml.jackson.`annotation`.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.`annotation`.JsonProperty
import com.fasterxml.jackson.`annotation`.JsonTypeInfo
import com.fasterxml.jackson.`annotation`.JsonValue
import io.opencui.core.AEntityFiller
import io.opencui.core.AlwaysAsk
import io.opencui.core.Annotation
import io.opencui.core.EntityFiller
import io.opencui.core.FillBuilder
import io.opencui.core.FrameFiller
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
import java.time.LocalDate
import java.time.LocalTime
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

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface Resource : IFrame {
  public var id: String?

  public var type: ResourceType?

  public var name: String?
}

public data class Reservation(
  @JsonInclude(NON_NULL)
  public override var session: UserSession? = null
) : IFrame {
  @JsonProperty
  public var id: String? = null

  @JsonProperty
  public var resourceId: String? = null

  @JsonProperty
  public var userId: String? = null

  @JsonProperty
  public var startDate: LocalDate? = null

  @JsonProperty
  public var startTime: LocalTime? = null

  @JsonProperty
  public var duration: Int? = null

  @JsonProperty
  public var endDate: LocalDate? = null

  @JsonProperty
  public var endTime: LocalTime? = null

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
    "startDate" -> listOf(SlotPromptAnnotation(LazyAction{SlotRequest("startDate",
        "java.time.LocalDate", listOf(this), templateOf("restful" to Prompts()))}), AlwaysAsk())
    "startTime" -> listOf(SlotPromptAnnotation(LazyAction{SlotRequest("startTime",
        "java.time.LocalTime", listOf(this), templateOf("restful" to Prompts()))}), AlwaysAsk())
    "duration" -> listOf(SlotPromptAnnotation(LazyAction{SlotRequest("duration", "kotlin.Int",
        listOf(this), templateOf("restful" to Prompts()))}), AlwaysAsk())
    "endDate" -> listOf(SlotPromptAnnotation(LazyAction{SlotRequest("endDate",
        "java.time.LocalDate", listOf(this), templateOf("restful" to Prompts()))}), AlwaysAsk())
    "endTime" -> listOf(SlotPromptAnnotation(LazyAction{SlotRequest("endTime",
        "java.time.LocalTime", listOf(this), templateOf("restful" to Prompts()))}), AlwaysAsk())
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
      filler.addWithPath(EntityFiller({filler.target.get()!!::startDate}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "java.time.LocalDate")!!) as?
          java.time.LocalDate})
      filler.addWithPath(EntityFiller({filler.target.get()!!::startTime}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "java.time.LocalTime")!!) as?
          java.time.LocalTime})
      filler.addWithPath(EntityFiller({filler.target.get()!!::duration}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "kotlin.Int")!!) as? kotlin.Int})
      filler.addWithPath(EntityFiller({filler.target.get()!!::endDate}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "java.time.LocalDate")!!) as?
          java.time.LocalDate})
      filler.addWithPath(EntityFiller({filler.target.get()!!::endTime}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "java.time.LocalTime")!!) as?
          java.time.LocalTime})
      return filler
    }
  }

  public companion object {
    public val mappings: Map<String, Map<String, String>> = mutableMapOf<String, Map<String,
        String>>()

    public inline fun <reified S : IFrame> from(s: S): Reservation = Json.mappingConvert(s)
  }
}

public data class Location(
  @JsonInclude(NON_NULL)
  public override var session: UserSession? = null
) : Resource, IFrame {
  @JsonProperty
  public override var id: String? = null

  @JsonProperty
  public override var type: ResourceType? = null

  @JsonProperty
  public override var name: String? = null

  public override fun annotations(path: String): List<Annotation> = when (path) {
    "id" -> listOf(NeverAsk())
    "type" -> listOf(SlotPromptAnnotation(LazyAction{SlotRequest("type",
        "services.opencui.reservation.ResourceType", listOf(this), templateOf("restful" to
        Prompts()))}), AlwaysAsk())
    "name" -> listOf(NeverAsk())
    else -> listOf()
  }

  public override fun createBuilder(p: KMutableProperty0<out Any?>?): FillBuilder = object :
      FillBuilder {
    public var frame: Location? = this@Location

    public override fun invoke(path: ParamPath): FrameFiller<Location> {
      val filler = FrameFiller({(p as? KMutableProperty0<Location?>) ?: ::frame}, path)
      filler.addWithPath(EntityFiller({filler.target.get()!!::id}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "kotlin.String")!!) as? kotlin.String})
      filler.addWithPath(EntityFiller({filler.target.get()!!::type}, {s: String? ->
          type?.origValue = s}) {s, t -> Json.decodeFromString(s, session!!.findKClass(t ?:
          "services.opencui.reservation.ResourceType")!!) as?
          services.opencui.reservation.ResourceType})
      filler.addWithPath(EntityFiller({filler.target.get()!!::name}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "kotlin.String")!!) as? kotlin.String})
      return filler
    }
  }

  public companion object {
    public val mappings: Map<String, Map<String, String>> = mutableMapOf<String, Map<String,
        String>>()

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
    "success" -> listOf(SlotPromptAnnotation(LazyAction{SlotRequest("success", "kotlin.Boolean",
        listOf(this), templateOf("restful" to Prompts()))}), AlwaysAsk())
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
    resourceType: ResourceType,
    date: LocalDate?,
    time: LocalTime?,
    filter: List<SlotValue>?
  ): Reservation?

  @JsonIgnore
  public fun listReservation(userId: String, resourceType: ResourceType): List<Reservation>

  @JsonIgnore
  public fun cancelReservation(id: String): ValidationResult

  @JsonIgnore
  public fun resourceAvailable(
    type: ResourceType,
    date: LocalDate?,
    time: LocalTime?,
    filter: List<SlotValue>?
  ): ValidationResult

  @JsonIgnore
  public fun reservationUpdatable(
    reservationId: String,
    date: LocalDate,
    time: LocalTime,
    features: List<SlotValue>?
  ): ValidationResult

  @JsonIgnore
  public fun updateReservation(
    reservationId: String,
    date: LocalDate?,
    time: LocalTime?,
    features: List<SlotValue>
  ): ValidationResult

  @JsonIgnore
  public fun reservationCancelable(id: String): ValidationResult

  @JsonIgnore
  public fun listLocation(): List<Location>

  @JsonIgnore
  public fun pickLocation(location: Location): Boolean?

  @JsonIgnore
  public fun availableDates(
    resourceType: ResourceType,
    time: LocalTime?,
    filter: List<SlotValue>?
  ): List<LocalDate>

  @JsonIgnore
  public fun availableTimes(
    resourceType: ResourceType,
    date: LocalDate?,
    filter: List<SlotValue>?
  ): List<LocalTime>

  @JsonIgnore
  public fun getResourceInfo(resourceId: String): Resource?
  @JsonIgnore
  public fun listResource(
    type: ResourceType,
    date: LocalDate?,
    time: LocalTime?,
    filter: List<SlotValue>?
  ): List<Resource>
}