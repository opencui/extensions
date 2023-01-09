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
    "key": "delegated_user",
    "label": "delegated_user",
    "type": "String"
  }
]
```
provider ClassName:
`services.opencui.reservation.ReservationProvider`

Implementation:
`io.opencui.extensions:reservation`

## ShortComings

- Currently, we are not able to figure out how to  get the available days for a resource i.e without knowing the business hours.We are not able to get the list of resources available in the calendar.

  Listing availability without knowing business hours is hard to do dynamically. We can only get the list of resources available in the calendar.

  I considered the `freebusyrequest` but that does not serve the purpose well. Still looking for a way to do this.


