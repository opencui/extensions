package services.opencui.hours

import com.fasterxml.jackson.`annotation`.JsonIgnore
import com.fasterxml.jackson.`annotation`.JsonProperty
import com.fasterxml.jackson.`annotation`.JsonSetter
import com.fasterxml.jackson.`annotation`.Nulls
import io.opencui.core.Annotation
import io.opencui.core.EntityFiller
import io.opencui.core.FillBuilder
import io.opencui.core.FrameFiller
import io.opencui.core.ICompositeFiller
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
import io.opencui.core.UserSession
import io.opencui.core.ValueCheckAnnotation
import io.opencui.core.da.SlotNotifyFailure
import io.opencui.core.da.SlotRequestMore
import io.opencui.core.templateOf
import io.opencui.serialization.Json
import java.time.LocalDate
import java.time.LocalTime
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.reflect.KMutableProperty0


public data class TimeInterval(
  @JsonSetter(contentNulls=Nulls.SKIP)
  public override var session: UserSession? = null,
) : IFrame {
  @JsonProperty
  public var startTime: LocalTime? = null

  @JsonProperty
  public var endTime: LocalTime? = null

  public override fun annotations(path: String): List<Annotation> = when (path) {
    "startTime" -> listOf(NeverAsk())
    "endTime" -> listOf(NeverAsk())
    else -> listOf()
  }

  public override fun createBuilder(p: KMutableProperty0<out Any?>?): FillBuilder = object :
      FillBuilder {
    public var frame: TimeInterval? = this@TimeInterval

    public override fun invoke(path: ParamPath): FrameFiller<TimeInterval> {
      val filler = FrameFiller({(p as? KMutableProperty0<TimeInterval?>) ?: ::frame}, path)
      filler.addWithPath(EntityFiller({filler.target.get()!!::startTime}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "java.time.LocalTime")!!) as?
          java.time.LocalTime})
      filler.addWithPath(EntityFiller({filler.target.get()!!::endTime}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "java.time.LocalTime")!!) as?
          java.time.LocalTime})
      return filler
    }
  }

  public companion object {
    public val mappings: Map<String, Map<String, String>> = mutableMapOf<String,
        Map<String, String>>()

    public inline fun <reified S : IFrame> from(s: S): TimeInterval = Json.mappingConvert(s)
  }
}

public data class BusinessHours(
  @JsonSetter(contentNulls=Nulls.SKIP)
  public override var session: UserSession? = null,
) : IFrame {
  @JsonProperty
  public var date: LocalDate? = null

  @JsonProperty
  public var opennings: MutableList<TimeInterval>? = null

  public override fun annotations(path: String): List<Annotation> = when (path) {
    "date" -> listOf(NeverAsk())
    "openings" ->
        listOf(SlotConditionalPromptAnnotation(listOf(LazyAction({if(opennings.isNullOrEmpty())
        LazyAction({SlotRequestMore("openings", "services.opencui.hours.TimeInterval", listOf(this),
        templateOf("restful" to Prompts()))}) else LazyAction({SlotRequestMore("openings",
        "services.opencui.hours.TimeInterval", listOf(this), templateOf("restful" to
        Prompts()))})}))), MinMaxAnnotation(1, {SlotNotifyFailure(opennings, "openings",
        "services.opencui.hours.TimeInterval", io.opencui.core.da.FailType.MIN, listOf(this),
        templateOf("restful" to Prompts()))}, 99, {SlotNotifyFailure(opennings, "openings",
        "services.opencui.hours.TimeInterval", io.opencui.core.da.FailType.MAX, listOf(this),
        templateOf("restful" to Prompts()))}), ValueCheckAnnotation({MaxValueCheck(session,
        {opennings}, 99, {SlotNotifyFailure(opennings, "openings",
        "services.opencui.hours.TimeInterval", io.opencui.core.da.FailType.MAX, listOf(this),
        templateOf("restful" to Prompts()))})}, switch = {opennings != null &&
        opennings!!.size > 99}), NeverAsk())
    "openings._item" -> listOf(SlotPromptAnnotation(LazyAction{SlotRequestMore("openings",
        "services.opencui.hours.TimeInterval", listOf(this), templateOf("restful" to Prompts()))}))
    else -> listOf()
  }

  public override fun createBuilder(p: KMutableProperty0<out Any?>?): FillBuilder = object :
      FillBuilder {
    public var frame: BusinessHours? = this@BusinessHours

    public override fun invoke(path: ParamPath): FrameFiller<BusinessHours> {
      val filler = FrameFiller({(p as? KMutableProperty0<BusinessHours?>) ?: ::frame}, path)
      filler.addWithPath(EntityFiller({filler.target.get()!!::date}, null) {s, t ->
          Json.decodeFromString(s, session!!.findKClass(t ?: "java.time.LocalDate")!!) as?
          java.time.LocalDate})
      filler.addWithPath(MultiValueFiller({filler.target.get()!!::opennings}, fun(p:
          KMutableProperty0<TimeInterval?>): ICompositeFiller {return p.apply {
          set(TimeInterval(frame!!.session))
          }.get()!!.createBuilder(p).invoke(path.join("openings._item", p.get()))}))
      return filler
    }
  }

  public companion object {
    public val mappings: Map<String, Map<String, String>> = mutableMapOf<String,
        Map<String, String>>()

    public inline fun <reified S : IFrame> from(s: S): BusinessHours = Json.mappingConvert(s)
  }
}

public interface IHours : IService {
  @JsonIgnore
  public fun getHoursByDay(day: LocalDate): BusinessHours

  @JsonIgnore
  public fun getHoursByWeek(): List<BusinessHours>
}
