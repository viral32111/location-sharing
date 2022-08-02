# Location Sharing

This is a fully-featured Android application that allows you to share your real-time location via private invite-only virtual groups, ideal for family, friends, work teams, small events, field trips, etc.

## Features

I made this application with the intention for it to be feature-rich and supersede the more general-purpose location sharing mobile applications already on the market, which is why the feature list is quite exhaustive.

* Account registration & login
  * Support for persistent accounts
     * Last until the account holder deletes them.
     * Passwords are required to allow future login attempts.
       * Hashed with PBKDF2 before being stored in the database.
  * Support for temporary accounts
     * Only exist for the duration of a session (i.e. until you logout or the application data is cleared), or up to 30 days.
     * Do not require a password, thus cannot be logged into on another instance or again in the future.
  * No email address or phone number required, purely username-based.
    * **NOTE:** There were plans to require phone numbers originally for SMS-based notifications but this idea was never implemented in the final build.
  * Quickly join groups via group invite code option.
* Location bookmarking
  * Save locations & areas to your account with recognisable names and a configurable radius.
  * Can be used in conjunction with scheduled events.
  * Managed in the *Yourself -> Manage Bookmarks* activity.
  * Viewable on the interactive map.
* Scheduled events
  * Create complex events that trigger and execute actions whenever something happens.
    * Specify a target group member.
    * Specify a saved location from your personal bookmarks.
    * Specify a condition to match (e.g. arrives at location, leaves location, etc.).
    * Specify an action to execute (e.g. send a notification to your device).
  * Only for your account, they wont interfere with events other group members have.
  * Managed in the *Yourself -> Manage Events* activity.
  * **NOTE:** There are future plans to add a time-based options to these, so events can either trigger at certain times or only trigger within a time period.
* Virtual groups
  * Customisable name that shows in the navigation drawer for each member in the group.
  * Private by design, can only join via an invite created by the group's administrator.
  * Administrated by the member that created the group, but ownership can be transferred to another group member.
    * **NOTE:** There are future plans to create a role-based permissions system for better administration.
  * Group members can view a list of other group members (*Group -> View Members*).
    * List shows each members name and last known location (if it is known).
      * Location shows as a bookmark name if it is within the area of one of your personal bookmarks, otherwise it is longitude and latitude.
      * **NOTE:** There were plans to show street names but this was scrapped because the [Google Geocoding API](https://developers.google.com/maps/documentation/geocoding/overview) is not free.
    * Group administrator has the option to kick (remove) individual members from the group.
  * Invite codes can be managed by the group administrator (*Group -> Manage Invites*).
    * Newly created codes are randomly-generated and expire automatically after 30 days.
    * Shows the invite code, the date it was created, and countdown until when it expires.
    * Individual invites can be deleted at any time.
  * Group options can be managed by the group administrator  (*Group -> Manage Group*).
    * Change the name of the group.
    * Bulk remove all invite codes, or bulk kick all members.
    * Transfer ownership (administrator abilities) to another group member.
    * Delete the group (deletes all invites, kicks all members, and erases it from the database).
  * Creatable from the account management activity.
  * **NOTE:** At present a user can only join one group, but there are future plans to introduce multi-group support, but this would require a huge rewrite.
* Account management
  * Shows date account was registered.
  * Shows when the account expires, if the account is a temporary one.
  * Easily change your username & password.
  * Create a new group, join an existing group or leave your current group.
    * Specify group name for first one, and group invite code for second one.
  * Convert temporary account to a permanent account (requires entering the new account password first).
  * Delete account (leaves current group, removes all bookmarks & events, erases credentials, location history and other information from the database)
* Interactive Google Maps embed
  * Live red marker that indicates your real-time location as other members in your group see it.
  * Live orange markers that indicate the real-time locations of other members in your group.
  * Live purple circles that indicate locations & area radius of your personal bookmarks.
  * Natural panning & pinch-to-zoom gestures.
  * Markers and circles refresh every time a location update occurs.
* Location history
  * Location is recorded/updated every minute in the background, if your location has changed.
    * When registering or logging into an account for the first time, a prompt will show to grant either approximate or precise location permissions.
    * Locations are always AES encrypted before being stored in the database, for this and other features of the application.
      * **NOTE:** There are future plans to implement zero-trust security by deriving the encryption key from the account password, or randomly generated for temporary accounts.
  * Viewable as a list of entries in the *Yourself -> View History* activity.
  * Entries that show everywhere you have been with a small, non-interactive Google Maps embed.
  * Shows longitude and latitude unless the location matches a bookmark on your account, then it shows its name.
  * Red marker on the map identifies the exact recorded location.
  * **NOTE:** This activity gets very laggy and can sometimes crash if there are many history entries, this will be fixed in the future via dynamic lazy-loading as the user scrolls.
* User-friendly design & interaction
  * Helpful popups when theres an issue with something.
  * Confirmations for dangerous actions.
  * Follows Android styling conventions to maintain familiarity.

## To-Do List

Other than the notes in the feature list above, there are other areas that need to be addressed at some point in the future.

* Cannot sign-out of an account, only way to do it at present is to clear the application's data in Android settings.
* Cannot export or download user data. Needed to comply with data protection regulations, but would require additional permissions to access device files for saving a file.
* Custom message/prompt before permissions prompt to inform user about why the permission is required and what happens if they do not grant it.
* Create a RESTful API with proper authentication for exchanging data between the application and the database.
  * At present the application connects directly to the MySQL database via a Java MySQL connector and executes all queries itself, this was fine for demonstration purposes but is not safe for practical use.
  * This also means the database credentials will not be embedded into the application file.
* Branding for the application (icon, Play Store metadata, etc.).
* Better colour scheme as the current monochrome/grayscale is not appealing.
* Terms of Service and Privacy Policy activities within the application accessible from both the registration & login activity, and the navigation drawer.
* Cross-platform compatibility so an iPhone/iOS release can be made.
* OAuth integration for easier account login.
* Rewrite entire application in Kotlin because Java sucks :)

