# Events

## Purchase Order YAML

```
PurchaseOrder: # This should be camelcase
  definition: Buy products and/or services from a supplier by sending a purchase order.
  owners:
    - retail-analytics # make sure it has git-sync enabled
    - retail-eng
  actions:
    View:
      definition: View library of existing purchase orders and action options
      events:
        Create:
          comment: Triggers when user chooses to create a new purchase order
          params:
          - object_name : ObjectName # field  names should be snakecase
            comment: Semantics of the object being interacted with
          - object_format : ObjectFormat
            comment: Format of the object being interacted with
          destinations:
          - SNOWFLAKE # Only snowflake is available for this POC
          # - product_event_order : Null # optional
          # - product_event_start_end : End # optional
          - source_database : CUSTOMER_DATA
          - source_schema : DASHBOARD
          - source_table : merchant_create_purchase_order
          - filter : [“properties_action='EDIT'”]

# If you need enums, declare it at the feature level. If you do not need enums, delete the everything below
  types:
    ObjectName: # This should be camelcase. If the enum is 1 word, make sure the first letter is upper cased
      - PO_INDEX: # This should all caps snakecase 
          comment: Description of enum_field_1
      - PO_ELLIPSIS_DROPDOWN: # This should all caps snakecase 
          comment: Description of enum_field_2
      - NEW_PO: # This should all caps snakecase 
          comment: Description of enum_field_3
      - EDIT_PO: # This should all caps snakecase 
          comment: Description of enum_field_4
      - SEND_PO: # This should all caps snakecase 
          comment: Description of enum_field_5
    ObjectFormat: # This should be camelcase. If the enum is 1 word, make sure the first letter is upper cased
      - MODULE: # This should all caps snakecase 
          comment: Description of enum_field_1
      - MODAL: # This should all caps snakecase 
          comment: Description of enum_field_2
      - BLADE: # This should all caps snakecase 
          comment: Description of enum_field_3
```

## Event 2 YAML

```
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
          - field_1 : String # field  names should be snakecase
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

# If you need enums, declare it at the feature level. If you do not need enums, delete the everything below
  types:
    EnumName: # This should be camelcase. If the enum is 1 word, make sure the first letter is upper cased
      - enum_field_1: # This should all caps snakecase 
          comment: Description of enum_field_1
      - enum_field_2: # This should all caps snakecase 
          comment: Description of enum_field_2
      - enum_field_3: # This should all caps snakecase 
          comment: Description of enum_field_3
```