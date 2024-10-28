package com.squareup.metron

import com.samskivert.mustache.Mustache
import com.squareup.metron.MetricEvent.ClickFeature
import com.squareup.metron.MetricEvent.EncounterError
import com.squareup.metron.MetricEvent.LoadFeature
import com.squareup.metron.MetricEvent.ViewFeature
import com.squareup.wire.schema.*
import okio.Path
import okio.Path.Companion.toPath
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

private const val FOUR_SPACES = "    "

class MetronEventsMarkdownSchemaHandler(projectDirectory: String) : SchemaHandler() {
    private val docsPath = "$projectDirectory/docs".toPath()
    private val mkdocsFile = "$projectDirectory/mkdocs.yml".toPath()
    private val mkdocsMustacheTemplateFile = "$projectDirectory/mkdocs.yml.mustache".toPath()
    private val mkdocsConfigEvents = mutableListOf<MkdocsConfigTemplateData.EventTemplateData>()
    private val mkdocsConfigTypes = mutableListOf<MkdocsConfigTemplateData.TypeTemplateData>()
    private val mustacheCompiler: Mustache.Compiler by lazy {
        Mustache.compiler()
    }
    private lateinit var mkdocsMustacheTemplate: String
    private lateinit var fileOptions: FileOptions
    private val eventTypeCountMap: MutableMap<EventType, Int> = EventType.entries.associateWith { 0 }.toMutableMap()
    private val metricEventTypeCountMap: MutableMap<String, Int> = mutableMapOf()
    private val teamEventCountMap: MutableMap<Team, Int> = mutableMapOf()

    override fun handle(schema: Schema, context: Context) {
        fileOptions = FileOptions(schema)
        mkdocsMustacheTemplate = context.fileSystem.read(mkdocsMustacheTemplateFile) {
            readUtf8()
        }
        super.handle(schema, context)
    }

    override fun handle(type: Type, context: Context): Path? {
        if (type.ignoreDocsGeneration()) return null

        return when (val eventType = type.options.eventType) {
            EventType.ActionTap, EventType.ActionCustom, EventType.Log, EventType.Error, EventType.Metric -> {
                val messageType = type as? MessageType ?: error("An event page is required to be a MessageType")
                updateEventSummaryPage(messageType = type, context = context)
                writeEventPage(messageType = messageType, context = context, eventType = eventType)
            }

            else -> {
                val enumType = type as? EnumType ?: error("A type page is required to be an EnumType")
                writeTypePage(enumType = enumType, context = context)
            }
        }
    }

    /**
     * These handlers are not used for markdown generation.
     */
    override fun handle(extend: Extend, field: Field, context: Context): Path? {
        return null
    }

    override fun handle(service: Service, context: Context): List<Path> =
        error("This schema handler does not support services")

    private fun updateEventSummaryPage(messageType: MessageType, context: Context) {
        val eventType = requireNotNull(messageType.options.eventType)
        val eventFilePath = docsPath / "home/event_summary.md"

        eventTypeCountMap.incrementCount(eventType)
        metricEventTypeCountMap.maybeIncrementMetricEventTypeCount(messageType)
        teamEventCountMap.incrementCount(messageType.options.team)
        val eventTotal = eventTypeCountMap.values.sum()
        val eventTypeTableRows = eventTypeCountMap.map { "| ${it.key} | ${it.value} |" }
        val metricEventTableRows = metricEventTypeCountMap.map { "| ${it.key} | ${it.value} |" }
        val teamTableRows = teamEventCountMap.map { "| ${it.key} | ${it.value} |" }

        context.fileSystem.write(eventFilePath) {
            writeUtf8(
"""
# Event Summary

This page includes a summary of all events organized by event type and owners.

## Events by Type

| Event Type | Count | 
|------------|-------|
${eventTypeTableRows.joinToString(separator = "\n")}
| **Total**   | **$eventTotal** |

## Metric Events by Type

| Event Type | Count | 
|------------|-------|
${metricEventTableRows.joinToString(separator = "\n")}

## Events by Team

| Team	 | Count	 | 
|--------|-----------|
${teamTableRows.joinToString(separator = "\n")}
""".trimIndent()
            )
        }
    }