## Background

This was originally a project that I started over a year ago for my degree at University.

I wanted to create this application because in the past I have never had any major experience developing a full-scale Android application, nor had I that much Java experience outside of Minecraft mod & plugin development.

## Requirements

This application requires a MySQL database using [the provided structure](/Database.sql) that all instances of the application are able to connect to, the address and credentials of this database should be configured in the Gradle `local.properties` file with the following keys referenced in the Android manifest:

* `DATABASE_HOST` is the hostname/IP address of the MySQL server (e.g. `192.168.0.5`).
* `DATABASE_PORT` is the port number of the MySQL server (e.g. `3306`).
* `DATABASE_NAME` is the name of the MySQL database (e.g. `LocationSharing`).
* `DATABASE_USER` is the name of the MySQL user that has full access to the database.
* `DATABASE_PASSWORD` is the password for the aforementioned user.

In addition to the above, the following properties are also required:

* `ENCRYPTION_KEY` must be set to a randomly generated string to be used when performing cryptographic operations. 
* MAPS_API_KEY` must be set to the [Google Maps API](https://developers.google.com/maps/) key from the [Google Cloud APIs & services dashboard](https://console.cloud.google.com/apis/dashboard).

## Acknowledgements

The following projects and online sources have been used during development of this application.

* [Google Maps SDK for Android](https://developers.google.com/maps/documentation/android-sdk/overview)
* [MySQL Connector for Java](https://dev.mysql.com/downloads/connector/j/)
* [SimpleLagLng library by JavadocMD](https://github.com/JavadocMD/simplelatlng)
* Documentation from GeeksForGeeks, MySQL Tutorial, Google Developers, W3Schools, StackOverflow, and many more websites.

I have tried my best to add link comments in the code that point to where I got help with said portion of code, but I apologise if I missed any as the codebase is quite big.

## License

Copyright (C) 2021-2022 [viral32111](https://viral32111.com).

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see https://www.gnu.org/licenses.
