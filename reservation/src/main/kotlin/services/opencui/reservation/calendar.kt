package services.opencui.reservation

import com.fasterxml.jackson.`annotation`.JsonIgnore
import com.fasterxml.jackson.`annotation`.JsonInclude
import com.fasterxml.jackson.`annotation`.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.`annotation`.JsonProperty
import com.fasterxml.jackson.`annotation`.JsonSubTypes
import com.fasterxml.jackson.`annotation`.JsonTypeInfo
import com.fasterxml.jackson.`annotation`.JsonValue
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventAttendee
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.directory.Directory
import com.google.api.services.directory.DirectoryScopes
import com.google.api.services.directory.model.CalendarResource
import io.opencui.core.*
import io.opencui.core.Annotation
import io.opencui.core.da.*
import io.opencui.du.BertStateTracker
import io.opencui.du.DUMeta
import io.opencui.du.DUSlotMeta
import io.opencui.du.EntityType
import io.opencui.du.LangPack
import io.opencui.du.StateTracker
import io.opencui.serialization.Json
import java.lang.Class
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.*
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty0




public data class Agent(
    public val user: String?
) : IChatbot() {
    public override val duMeta: DUMeta
        public get() = Agent.duMeta

    public override val stateTracker: StateTracker
        public get() = Agent.stateTracker

    public override val rewriteRules: MutableList<KClass<out DialogActRewriter>> = mutableListOf()

    public override val routing: Map<String, RoutingInfo> = mapOf()
    init {
        rewriteRules += Class.forName("io.opencui.core.da.SlotOfferSepInformConfirmRule").kotlin as
                KClass<out DialogActRewriter>
    }

    public constructor() : this("")

    public companion object {
        public val duMeta: DUMeta = loadDUMetaDsl(struct, Agent::class.java.classLoader,
            "services.opencui", "reservation", "struct", "769257801632452608", "271", "Asia/Shanghai")

        public val stateTracker: StateTracker = BertStateTracker(duMeta)
    }
}

public object struct : LangPack {
    public override val frames: List<ObjectNode> = listOf()

    public override val entityTypes: Map<String, EntityType> = mapOf("kotlin.Int" to
            entityType("kotlin.Int") {
                children(listOf())
                recognizer("DucklingRecognizer")
            }
        ,
        "kotlin.Float" to entityType("kotlin.Float") {
            children(listOf())
            recognizer("DucklingRecognizer")
        }
        ,
        "kotlin.String" to entityType("kotlin.String") {
            children(listOf())
        }
        ,
        "kotlin.Boolean" to entityType("kotlin.Boolean") {
            children(listOf())
            recognizer("ListRecognizer")
        }
        ,
        "kotlin.Unit" to entityType("kotlin.Unit") {
            children(listOf())
        }
        ,
        "java.time.LocalDateTime" to entityType("java.time.LocalDateTime") {
            children(listOf())
            recognizer("DucklingRecognizer")
        }
        ,
        "java.time.Year" to entityType("java.time.Year") {
            children(listOf())
            recognizer("DucklingRecognizer")
        }
        ,
        "java.time.YearMonth" to entityType("java.time.YearMonth") {
            children(listOf())
            recognizer("DucklingRecognizer")
        }
        ,
        "java.time.LocalDate" to entityType("java.time.LocalDate") {
            children(listOf())
            recognizer("DucklingRecognizer")
        }
        ,
        "java.time.LocalTime" to entityType("java.time.LocalTime") {
            children(listOf())
            recognizer("DucklingRecognizer")
        }
        ,
        "java.time.DayOfWeek" to entityType("java.time.DayOfWeek") {
            children(listOf())
            recognizer("ListRecognizer")
        }
        ,
        "java.time.ZoneId" to entityType("java.time.ZoneId") {
            children(listOf())
            recognizer("ListRecognizer")
        }
        ,
        "kotlin.Any" to entityType("kotlin.Any") {
            children(listOf())
        }
        ,
        "io.opencui.core.Email" to entityType("io.opencui.core.Email") {
            children(listOf())
            recognizer("DucklingRecognizer")
        }
        ,
        "io.opencui.core.PhoneNumber" to entityType("io.opencui.core.PhoneNumber") {
            children(listOf())
            recognizer("DucklingRecognizer")
        }
        ,
        "io.opencui.core.Ordinal" to entityType("io.opencui.core.Ordinal") {
            children(listOf())
            recognizer("DucklingRecognizer")
        }
        ,
        "io.opencui.core.Currency" to entityType("io.opencui.core.Currency") {
            children(listOf())
            recognizer("ListRecognizer")
        }
        ,
        "io.opencui.core.FrameType" to entityType("io.opencui.core.FrameType") {
            children(listOf())
            recognizer("ListRecognizer")
        }
        ,
        "io.opencui.core.EntityType" to entityType("io.opencui.core.EntityType") {
            children(listOf())
            recognizer("ListRecognizer")
        }
        ,
        "io.opencui.core.SlotType" to entityType("io.opencui.core.SlotType") {
            children(listOf())
            recognizer("ListRecognizer")
        }
        ,
        "io.opencui.core.PromptMode" to entityType("io.opencui.core.PromptMode") {
            children(listOf())
            recognizer("ListRecognizer")
        }
        ,
        "io.opencui.core.Language" to entityType("io.opencui.core.Language") {
            children(listOf())
            recognizer("ListRecognizer")
        }
        ,
        "io.opencui.core.Country" to entityType("io.opencui.core.Country") {
            children(listOf())
            recognizer("ListRecognizer")
        }
        ,
        "io.opencui.core.FillState" to entityType("io.opencui.core.FillState") {
            children(listOf())
            recognizer("ListRecognizer")
        }
        ,
        "io.opencui.core.FailType" to entityType("io.opencui.core.FailType") {
            children(listOf())
            recognizer("ListRecognizer")
        }
        ,
        "services.opencui.reservation.ResourceType" to
                entityType("services.opencui.reservation.ResourceType") {
                    children(listOf())
                    recognizer("ListRecognizer")
                }
        ,
        "services.opencui.reservation.Comparation" to
                entityType("services.opencui.reservation.Comparation") {
                    children(listOf())
                    recognizer("ListRecognizer")
                }
    )