    private fun <T> MutableMap<T, Int>.incrementCount(key: T) {
        if (this.contains(key)) {
            val incrementedCount = requireNotNull(this[key]) + 1
            this[key] = incrementedCount
        } else {
            this[key] = 1
        }
    }

    private fun MutableMap<String, Int>.maybeIncrementMetricEventTypeCount(messageType: MessageType) {
        if (messageType.options.eventType == EventType.Metric) {
            val metricEvent = messageType.options.metricEvent
            val metricEventKey = metricEvent.name

            this.incrementCount(metricEventKey)
        }
    }

    /**
     * Writes an event message proto to a markdown file located at `docs/event/[eventName].md`. An entry
     * is also added to the mkdocs navigation tree.
     */
    private fun writeEventPage(messageType: MessageType, context: Context, eventType: EventType): Path {
        val eventName = messageType.name
        val eventFilePath = docsPath / "event/$eventName.md"

        context.fileSystem.write(eventFilePath) {
            writeUtf8(generateEventPage(messageType = messageType, eventType = eventType))
        }

        mkdocsConfigEvents.add(MkdocsConfigTemplateData.EventTemplateData(name = eventName))

        updateMkDocsConfig(context = context)

        return eventFilePath
    }

    /**
     * Writes an event type proto to a markdown file located at `docs/type/[enumType.name].md`. An entry
     * is also added to the mkdocs navigation tree.
     *
     * Note, that only [EnumType]'s are supported as custom event properties.
     */
    private fun writeTypePage(enumType: EnumType, context: Context): Path {
        val typeName = enumType.name
        val typeFilePath = docsPath / "type/$typeName.md"

        context.fileSystem.write(typeFilePath) {
            writeUtf8(generateTypePage(enumType = enumType))
        }

        mkdocsConfigTypes.add(MkdocsConfigTemplateData.TypeTemplateData(name = typeName))

        updateMkDocsConfig(context = context)

        return typeFilePath
    }

    /**
     * Updates the current mkdocs.yml.mustache template with the most recent entries in [mkdocsConfigEvents] and
     * [mkdocsConfigTypes]. This may be called multiple times for each event and type proto file that is processed.
     * Each time the config is generated, a timestamp will be placed in the footer too.
     */
    private fun updateMkDocsConfig(context: Context) {
        val mkdocsConfigString = mustacheCompiler.compile(mkdocsMustacheTemplate).execute(object : Any() {
            var event: Any = mkdocsConfigEvents
            var type: Any = mkdocsConfigTypes
            var updateTime: Any = MkdocsConfigTemplateData.LastUpdated.now()
        })

        context.fileSystem.write(mkdocsFile) {
            writeUtf8(mkdocsConfigString)
        }
    }

    /**
     * Generates a markdown type page. Please note that the multi-line string is intentionally left formatted all the
     * way to the left to ensure that tables are rendered correctly.
     */
    private fun generateTypePage(enumType: EnumType): String =
"""
# ${enumType.name}

${enumType.documentation}

### Properties

| Value | Comment |
| --- | --- |
${enumType.constants.joinToString(separator = "\n") { "| `${it.name}` | ${it.documentation} |" }}
""".trimIndent()

    /**
     * Generates a markdown event page. Please note that the multi-line string is intentionally left formatted all the
     * way to the left to ensure that tables are rendered correctly.
     */
    private fun generateEventPage(messageType: MessageType, eventType: EventType): String =
"""
${messageType.toTagsBlock()}
# ${messageType.name}

${messageType.documentation.ifEmpty { 
    if (eventType == EventType.Metric) messageType.options.metricEvent.eventDescription else ""
}}

### Event Type

${messageType.options.eventType?.toEventTypeSection(messageType)}

### Owners

${messageType.options.team}

### Properties

${addPropertiesSection(messageType)}

### Availability
${addAvailability(messageType)}

### Destinations 

${eventType.addDestinationsSection(messageType)}
""".trimIndent()

    private fun MessageType.toTagsBlock(): String {
        val tagsList: String? = fileOptions[location]?.getOption("tags")
        return if (tagsList.isNullOrEmpty()) {
""
        } else {
            val tags = tagsList.split(",")
"""
---
tags:
${tags.joinToString(separator = "\n") { "- ${it.trim()}" }}
---
""".trimIndent()
        }
    }

