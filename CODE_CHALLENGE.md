# Code Challenge: The Notifications Problem

## Background

In our app, we send push notifications to users based on their preferences. Users can subscribe to one or two
categories of notifications, by activating the corresponding toggles in the app. For historical reasons, we are using
notification types under the hood.

* **Category A**: Includes notification types **type1, type2, type3**.
* **Category B**: Includes notification types **type4, type5**.

When they subscribe, they call:
_POST /register { "userId": "UUID",  "notifications": ["type1", "type2", "type3"] }_  
They always register to all types of a category, and we store it in the table user_notifications.

To send a notification, we receive a request:
_POST /notify { "userId": "UUID",  "type": "type1" }_  
The notification is sent only if the user is subscribed to that notification type.

## The Problem

As new notification types are added to a category, say, **type6** is added to **Category A**, users subscribing to that
category for the first time, will now register to notifications of **type1, type2, type3**, and **type6**.
Previous notification types are guaranteed to stay within each category.
    
Users previously subscribed to **Category A**, won’t automatically receive notifications of **type6**, as they weren’t
registered to this type in the table; but the intended behaviour is that they receive all notifications of the category
they are subscribed to.

## The Task

Marketing just added the **type6** to **Category A**, and our PO brought the request to the team of designing
and coding a solution, so users subscribed earlier to that category (all users subscribed to type1), can now receive
notifications of type6. They also told us, that they’ll be bring new types twice a year from now on.

1. **Design a long-term solution** to handle new notification types with minimal effort.
2. **Implement the solution** in the given project.
3. **Ensure existing users** can receive notifications for new types within their subscribed categories.

## Considerations

* Categories **Category A** and **Category B** are unlikely to change, and their notification types are ever growing.
* Users subscribed to a newer type, such as **type3** are guaranteed to be subscribed to earlier types **type1** and,
**type2** on that category.
* The database contains examples of users registered before and after new types were added.
* The endpoints are already used by a considerable number of clients.
* The code in NotificationSubscriber.kt is for illustrating the heavy use of the code base; a kafka implementation
is **not** needed for the code challenge.