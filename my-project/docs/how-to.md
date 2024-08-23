# CDF POC How-To Guide

## Identify 2+ jobs

Identify 2+ jobs for your product that follows the below criteria:

1. These events should aid in the calculation of business metrics and guide product decision-making
2. Jobs/features should be a good representative sample of the product ecosystem that you are responsible for
3. Include at least one Job that spans across platforms such Desktop/Mobile or Dashboard/POS
4. [Nice to Have] Incorporate shared features that also depend on collaboration with other teams for development and ownership
5. Recommended: You can pick jobs from the [go/audience-jtbd](http://go/audience-jtbd) if they already satisfy the aforementioned criteria

## Insert Events into Tracking Plan

Put them [here](http://go/uedc-poc-features) for the POC. This step lets you capture the events you want to build before subsequent steps. You can use this list to ensure you know which Event schema to use (or if you need to request changes to those schemas.)  It can also act as a checklist between Product, Engineering, and Data Consumers to ensure all parties know the important touchpoints to instrument and evaluate. You will have a second time to review your planned events in GitHub later.

## Identify or create event schemas

Checkout our git repo **recursively** (to include submodules) [here](https://github.com/squareup/message-schemas):

    git clone --recurse-submodules org-49461806@github.com:squareup/message-schemas.git

For each event in your product tracking plan,

### Check for an Existing Event

CDP provides a UI to catalog all existing event schemas. Before creating new events, check if your plan can be served by these events at [go/ueschema](http://go/ueschema).

For advice on your use case and how to apply these Schemas, contact us in [#unified-eventing-data-council](https://square.slack.com/archives/C06H6Q8FKB2) and on call can advise.

### Create a New Event or Update Existing Event

#### Create a new Event for a new Feature

First, create a directory for your feature in message-schemas/cdp_events. The directory name should be **snake case**.

For Example, if your feature is “HomeModule” then the directory should be:

    message-schemas/cdp_event/home_module

The file name for your feature yaml should also be snake case. For the feature HomeModule, the resulting filename should be:

    message-schemas/cdp_event/home_module/home_module.yaml

Copy the template for creating your feature yaml and replace the red text with the actual values for your events. Details of each field will be covered below.

```yaml
FeatureName: # This should be camelcase
definition: description of the feature
owners:
  - registry-group # make sure it has git-sync enabled
actions:
  ActivityName: # This should be camelcase
    definition: description of the activity
    events:
      EventName: # This should be camelcase
        comment: description of the event
        params:
          - field_1 : String # field  names should be snake case
            semantic_types:
              - namespace: CONSUMER
                name: FIRST_NAME
                rubric_version: 0
            comment: Description of field_1
          - field_2 : String
            comment: Description of field_2
          - field_3: EnumName
            comment: Description of field_3
        destinations:
          - SNOWFLAKE # Only snowflake is available for this POC

# If you need enums, declare it at the feature level. If you do not need enums, delete everything below
types:
  EnumName: # This should be camelcase. If the enum is 1 word, make sure the first letter is uppercased
    - enum_field_1: # This should all caps snake case 
      comment: Description of enum_field_1
    - enum_field_2: # This should all caps snake case 
      comment: Description of enum_field_2
    - enum_field_3: # This should all caps snake case 
      comment: Description of enum_field_3
```

#### Yaml Specification

##### Feature

The FeatureName key should be replaced with the actual feature name. Feature has the following properties:
- **definition**: Description of the feature
- **owners**: List of [registry group](https://registry.sqprod.co/groups) owners for the feature. Owner teams are responsible for all events defined in the feature file. They will be required to approve changes to these schemas. Make sure git-sync is enabled for the registry group.
  - Example registry group: [marketing-automation](https://registry.sqprod.co/groups/marketing-automation)
- **actions**: List of activities

##### Actions

List of activities. Refer to [Square Eventing Framework proposal](https://docs.google.com/document/d/1ohbqdCwgTAlDkoaE0uMUOixC0PFTj8l5xdFipBum9lk) for details on feature and activities

##### Activity

The ActivityName key should be replaced with the actual activity name. Activity has the following properties:

- **definition**: Description of the activity
- **events**: List of events

##### Event

The EventName key should be replaced with the actual event name. An event block has the following properties:

- **comment**: Description of the event
- **destination**: List of destinations to sync the event to (**only Snowflake is supported** as of August 2024)
- **params**: Map of fields to their definition

##### Param

A param should be thought of as an event field. Each param has the following properties:
- type: Field value type
  - Supported primitive types ([details](https://github.com/squareup/message-schemas/blob/main/cdp_events/cdf.yaml#L7))
    - String
    - Int32
    - Int64
      - For timestamp fields it is recommended to use Int64 type to represent an epoch millisecond timestamp
    - Bool
    - Double
  - Can be an enum; more details in the [enum section](#enums)
  - Can be an array of one of the primitives or enum; just add brackets around the type. You cannot have an array of mixed types. Array has to store elements of the same type. Example:

```yaml
Start: # AccountCreateStart
  comment: User started create account flow
  params:
    - merchant_categories : [String]
      comment: list of merchant categories
```

- **comment**: Description of the field
- **semantic_types**: List of semantic information relating to the field, which is used to determine PII level. Refer to the [DSL rubric](https://github.com/squareup/dsl-framework/blob/main/rubrics/releases/for-dsl-engine/rubrics_v0.json) for possible values.
  - **namespace**: Semantic types are grouped by namespace, which are the top level keys in the rubric json. Currently available namespaces:
    - [PCI](https://github.com/squareup/dsl-framework/blob/main/rubrics/releases/for-dsl-engine/rubrics_v0.json#L3)
    - [CONSUMER](https://github.com/squareup/dsl-framework/blob/main/rubrics/releases/for-dsl-engine/rubrics_v0.json#L151)
    - [MERCHANT](https://github.com/squareup/dsl-framework/blob/main/rubrics/releases/for-dsl-engine/rubrics_v0.json#L1011)
    - [EMPLOYEE](https://github.com/squareup/dsl-framework/blob/main/rubrics/releases/for-dsl-engine/rubrics_v0.json#L2024)
  - **semantic_type**: Describes the general contents of data in a field. Choose from the semantic types defined in the dsl rubric 
  - **version**: Version of the DSL rubric definitions

```yaml
Start:
  params:
    - first_name: String
      comment: first name of person owner of the merchant
      semantic_types:
        - name: FIRST_NAME
          namespace: consumer
          rubric_version: 0
```

##### Enums

Params can have enum types. For enums that are only used in 1 feature, they should be declared in the same feature file. You should declare the enum in the types list of your feature:

```yaml
FeatureName:
  definition:
  owners:
  actions:
    ...

  types:
    Status:
      - INACTIVE
        comment: account was deactivated by owner
      - ACTIVE
        comment: account is active and accepting payments
    Level:
      - PRIMARY
        comment: first level of naming hierarchy
      - SECONDARY
        comment: second level of naming hierarchy
```

###### Global Enums

If you want to reuse your enums across multiple features, you will need to first declare them in the [message-schemas/cdp_events/cdf.yaml](https://github.com/squareup/message-schemas/blob/main/cdp_events/cdf.yaml#L43) file. There is a custom_types block that is a list of global enum types; add yours to this list:

```yaml
cdf:
  custom_types:
    CurrencyCode:
      - USD
      - GBP
      - JPY
      - BTC
      - CAD
```

Once you have added your global enum to custom_types list, you can reference it in your feature files and use the enum as the value type for your event fields:

```yaml
Account:
  definition: Square Merchant Account
  owners:
    - marketing-automation
  actions:
    Create:
      definition: Create Square account
      events:
        Start:
          comment: Started square account creation
          params:
          - currency_code: CurrencyCode
            comment: The currency code used by the merchant
```

#### Create a new Activity or Event for an existing Feature

If the feature already exists, you can add a new activity to the actions array. Likewise, you can also add a new event to the events field of any activity.

## Commit, Push, and Open PR

Once you are finished editing your feature yaml. Run through the normal git flow of “git add” then “git commit” with a commit message describing the event you have added or updated:

    git add <event yaml>
    git commit -m "added X event"

For all schema changes, Our system has a pre-commit hook set up for code generation. When you commit, you will see additional files checked in automatically. This includes:
- avro schemas
- proto schemas
- A JSON map used internally for routing
- OWNERS.yaml

!!! warning

    To ensure these files are correctly updated and integrated, please avoid skipping the pre-commit hook (occurs when `--no-verify` flag is set or the GitHub web editor is used)!


Once the “git commit” command finishes successfully, push your changes:

    git push

You should now be able to open a PR in the [message-schemas](https://github.com/squareup/message-schemas) repo.

If your yaml is not valid, the pre-commit hook should log errors on what needs to be fixed. Repeat the flow:

1. Make changes
2. `git add` changes
3. `git commit` changes
    1. If `git commit` is successful, then `git push` and open PR
    2. If `git commit` is unsuccessful, start from (1)

If it is not clear what the error is, feel free to reach out to [#cdp](https://square.slack.com/archives/CT2R75XHA) for help.

## Wait for Approvals and Infrastructure
After your PR has been approved and successfully passed all checks, you are ready to merge it into the main branch. **You will need to manually merge your PR!** Merging kicks off the [runway pipeline](https://buildkite.com/runway/schema-validation-sync/builds/28#_) which handles a few prerequisites to send your events, which includes:
- Schema validation checks
- Schema upload to S3 bucket
- Schema syncing to confluent
    - Via automated PR [like this one](https://github.com/squareup/tf-confluent-v2/pull/1402) against tf-confluent-v2 repo to apply schema changes and provision kafka infrastructure

You can view your new event definition in [our GitHub Docs](http://go/ue-schemas)! 

It is recommended that you leave a few hours for Kafka infrastructure to be provisioned by terraform. Unfortunately, DSI owns the terraform build and only their team has access to view the progress.

The code gen pipeline uploads new events to Artifactory once a day around midnight. Regardless if there are any new events, the job will run every night and create a new artifact. Once this job completes, you will be able to move onto step 6. Essentially, you will have to wait until the next day to start instrumenting with your code gen classes. The CDP team is working on improving this turnaround time for general release.

(For the POC, you can check these repositories to find your change and check on status or if anything went wrong, tf-confluent-v2, also feel free to ping #ue-prototype for speedy help.)

It is recommended that you leave a few hours for Kafka infrastructure to be provisioned by Terraform. [DSI](https://square.slack.com/archives/C05LGEYS1GC) owns the Terraform build and only their team has access to view the progress.

**The code gen pipeline uploads new events to Artifactory once a day around midnight.** Regardless of whether there are any new events, the job will run every night and create a new artifact. Once this job completes you will be able to move onto the next step. Essentially, you will have to wait until the next day to start instrumenting with your code gen classes. The CDP team is working on improving this turnaround time for general release.

(For the POC, you can check these repositories to find your change and check on status or if anything went wrong, [tf-confluent-v2](https://github.com/squareup/tf-confluent-v2/); also, feel free to ping [#ue-prototype](https://square.slack.com/archives/C072CB6B5JA) for speedy help.)

## Instrument Events in your Applications (through Staging)

### Web Engineers

Once a schema has been published, a web engineer will then need to:

1. Add or Update `@squareup/message-schemas-web` dependency to use the latest version
2. Call `trackWithSchema` in application code:

```javascript
const accountEvents = AccountCreateStart({
    accountAge: 17,
    accountType: "Kevin's test account",
    currencyCode: 'USD',
    firstName: 'Kevin',
    hasEmployees: false,
    squareAccount: true,
    statuses: ['ACTIVE', 'FROZEN'],
});

trackWithSchema(accountEvents);
```

### iOS Engineers

Once a schema has been published, an iOS engineer should:

1. Update the Pod (MessageSchemas) that contains all the generated event structures. In a terminal, and within the root of the ios-register repo, run the following:

       be pod update MessageSchemas

2. Add the following dependencies to your Bazel module:

        ```yaml
        sq_apple_framework()
            ...
            deps = [
            "//Frameworks/UnifiedEventing/Public:UnifiedEventing",
            "//Frameworks/Metron/Public:Metron",
            "//Pods/MessageSchemas",
        ],
        ...

3. Add the imports at the top of the file where you intend on logging:

        import MessageSchemas
        import Metron
        import UnifiedEventing

4. Call `Log.cdpv2` in your code:

        Log.cdpv2(
            BetaLabsRenderView(
                feature_id: 'test-feature-id',
                feature_format: 'feature-format',
                feature_name: 'Beta Labs Feature name',
                is_default_view: false,
                event_description: 'test event description',
                feature_parent_id: 'test-feature-parent-id',
                feature_layout: 'test-feature-layout',
                is_task_completed: false
           )
        )

### Android Engineers

1. Once a schema has been published (may take 24h), it will appear in one of the published compiled libraries here,
2. Update the event library to the latest version. You can view the latest version number here 
   1. Open dependencies.gradle
   2. Update unified_eventing_events to the new version
   3. Don’t sync your IDE yet!
3. If necessary, add a reference from your module to the events library. Add api deps.unified_eventing_events to your build.gradle.
4. Sync your IDE to have the latest version of the events available.
5. Send via one of the following methods. The first two use a lambda to defer or avoid construction of the event when it’s going to be filtered or sampled out. The third is an extension method in Metron that calls the static method.
   1. Metron.cdpv2 { MyEvent(param1, param2) }
   2. metronLogger.cdpv2 { MyEvent(param1, param2) }
   3. MyEvent(param1, param2).log()

## Validate Data in Snowflake

In the POC, data will land in [go/stagingsnowflake](http://go/stagingsnowflake).

After the message-schemas post-merge workflow completes a follow-up workflow is kicked off to update the kafka-snowflake connector to include (or remove) the updated topics. Once this is complete the pipeline is ready for events to be written to snowflake.

### Event tables

Example event: **HomeModule_Render_View**
Table: CUSTOMER_DATA.GLOBAL.HOME_MODULE_RENDER_VIEW
Query:

    SELECT * FROM CUSTOMER_DATA.GLOBAL.HOME_MODULE_RENDER_VIEW; 

Example query:

    SELECT record_content:entity_id AS entity_id, record_content:properties:feature AS feature 
    FROM customer_data.global.home_module_render_view
    WHERE record_content:entity_type = 'merchant';

### Aggregate tables

The aggregate table is built off of a dynamic table. The creation of this table is not automatic and requires manual intervention at this phase of the project. In future iterations this will become automated.

Table: CUSTOMER_DATA.GLOBAL.TRACK
Query:

    SELECT * FROM CUSTOMER_DATA.GLOBAL.TRACK;

Example query:

    SELECT *
    FROM CUSTOMER_DATA.GLOBAL.TRACK
    WHERE record_content:event_name = 'account_create_start'

### PII

The CDP to Snowflake pipeline is currently not set up to handle PII. Fields annotated with a semantic_type, will be set to null in Snowflake. We are working on setting up our Snowflake pipeline to handle PII and should have this available shortly.