    private fun EventType.addDestinationsSection(messageType: MessageType): String = when (this) {
        EventType.ActionTap, EventType.ActionCustom, EventType.Error, EventType.Log -> addDataDogSection(messageType)
        EventType.Metric -> addMetricDestinations(messageType)
    }

    private fun EventType.addDataDogSection(messageType: MessageType): String =
"""
**Data Dog**

View events the last seven days.

* <a href="${this.toDatadogLink(messageType = messageType, environment = Environment.Staging)}" target="_blank">Staging</a>
* <a href="${this.toDatadogLink(messageType = messageType, environment = Environment.Production)}" target="_blank">Production</a>
""".trimIndent()

    private fun EventType.addMetricDestinations(messageType: MessageType): String =
"""
${addDataDogSection(messageType)}

${addSnowflakeSection(messageType)}
""".trimIndent()

    private fun EventType.toDatadogLink(messageType: MessageType, environment: Environment): String {
        val eventName = messageType.name
        return when(this) {
            EventType.ActionTap, EventType.ActionCustom -> "https://square.datadoghq.com/rum/sessions?query=%40action.target.name%3A$eventName%20%40type%3Aaction%20env%3A${environment.datadogName.lowercase()}%20&agg_m=count&agg_m_source=base&agg_t=count&cols=&fromUser=true&viz=stream&from_ts=1715135405164&to_ts=1715740205164&live=true"
            EventType.Error -> "https://square.datadoghq.com/rum/sessions?query=%40type%3Aerror%20%40error.message%3$eventName%20env%3A${environment.datadogName.lowercase()}%20&agg_m=count&agg_m_source=base&agg_t=count&cols=has_replay%2Cstatus%2Ctimestamp%2C%40view.name%2C%40error.source%2C%40error.message%2C%40error.is_crash&fromUser=true&sort_by=%40error.message&sort_order=asc&viz=stream&from_ts=1715353984121&to_ts=1715958784121&live=true"
            EventType.Log -> "https://square.datadoghq.com/logs?query=%40square.message%3A$eventName%20%20env%3A${environment.datadogName.lowercase()}%20&agg_m=count&agg_m_source=base&agg_t=count&cols=host%2Cservice&fromUser=true&messageDisplay=inline&refresh_mode=sliding&storage=hot&stream_sort=desc&viz=stream&from_ts=1715354197419&to_ts=1715958997419&live=true"
            EventType.Metric -> {
                when (val metricProperties = messageType.options.metricEvent) {
                    is ClickFeature -> "https://square.datadoghq.com/logs?query=%40square.feature_name%3A${metricProperties.featureName.quoted().toUrlEncodedString()}%20%40square.action_item%3A${metricProperties.actionItem}%20%20env%3A${environment.datadogName.lowercase()}%20&agg_m=count&agg_m_source=base&agg_t=count&cols=host%2Cservice%2C%40square.action_item&fromUser=true&messageDisplay=inline&refresh_mode=sliding&storage=hot&stream_sort=desc&viz=stream&from_ts=1721420260733&to_ts=1722025060733&live=true"
                    is EncounterError, is LoadFeature, is ViewFeature -> "https://square.datadoghq.com/logs?query=%40square.feature_name%3A${metricProperties.featureName.quoted().toUrlEncodedString()}%20%40square.event_description%3A${metricProperties.eventDescription.quoted().toUrlEncodedString()}%20%20env%3A${environment.datadogName.lowercase()}%20&agg_m=count&agg_m_source=base&agg_t=count&cols=host%2Cservice%2C%40square.action_item&fromUser=true&messageDisplay=inline&refresh_mode=sliding&storage=hot&stream_sort=desc&viz=stream&from_ts=1721420260733&to_ts=1722025060733&live=true"
                }
            }
        }
    }
}

private fun EventType.addSnowflakeSection(messageType: MessageType): String =
"""
**Snowflake**

Enter the following Snowflake query to view the last seven days of events in staging or production.

* <a href="https://go/snowflakestaging" target="_blank">Staging</a>
* <a href="https://go/snowflake" target="_blank">Production</a>

```sql
select
    to_char(timestamp, 'YYYY-MM-DD HH24:MI:SS') as time,
    properties_event_description,
${messageType.addMetricTypeSelection()}
    context_application_name,
    context_application_version,
    context_os_name,
    context_subject_merchant_token
from
    customer_data.square_mobile.${messageType.metricTypeSnowflakeTable()}
where
${messageType.addMetricEventFilter()}
    and to_date(timestamp) >= dateadd(day, -7, current_date)
order by
    time desc;
```
""".trimIndent()