    public override val frameSlotMetas: Map<String, List<DUSlotMeta>> =
        mapOf("io.opencui.core.PagedSelectable" to listOf(
            DUSlotMeta(label = "index", isMultiValue = false, type = "io.opencui.core.Ordinal", isHead =
            false, triggers = listOf()),
        ),
            "io.opencui.core.IDonotGetIt" to listOf(
            ),
            "io.opencui.core.IDonotKnowWhatToDo" to listOf(
            ),
            "io.opencui.core.AbortIntent" to listOf(
                DUSlotMeta(label = "intentType", isMultiValue = false, type = "io.opencui.core.FrameType",
                    isHead = false, triggers = listOf()),
                DUSlotMeta(label = "intent", isMultiValue = false, type = "io.opencui.core.IIntent", isHead =
                false, triggers = listOf()),
            ),
            "io.opencui.core.GetLiveAgent" to listOf(
            ),
            "io.opencui.core.BadCandidate" to listOf(
                DUSlotMeta(label = "value", isMultiValue = false, type = "T", isHead = false, triggers =
                listOf()),
                DUSlotMeta(label = "slotType", isMultiValue = false, type = "io.opencui.core.SlotType", isHead
                = false, triggers = listOf()),
            ),
            "io.opencui.core.BadIndex" to listOf(
                DUSlotMeta(label = "index", isMultiValue = false, type = "kotlin.Int", isHead = false,
                    triggers = listOf()),
            ),
            "io.opencui.core.ConfirmationNo" to listOf(
            ),
            "io.opencui.core.ResumeIntent" to listOf(
                DUSlotMeta(label = "intent", isMultiValue = false, type = "io.opencui.core.IIntent", isHead =
                false, triggers = listOf()),
            ),
            "io.opencui.core.SlotUpdate" to listOf(
                DUSlotMeta(label = "originalSlot", isMultiValue = false, type = "io.opencui.core.SlotType",
                    isHead = false, triggers = listOf()),
                DUSlotMeta(label = "oldValue", isMultiValue = false, type = "T", isHead = false, triggers =
                listOf()),
                DUSlotMeta(label = "index", isMultiValue = false, type = "io.opencui.core.Ordinal", isHead =
                false, triggers = listOf()),
                DUSlotMeta(label = "newValue", isMultiValue = false, type = "T", isHead = false, triggers =
                listOf()),
                DUSlotMeta(label = "confirm", isMultiValue = false, type =
                "io.opencui.core.confirmation.IStatus", isHead = false, triggers = listOf()),
            ),
            "io.opencui.core.da.SlotRequest" to listOf(
                DUSlotMeta(label = "slotName", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "slotType", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "context", isMultiValue = true, type = "io.opencui.core.IFrame", isHead =
                false, triggers = listOf()),
            ),
            "io.opencui.core.da.SlotRequestMore" to listOf(
                DUSlotMeta(label = "slotName", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "slotType", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "context", isMultiValue = true, type = "io.opencui.core.IFrame", isHead =
                false, triggers = listOf()),
            ),
            "io.opencui.core.da.SlotNotifyFailure" to listOf(
                DUSlotMeta(label = "target", isMultiValue = false, type = "T", isHead = false, triggers =
                listOf()),
                DUSlotMeta(label = "slotName", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "slotType", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "failType", isMultiValue = false, type = "io.opencui.core.FailType", isHead
                = false, triggers = listOf()),
                DUSlotMeta(label = "context", isMultiValue = true, type = "io.opencui.core.IFrame", isHead =
                false, triggers = listOf()),
            ),
            "io.opencui.core.da.SlotOffer" to listOf(
                DUSlotMeta(label = "value", isMultiValue = true, type = "T", isHead = false, triggers =
                listOf()),
                DUSlotMeta(label = "slotName", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "slotType", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "context", isMultiValue = true, type = "io.opencui.core.IFrame", isHead =
                false, triggers = listOf()),
            ),
            "io.opencui.core.da.SlotOfferSepInform" to listOf(
                DUSlotMeta(label = "value", isMultiValue = false, type = "T", isHead = false, triggers =
                listOf()),
                DUSlotMeta(label = "slotName", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "slotType", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "context", isMultiValue = true, type = "io.opencui.core.IFrame", isHead =
                false, triggers = listOf()),
            ),
            "io.opencui.core.da.SlotOfferZepInform" to listOf(
                DUSlotMeta(label = "slotName", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "slotType", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "context", isMultiValue = true, type = "io.opencui.core.IFrame", isHead =
                false, triggers = listOf()),
            ),
            "io.opencui.core.da.SlotInform" to listOf(
                DUSlotMeta(label = "target", isMultiValue = false, type = "T", isHead = false, triggers =
                listOf()),
                DUSlotMeta(label = "slotName", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "slotType", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "context", isMultiValue = true, type = "io.opencui.core.IFrame", isHead =
                false, triggers = listOf()),
            ),
            "io.opencui.core.da.SlotConfirm" to listOf(
                DUSlotMeta(label = "target", isMultiValue = false, type = "T", isHead = false, triggers =
                listOf()),
                DUSlotMeta(label = "slotName", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "slotType", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "context", isMultiValue = true, type = "io.opencui.core.IFrame", isHead =
                false, triggers = listOf()),
            ),
            "io.opencui.core.da.FrameInform" to listOf(
                DUSlotMeta(label = "target", isMultiValue = false, type = "T", isHead = false, triggers =
                listOf()),
            ),
            "io.opencui.core.da.SlotGate" to listOf(
                DUSlotMeta(label = "slotName", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "slotType", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "context", isMultiValue = true, type = "io.opencui.core.IFrame", isHead =
                false, triggers = listOf()),
            ),
            "io.opencui.core.da.FrameOffer" to listOf(
                DUSlotMeta(label = "value", isMultiValue = true, type = "T", isHead = false, triggers =
                listOf()),
                DUSlotMeta(label = "frameType", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
            ),
            "io.opencui.core.da.FrameOfferSepInform" to listOf(
                DUSlotMeta(label = "value", isMultiValue = false, type = "T", isHead = false, triggers =
                listOf()),
                DUSlotMeta(label = "frameType", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
            ),
            "io.opencui.core.da.FrameOfferZepInform" to listOf(
                DUSlotMeta(label = "frameType", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
            ),
            "io.opencui.core.da.FrameConfirm" to listOf(
                DUSlotMeta(label = "target", isMultiValue = false, type = "T", isHead = false, triggers =
                listOf()),
            ),
            "io.opencui.core.da.UserDefinedInform" to listOf(
                DUSlotMeta(label = "target", isMultiValue = false, type = "T", isHead = false, triggers =
                listOf()),
            ),
            "io.opencui.core.da.SlotOfferSepInformConfirm" to listOf(
                DUSlotMeta(label = "target", isMultiValue = false, type = "T", isHead = false, triggers =
                listOf()),
                DUSlotMeta(label = "slotName", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "slotType", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "context", isMultiValue = true, type = "io.opencui.core.IFrame", isHead =
                false, triggers = listOf()),
            ),
            "io.opencui.core.da.SlotOfferSepInformConfirmRule" to listOf(
                DUSlotMeta(label = "slot0", isMultiValue = false, type =
                "io.opencui.core.da.SlotOfferSepInform", isHead = false, triggers = listOf()),
                DUSlotMeta(label = "slot1", isMultiValue = false, type = "io.opencui.core.da.SlotConfirm",
                    isHead = false, triggers = listOf()),
            ),
            "kotlin.Pair" to listOf(
            ),
            "io.opencui.core.IIntent" to listOf(
            ),
            "io.opencui.core.IContact" to listOf(
                DUSlotMeta(label = "channel", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "id", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
            ),
            "io.opencui.core.CleanSession" to listOf(
            ),
            "io.opencui.core.DontCare" to listOf(
                DUSlotMeta(label = "slot", isMultiValue = false, type = "io.opencui.core.EntityType", isHead =
                false, triggers = listOf()),
            ),
            "io.opencui.core.confirmation.IStatus" to listOf(
            ),
            "io.opencui.core.confirmation.Yes" to listOf(
            ),
            "io.opencui.core.confirmation.No" to listOf(
            ),
            "io.opencui.core.AmountOfMoney" to listOf(
            ),
            "io.opencui.core.hasMore.IStatus" to listOf(
            ),
            "io.opencui.core.hasMore.No" to listOf(
            ),
            "io.opencui.core.HasMore" to listOf(
                DUSlotMeta(label = "status", isMultiValue = false, type = "io.opencui.core.hasMore.IStatus",
                    isHead = false, triggers = listOf()),
            ),
            "io.opencui.core.hasMore.Yes" to listOf(
            ),
            "io.opencui.core.Companion" to listOf(
                DUSlotMeta(label = "slot", isMultiValue = false, type = "kotlin.Any", isHead = false, triggers
                = listOf()),
            ),
            "io.opencui.core.companion.Not" to listOf(
                DUSlotMeta(label = "slot", isMultiValue = false, type = "kotlin.Any", isHead = false, triggers
                = listOf()),
            ),
            "io.opencui.core.companion.Or" to listOf(
                DUSlotMeta(label = "slot", isMultiValue = false, type = "kotlin.Any", isHead = false, triggers
                = listOf()),
            ),
            "io.opencui.core.booleanGate.IStatus" to listOf(
            ),
            "io.opencui.core.booleanGate.Yes" to listOf(
            ),
            "io.opencui.core.booleanGate.No" to listOf(
            ),
            "io.opencui.core.IntentClarification" to listOf(
                DUSlotMeta(label = "utterance", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "source", isMultiValue = true, type = "io.opencui.core.IIntent", isHead =
                false, triggers = listOf()),
                DUSlotMeta(label = "target", isMultiValue = false, type = "io.opencui.core.IIntent", isHead =
                false, triggers = listOf()),
            ),
            "io.opencui.core.ValueClarification" to listOf(
                DUSlotMeta(label = "source", isMultiValue = true, type = "T", isHead = false, triggers =
                listOf()),
                DUSlotMeta(label = "target", isMultiValue = false, type = "T", isHead = false, triggers =
                listOf()),
            ),
            "io.opencui.core.NextPage" to listOf(
            ),
            "io.opencui.core.PreviousPage" to listOf(
            ),
            "io.opencui.core.SlotInit" to listOf(
                DUSlotMeta(label = "slot", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
            ),
            "io.opencui.core.EntityRecord" to listOf(
                DUSlotMeta(label = "label", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "expressions", isMultiValue = true, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
            ),
            "io.opencui.core.user.UserIdentifier" to listOf(
                DUSlotMeta(label = "channelType", isMultiValue = false, type = "kotlin.String", isHead =
                false, triggers = listOf()),
                DUSlotMeta(label = "userId", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "channelLabel", isMultiValue = false, type = "kotlin.String", isHead =
                false, triggers = listOf()),
            ),
            "io.opencui.core.user.IUserProfile" to listOf(
                DUSlotMeta(label = "channelType", isMultiValue = false, type = "kotlin.String", isHead =
                false, triggers = listOf()),
                DUSlotMeta(label = "userId", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "channelLabel", isMultiValue = false, type = "kotlin.String", isHead =
                false, triggers = listOf()),
                DUSlotMeta(label = "phone", isMultiValue = false, type = "io.opencui.core.PhoneNumber", isHead
                = false, triggers = listOf()),
                DUSlotMeta(label = "name", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "email", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "userInputCode", isMultiValue = false, type = "kotlin.Int", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "code", isMultiValue = false, type = "kotlin.Int", isHead = false, triggers
                = listOf()),
            ),
            "io.opencui.core.user.IUserIdentifier" to listOf(
                DUSlotMeta(label = "channelType", isMultiValue = false, type = "kotlin.String", isHead =
                false, triggers = listOf()),
                DUSlotMeta(label = "userId", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "channelLabel", isMultiValue = false, type = "kotlin.String", isHead =
                false, triggers = listOf()),
            ),
            "io.opencui.core.IPersistent" to listOf(
            ),
            "io.opencui.core.ISingleton" to listOf(
            ),
            "io.opencui.core.IKernelIntent" to listOf(
            ),
            "io.opencui.core.ITransactionalIntent" to listOf(
            ),
            "io.opencui.core.That" to listOf(
                DUSlotMeta(label = "slot", isMultiValue = false, type = "T", isHead = true, triggers =
                listOf()),
            ),
            "io.opencui.core.SlotClarification" to listOf(
                DUSlotMeta(label = "mentionedSource", isMultiValue = false, type = "io.opencui.core.Cell",
                    isHead = false, triggers = listOf()),
                DUSlotMeta(label = "source", isMultiValue = true, type = "io.opencui.core.Cell", isHead =
                false, triggers = listOf()),
                DUSlotMeta(label = "target", isMultiValue = false, type = "io.opencui.core.Cell", isHead =
                false, triggers = listOf()),
            ),
            "io.opencui.core.Cell" to listOf(
                DUSlotMeta(label = "originalSlot", isMultiValue = false, type = "io.opencui.core.SlotType",
                    isHead = false, triggers = listOf()),
                DUSlotMeta(label = "index", isMultiValue = false, type = "io.opencui.core.Ordinal", isHead =
                false, triggers = listOf()),
            ),
            "io.opencui.core.UserSession" to listOf(
                DUSlotMeta(label = "chatbot", isMultiValue = false, type = "io.opencui.core.IChatbot", isHead
                = false, triggers = listOf()),
                DUSlotMeta(label = "channelType", isMultiValue = false, type = "kotlin.String", isHead =
                false, triggers = listOf()),
            ),
            "io.opencui.core.IChatbot" to listOf(
            ),
            "io.opencui.core.IFrame" to listOf(
            ),
            "services.opencui.reservation.Resource" to listOf(
                DUSlotMeta(label = "id", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "type", isMultiValue = false, type =
                "services.opencui.reservation.ResourceType", isHead = false, triggers = listOf()),
                DUSlotMeta(label = "name", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
            ),
            "services.opencui.reservation.Reservation" to listOf(
                DUSlotMeta(label = "id", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "resourceId", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "userId", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "startDate", isMultiValue = false, type = "java.time.LocalDate", isHead =
                false, triggers = listOf()),
                DUSlotMeta(label = "startTime", isMultiValue = false, type = "java.time.LocalTime", isHead =
                false, triggers = listOf()),
                DUSlotMeta(label = "duration", isMultiValue = false, type = "kotlin.Int", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "endDate", isMultiValue = false, type = "java.time.LocalDate", isHead =
                false, triggers = listOf()),
                DUSlotMeta(label = "endTime", isMultiValue = false, type = "java.time.LocalTime", isHead =
                false, triggers = listOf()),
            ),
            "services.opencui.reservation.ResourceFeature" to listOf(
                DUSlotMeta(label = "key", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "value", isMultiValue = false, type = "kotlin.Any", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "operator", isMultiValue = false, type =
                "services.opencui.reservation.Comparation", isHead = false, triggers = listOf()),
            ),
            "services.opencui.reservation.Location" to listOf(
                DUSlotMeta(label = "id", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "type", isMultiValue = false, type =
                "services.opencui.reservation.ResourceType", isHead = false, triggers = listOf()),
                DUSlotMeta(label = "name", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
            ),
            "services.opencui.reservation.ValidationResult" to listOf(
                DUSlotMeta(label = "success", isMultiValue = false, type = "kotlin.Boolean", isHead = false,
                    triggers = listOf()),
                DUSlotMeta(label = "invalidFeatureKeys", isMultiValue = true, type = "kotlin.String", isHead =
                false, triggers = listOf()),
                DUSlotMeta(label = "message", isMultiValue = false, type = "kotlin.String", isHead = false,
                    triggers = listOf()),
            ),
            "services.opencui.reservation.TimeRange" to listOf(
                DUSlotMeta(label = "startTime", isMultiValue = false, type = "java.time.LocalTime", isHead =
                false, triggers = listOf()),
                DUSlotMeta(label = "endTime", isMultiValue = false, type = "java.time.LocalTime", isHead =
                false, triggers = listOf()),
            ),
        )

    public override val typeAlias: Map<String, List<String>> = mapOf()
}

public data class ResourceType(
    @get:JsonIgnore
    public override var value: String
) : IEntity {
    public override var origValue: String? = null

    @JsonValue
    public override fun toString(): String = value

    @JsonIgnore
    public fun getChildren(): List<ResourceType> =  ResourceType.getAllInstances()

    public companion object {
        @JsonIgnore
        public val valueGood: ((String) -> Boolean)? = { true }

        @JsonIgnore
        public fun getAllInstances(): List<ResourceType> =
            Agent.duMeta.getEntityInstances(ResourceType::class.qualifiedName!!).map {
                ResourceType(it.key) }
    }
}

public data class Comparation(
    @get:JsonIgnore
    public override var value: String
) : IEntity {
    public override var origValue: String? = null

    @JsonValue
    public override fun toString(): String = value

    @JsonIgnore
    public fun getChildren(): List<Comparation> =  Comparation.getAllInstances()

    public companion object {
        @JsonIgnore
        public val valueGood: ((String) -> Boolean)? = { true }

        @JsonIgnore
        public fun getAllInstances(): List<Comparation> =
            Agent.duMeta.getEntityInstances(Comparation::class.qualifiedName!!).map {
                Comparation(it.key) }
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonSubTypes(value = [JsonSubTypes.Type(value = Location::class)])
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

    @JsonIgnore
    public fun getResourceInfo(): Resource? {
        TODO()
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

public data class ResourceFeature(
    @JsonInclude(NON_NULL)
    public override var session: UserSession? = null
) : IFrame {
    @JsonProperty
    public var key: String? = null

    @JsonProperty
    public var value: Any? = null

    @JsonProperty
    public var `operator`: Comparation? = null

    public override fun annotations(path: String): List<Annotation> = when (path) {
        "key" -> listOf(NeverAsk())
        "value" -> listOf(SlotPromptAnnotation(LazyAction{SlotRequest("value", "kotlin.Any",
            listOf(this), templateOf("restful" to Prompts()))}), AlwaysAsk())
        "operator" -> listOf(SlotPromptAnnotation(LazyAction{SlotRequest("operator",
            "services.opencui.reservation.Comparation", listOf(this), templateOf("restful" to
                    Prompts()))}), AlwaysAsk())
        else -> listOf()
    }

    public override fun createBuilder(p: KMutableProperty0<out Any?>?): FillBuilder = object :
        FillBuilder {
        public var frame: ResourceFeature? = this@ResourceFeature

        public override fun invoke(path: ParamPath): FrameFiller<ResourceFeature> {
            val filler = FrameFiller({(p as? KMutableProperty0<ResourceFeature?>) ?: ::frame}, path)
            filler.addWithPath(EntityFiller({filler.target.get()!!::key}, null) {s, t ->
                Json.decodeFromString(s, session!!.findKClass(t ?: "kotlin.String")!!) as? kotlin.String})
            filler.addWithPath(EntityFiller({filler.target.get()!!::value}, null) {s, t ->
                Json.decodeFromString(s, session!!.findKClass(t ?: "kotlin.Any")!!) as? kotlin.Any})
            filler.addWithPath(EntityFiller({filler.target.get()!!::`operator`}, {s: String? ->
                operator?.origValue = s}) {s, t -> Json.decodeFromString(s, session!!.findKClass(t ?:
            "services.opencui.reservation.Comparation")!!) as?
                    services.opencui.reservation.Comparation})
            return filler
        }
    }

    public companion object {
        public val mappings: Map<String, Map<String, String>> = mutableMapOf<String, Map<String,
                String>>()

        public inline fun <reified S : IFrame> from(s: S): ResourceFeature = Json.mappingConvert(s)
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

public data class TimeRange(
    @JsonInclude(NON_NULL)
    public override var session: UserSession? = null
) : IFrame {
    @JsonProperty
    public var startTime: LocalTime? = null

    @JsonProperty
    public var endTime: LocalTime? = null

    public override fun annotations(path: String): List<Annotation> = when (path) {
        "startTime" -> listOf(SlotPromptAnnotation(LazyAction{SlotRequest("startTime",
            "java.time.LocalTime", listOf(this), templateOf("restful" to Prompts()))}), AlwaysAsk())
        "endTime" -> listOf(SlotPromptAnnotation(LazyAction{SlotRequest("endTime",
            "java.time.LocalTime", listOf(this), templateOf("restful" to Prompts()))}), AlwaysAsk())
        else -> listOf()
    }

    public override fun createBuilder(p: KMutableProperty0<out Any?>?): FillBuilder = object :
        FillBuilder {
        public var frame: TimeRange? = this@TimeRange

        public override fun invoke(path: ParamPath): FrameFiller<TimeRange> {
            val filler = FrameFiller({(p as? KMutableProperty0<TimeRange?>) ?: ::frame}, path)
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
        public val mappings: Map<String, Map<String, String>> = mutableMapOf<String, Map<String,
                String>>()

        public inline fun <reified S : IFrame> from(s: S): TimeRange = Json.mappingConvert(s)
    }
}

public interface IReservation : IService {
    @JsonIgnore
    public fun makeReservation(
        userId: String,
        resourceType: ResourceType,
        date: LocalDate?,
        time: LocalTime?,
        filter: List<ResourceFeature>?
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
        filter: List<ResourceFeature>?
    ): ValidationResult

    @JsonIgnore
    public fun reservationUpdatable(
        reservationId: String,
        date: LocalDate,
        time: LocalTime,
        features: List<ResourceFeature>?
    ): ValidationResult

    @JsonIgnore
    public fun updateReservation(
        reservationId: String,
        date: LocalDate?,
        time: LocalTime?,
        features: List<ResourceFeature>
    ): ValidationResult

    @JsonIgnore
    public fun reservationCancelable(id: String): ValidationResult

    @JsonIgnore
    public fun listLocation(): List<Location>

    @JsonIgnore
    public fun pickLocation(locationId: String): Location?

    @JsonIgnore
    public fun availableDates(
        resourceType: ResourceType,
        time: LocalTime?,
        filter: List<ResourceFeature>?
    ): List<LocalDate>

    @JsonIgnore
    public fun availableTimeRanges(
        resourceType: ResourceType,
        date: LocalDate?,
        filter: List<ResourceFeature>?
    ): List<TimeRange>

    @JsonIgnore
    public fun getResourceInfo(resourceId: String): Resource?
}

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
    fun checkEvent(date: LocalDate?, close:LocalTime, open:LocalTime, start:LocalTime, resourceType: ResourceType, filter: List<ResourceFeature>?): MutableList<TimeRange> {

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



