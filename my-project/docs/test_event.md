# Test event 1

## Purchase Order

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