private fun MessageType.addMetricTypeSelection(): String {
    val metricTypeQueryParameters = addMetricTypeQueryParameters()
    val additionalParametersQuery = addAdditionalParametersQuery()
    val additionalParametersQueryOrEmpty = if (additionalParametersQuery.isNotEmpty()) {
        "\n$additionalParametersQuery"
    } else {
        ""
    }

    return metricTypeQueryParameters + additionalParametersQueryOrEmpty
}

private fun MessageType.addMetricTypeQueryParameters(): String = when (options.metricEvent) {
    is MetricEvent.LoadFeature -> ""

    is ClickFeature -> "${FOUR_SPACES}properties_action_item,\n" +
            "${FOUR_SPACES}properties_sub_action_item,"

    is EncounterError -> "${FOUR_SPACES}properties_event_called,\n" +
            "${FOUR_SPACES}properties_event_type,\n" +
            "${FOUR_SPACES}properties_error_message,"

    is ViewFeature -> "${FOUR_SPACES}properties_is_default_view,"
}

private fun MessageType.addAdditionalParametersQuery(): String = fields.joinToString(separator = "\n") {
    "${FOUR_SPACES}properties_additional_parameters_${it.name.camelToSnakeCase()},"
}

private fun MessageType.metricTypeSnowflakeTable(): String = when (options.metricEvent) {
    is LoadFeature -> "merchant_load_feature"
    is ClickFeature -> "merchant_click_feature"
    is EncounterError -> "merchant_encounter_error"
    is ViewFeature -> "merchant_view_feature"
}

private fun MessageType.addMetricEventFilter(): String = when (val metricEvent = options.metricEvent) {
    is LoadFeature -> "${FOUR_SPACES}properties_feature_name = '${metricEvent.featureName}'\n" +
            "${FOUR_SPACES}and properties_feature_id = '${metricEvent.featureId}'\n" +
            "${FOUR_SPACES}and properties_event_description = '${metricEvent.eventDescription}'\n"

    is ClickFeature -> "${FOUR_SPACES}properties_action_item = '${metricEvent.actionItem}'\n" +
            "${FOUR_SPACES}and properties_feature_name = '${metricEvent.featureName}'\n" +
            "${FOUR_SPACES}and properties_feature_id = '${metricEvent.featureId}'"

    is EncounterError -> "${FOUR_SPACES}properties_feature_name = '${metricEvent.featureName}'\n" +
            "${FOUR_SPACES}and properties_feature_id = '${metricEvent.featureId}'\n" +
            "${FOUR_SPACES}and properties_event_description = '${metricEvent.eventDescription}'"

    is ViewFeature -> "${FOUR_SPACES}properties_feature_name = '${metricEvent.featureName}'\n" +
            "${FOUR_SPACES}and properties_feature_id = '${metricEvent.featureId}'\n" +
            "${FOUR_SPACES}and properties_event_description = '${metricEvent.eventDescription}'\n" +
            "${FOUR_SPACES}and properties_is_default_view = '${metricEvent.isDefaultView}'"
}

private fun String.quoted(): String = "\"$this\""

private fun String.toUrlEncodedString(): String = URLEncoder.encode(this, UTF_8)

private fun EventType.toEventTypeSection(messageType: MessageType): String = when (this) {
    EventType.Metric -> when (messageType.options.metricEvent) {
        is ClickFeature, is EncounterError, is LoadFeature, is ViewFeature -> "$name : ${messageType.options.metricEvent.name}"
    }
    else -> this.name
}

private fun addPropertiesSection(messageType: MessageType): String = when (messageType.options.eventType) {
    EventType.ActionTap, EventType.ActionCustom, EventType.Error, EventType.Log -> addRumProperties(messageType)
    EventType.Metric -> addMetricProperties(messageType)
    null -> ""
}

