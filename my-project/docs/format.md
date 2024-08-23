# Event Definition Format

Event definitions live in the `/events` directory, and each Entity gets its own subdirectory and YAML definition file.

## Global CDF Configuration

A file named `cdf.yaml` located in the root events directory can be used to configure global settings for CDF.

### `types`

Type: [`Enum`](#custom-enums)

A set of custom enum definitions that are globally scoped.
Any event can reference these enums.

### `tags`

Type: [`TagDeclaration`](#tag-declaration)

A set of declarations for all the tags that can be applied to events. The keys of this object are the tag identifiers
that are used in the Events to refer to a specific tag. By convention, the keys/identifiers should be written in kebab-case.

```yaml
tags:
  my-tag:
    name: This is My Tag
    description: The description goes here
```

### `destinations`

Type: [`DestinationDeclaration`](#destination-declaration)

A restricted set of event destinations. An event will only be sent to destinations defined in this list.

**NOTE: MAINTAINED BY THE CDP TEAM. DO NOT ADD/REMOVE/MODIFY**.

## Entities

Entity names should be a high-level noun written in "upper" camel case, and should go in an event definition file that
matches the name
of the entity (written in snake_case).

Compiler Source: [`compiler/src/.../config/Entity.kt`](https://github.com/squareup/cash-cdf/blob/main/compiler/src/main/kotlin/app/cash/cdf/config/Entity.kt)

### `description`

Type: `string`

A description of what the Entity represents.

### `actions`

Type: [`Action`](#actions-1)

A set of action definitions for this entity. See the section of Action definitions below.

### `types`

Type: [`Enum`](#custom-enums)

A set of custom enum definitions that are scoped to this Entity.
Only Events that are contained within this Entity can reference these Enums.

```yaml
# events/my_entity/my_entity.yaml
MyEntity:
  description: "A description of My Entity"
  actions:
    MyAction: ...
  types:
    MyType: ...
```

## Actions

Actions named should be a high-level verb and should also be named in "upper" camel case.

Compiler Source: [`compiler/src/.../config/Action.kt`](https://github.com/squareup/cash-cdf/blob/main/compiler/src/main/kotlin/app/cash/cdf/config/Action.kt)

### `definition`

Type: `string`

A description of what the Action represents.

### `events`

Type: [`Event`](#events-1)

A set of event definitions for this action. See the section on Event definitions below.

### `deprecated` (optional)

Type: `string`
Default: `null`

Actions can be marked as deprecated when they are no longer needed by adding this property
with a message explaining why the action was deprecated, or what replacements should be used in its place.

### Example

```yaml
# events/my_entity/my_eneity.yaml
MyEntity:
  actions:
    MyAction:
      definition: "A description of My Action"
      deprecated: "This action should no longer be used because[...]"
      events:
        MyEvent: ...
    AnotherAction: ...
```

## Events

Event names should be a specific verb(+noun) written in "upper" camel case.

Compiler Source: [`compiler/src/.../config/Event.kt`](https://github.com/squareup/cash-cdf/blob/main/compiler/src/main/kotlin/app/cash/cdf/config/Event.kt)

### `comment`

Type: `string`

A description of what the Event represents, including scenarios which may trigger this event to be recorded.

### `owners`

Type: `list<string>`

A list of event owners. Preferably a team name or slack channel, but LDAPs work as well. Will be displayed on the event page in [go/$cdf](go/$cdf).

### `params`

Type: [`list<EventParam>`](#event-params)

A list of data parameters that this Event contains. See the Event Params section below for more details.

### `tags`

Type: `list<string>`

A list of tag identifiers listing which tags are applied to this Event. See the [global configuration](#tags) for 
details.

### `types`

Type: [`Enum`](#custom-enums)

A set of custom enum definitions that are scoped specifically to this event.
Other event definitions can not reference Enums that are defined here.

### `destinations`

Type: [`list<string>`]

This list of destinations will correspond to where the cdp-proxy client will send your Event. See the [global configuration](#destinations) for
details.

### `deprecated` (optional)

Type: `string`
Default: `null`

Events can be marked as deprecated when they are no longer needed by adding this property
with a message explaining why the event was deprecated, or what replacements should be used in its place.

### Example

```yaml
# events/my_entity/my_entity.yaml
MyEntity:
  actions:
    MyAction:
      events:
        MyEvent:
          comment: "A description of MyEvent"
          deprecated: "This event is deprecated because[...]"
          params: [ ... ]
          types: ...
        AnotherEvent: ...
```

## Event Params

The parameters of an Event are defined as a list of Event Param objects.
The first key in the Param object is the name of the parameter, and its value is the type of the parameter.

Compiler Source: [`compiler/src/.../config/Param.kt`](https://github.com/squareup/cash-cdf/blob/main/compiler/src/main/kotlin/app/cash/cdf/config/Param.kt)

### `name: type`

The name and type of the property. The `type` of the property can be any [built-in type](#built-in-types) or an `Enum`

#### Nullable Types

By default, all event parameters are optional and have a default value of `null`. To mark an event parameter as required,
simply add `required: true` to that parameter. The generated code will enforce that a value must be passed for that
parameter.

Making a parameter required will make that parameter non-nullable in the generated code. To allow a null value to be
passed to a required parameter, simply add a `?` after the parameter's type in the CDF definition.

```yaml
# Event definition
ViewTile:
  params:
    # Required parameter, can not be null
    - app_location: AppLocation
      required: true
    # Required parameter, but this one can be null  
    - referrer_flow_token: String?
      required: true
    # Optional parameters, both can be null (the ? is optional here)  
    - row: Int32
    - col: Int32?
```

### `comment`

Type: `string`

A comment describing what this parameter represents.

### `required` (optional)

Type: `boolean`  
Default: `false`

If set to true, this parameter will be required when the event is sent.
The generated code for this parameter will be a non-null (or non-optional) parameter.

Note: a parameter can not be both deprecated _and_ required, and the compiler will report an error if this happens.

### `deprecated` (optional)

Type: `string`
Default: `null`

Event Parameters can be marked as deprecated when they are no longer needed by adding this property
with a message explaining why the parameter was deprecated, or what replacements should be used in its place.

Note: a parameter can not be both deprecated _and_ required, and the compiler will report an error if this happens.

## Built-In Types

The following types are built-in and can be specified on any event parameter:

`String`, `Int32`, `Int64`, `Bool`, `Double`, `Int`.

## Custom Enums

Custom Enums allow you to restrict the values of a parameter to a predefined set of values.
These types can be scoped globally, at the entity level, and at the event level.

Enums are defined with a list of possible entries, which are typically written in all UPPERCASE snake case.

```yaml
types:
  MyEnum:
    - OPTION_A
    - OPTION_B
    - OPTION_C 
```

### Adding Comments to Enums and Entries
The default format for defining Enums is a short form. Adding comments requires a longer format.

```yaml
types:
  MyEnum:
    comment: An explanation of this Enum
    # The entries are defined with a dedicated property here
    entries:
      # The colon after the entry name is required here to allow the comment property to be specified
      - A:
        comment: description of what entry "A" means
      - B
      - C
```

### Deprecating Enums and Entries

Like comments, this requires the longer definition format.

### Adding Comments to Enums and Entries
The default format for defining Enums is a short form. Adding comments requires a longer format.

```yaml
types:
  MyEnum:
    deprecated: An explanation of this deprecation
    entries:
      # The colon after the entry name is required here to allow the comment property to be specified
      - A:
        deprecated: description of why this is deprecated
      - B
      - C
```

## Tag Declaration

A tag declaration contains the name and description of a tag. These will be displayed in the generated documentation
pages.

```yaml
tags:
  my-tag:
    name: This is My Tag
    description: The description goes here
```

Tags must be declared in the global configuration to help ensure that every tag referenced by an event actually exists.

## Destination Declaration

A destination declaration contains the name and description of a Destination. A list of Destinations will be defined for each event that will determine where the Cdp-Proxy client will send it.

For [*cash_events*](https://github.com/squareup/cash-cdf/tree/main/cash_events):
```yaml
  destinations:
    SEGMENT:
      description: Segment is the Customer Data Platform (CDP) platform at Cash App. 
                   With Segment, we can track events that happen when a user interacts with Cash App on mobile, web or in our servers. 
                   Segment is able to forward events to downstream destinations including analytics tools, data warehouses, and marketing platforms. 
                   In addition, Segment offers a suite of Marketing tools that allows the creation and management of Audiences, Journeys and user traits.  (go/segment)
    AMPLITUDE:
      description: Amplitude Analytics is used to track user data and gain insights into user engagement, behavior, retention, and revenue. 
                   These tools help answer questions regarding our customers about what happened, why it happened, and which actions to take next. 
                   Amplitude Experiments is also the recommended solution at Cash App to run experiments.  (go/amplitude)
    SNOWFLAKE:
      description: Snowflake is used as the primary data warehousing solution across Cash App (go/snowflake)
    KAFKA:
      description: Custom Kafka topic in CDP Proxy that allows consumption of events for custom business logic.
```

Declared in the global configuration to help ensure that every destination referenced by an event is allowed.

**NOTE: MAINTAINED BY THE CDP TEAM. DO NOT ADD/REMOVE/MODIFY**.
