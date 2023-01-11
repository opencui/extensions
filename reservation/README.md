## Google Calendar
This provides a way for user to add events to their Google Calendar.It also allows
for checking availabilty of resource at a certain time, canceling and rescheduling.

## Requirements
- Google Workspace account 
- Google Calendar API enabled 
- A service account with domain wide authority and scopes for google directory(Admin Sdk)+ Calendar API

Resources:
- [Google docs](https://developers.google.com/calendar/quickstart/java)
- [Interface/Module](https://build.opencui.io/org/6343b48f7b5ac3b280b6f5e1/agent/63b50c47fb84b020c72ba4c4/service_schema)
- [Google Calendar API](https://developers.google.com/calendar/v3/reference)
- [Google Directory API](https://developers.google.com/admin-sdk/directory/v1/reference)

## Setup

configuration Meta:

```json
[
  {
    "key": "client_secret",
    "label": "client_secret",
    "type": "String"
  },
  {
    "key": "calendar_id",
    "label": "calendar_id",
    "type": "String"
  },
  {
    "key": "day_range",
    "label": "day_range",
    "type": "String"
  },
  {
    "key": "time_range",
    "label": "time_range",
    "type": "String"
  },
  {
    "key": "open_hour",
    "label": "open_hour",
    "type": "String"
  },
  {
    "key": "close_hour",
    "label": "calendar_id",
    "type": "String"
  },
  {
    "key": "delegated_user",
    "label": "delegated_user",
    "type": "String"
  }
]
```
### 
- day_range is meant to provide number of days ahead that we can check for an event eg from today, we check the next 5days
- time_range is meant to provide for  plus hours from the start of an event to the end. This should be common for all events for consistency
- close_hour is the end of business hours. Should be a single number between 1-24
- open_hours is the start of business hours. Should be a single number between 1-24
- delegated_user is the owner of the domain eg admin@example.com
- calendar_id should be "primary"
- client_secret is the secrets.json string for a service account
provider ClassName:
`services.opencui.reservation.ReservationProvider`

Implementation:
`io.opencui.extensions:reservation`


## Making a resource on Google Admin
[Reference](https://app.tango.us/app/workflow/Workflow-with-Google-888c56e14df64771ac6adef20ae55ea6)

1. Create a building. The concept of buildings in google calendar is that they represent a physical location.
2. Create a resource and choose the building it belongs to.
3. Make sure to fil in the recommended items as shown in the shared link above


## Note
We need to grant a service account with the following authorities:
- https://www.googleapis.com/auth/calendar
- https://www.googleapis.com/auth/admin.directory.resource.calendar

To grant domain wide authority:
[Reference](https://developers.google.com/identity/protocols/oauth2/service-account#delegatingauthority)