private fun addAvailability(messageType: MessageType): String {
    val versionAvailability = messageType.options.versionAvailability
    val androidVersionAvailability = versionAvailability.find { it is VersionAvailability.AndroidVersionAvailability }
    val iosVersionAvailability = versionAvailability.find { it is VersionAvailability.IosVersionAvailability }

return """
| Platform | Version(s)
| --- | --- |
| Android | ${androidVersionAvailability.toVersionsColumn()} |
| iOS | ${iosVersionAvailability.toVersionsColumn()} |
""".trimIndent()
}

private fun VersionAvailability?.toVersionsColumn() = when {
    this == null -> ":warning:"
    this.lastVersion != null -> "${this.firstVersion} - ${this.lastVersion}"
    else -> this.firstVersion
}

private fun addRumProperties(messageType: MessageType): String =
"""
${
if (messageType.fields.isEmpty()) {
"""
No data included with event
"""
} else {
"""
| Label | Type | Name | Comment |
| --- | --- | --- | --- |
${
messageType.fields.joinToString(separator = "\n") {
    "| ${it.label?.name.orEmpty().lowercase()} | ${it.type?.toTypeNameOrLink()} | `${it.name}` | ${it.documentation} |"
}}
"""
}
}
""".trimIndent()

private fun ProtoType.toTypeNameOrLink(): String = if (MetronEventKotlinGenerator.builtInType(this)) {
    "`$simpleName`"
} else {
    "[`$simpleName`](/type/$simpleName)"
}

private fun addMetricProperties(messageType: MessageType): String = when (messageType.options.eventType) {
    EventType.Metric -> {
"""
${messageType.options.metricEvent.toMetricPropertiesSection()}
${messageType.options.metricEvent.toAdditionalParametersSection(messageType)}
""".trimIndent()
    }
    else -> error("${messageType.name} is not a Metric")
}

private fun MetricEvent.toMetricPropertiesSection(): String = when (this) {
    is ClickFeature -> toClickFeaturePropertiesSection()
    is EncounterError -> toEncounterErrorPropertiesSection()
    is LoadFeature -> toLoadFeaturePropertiesSection()
    is ViewFeature -> toViewFeaturePropertiesSection()
}

private fun MetricEvent.LoadFeature.toLoadFeaturePropertiesSection(): String =
"""
| Square One Property | Value |
| --- | --- |
`feature_name` | $featureName |
`feature_id` | $featureId |
`feature_format` | $featureFormat |
`event_description` | $eventDescription |
`feature_parent_id` | ${featureParentId.orEmpty()} |
""".trimIndent()

private fun MetricEvent.ClickFeature.toClickFeaturePropertiesSection(): String =
"""
| Square One Property | Value |
| --- | --- |
`feature_name` | $featureName |
`feature_id` | $featureId |
`feature_format` | $featureFormat |
`event_description` | $eventDescription |
`action_item` | $actionItem |
`sub_action_item` | ${subActionItem.orEmpty()} |
`feature_parent_id` | ${featureParentId.orEmpty()} |
""".trimIndent()

private fun MetricEvent.toAdditionalParametersSection(messageType: MessageType): String =
"""
${
if (messageType.fields.isEmpty()) {
"""
No additional parameters are included with this event.
"""
} else {
"""
### Additional Parameters
| Label | Type | Name | Comment |
| --- | --- | --- | --- |
${
messageType.fields.joinToString(separator = "\n") {
    "| ${it.label?.name.orEmpty().lowercase()} | ${it.type?.toTypeNameOrLink()} | `${it.name.camelToSnakeCase()}` | ${it.documentation} |"
}}
"""
}
}
""".trimIndent()

private fun MetricEvent.EncounterError.toEncounterErrorPropertiesSection(): String =
    """
| Square One Property | Value |
| --- | --- |
`feature_name` | $featureName |
`feature_id` | $featureId |
`feature_parent_id` | ${featureParentId.orEmpty()} |
`feature_format` | $featureFormat |
`event_description` | $eventDescription |
`event_called` | $eventCalled |
`error_type` | $errorType |
`error_message` | $errorMessage |
""".trimIndent()

private fun MetricEvent.ViewFeature.toViewFeaturePropertiesSection(): String =
    """
| Square One Property | Value |
| --- | --- |
`feature_name` | $featureName |
`feature_id` | $featureId |
`feature_format` | $featureFormat |
`event_description` | $eventDescription |
`feature_parent_id` | ${featureParentId.orEmpty()} |
`is_default_view` | $isDefaultView |
""".trimIndent()
