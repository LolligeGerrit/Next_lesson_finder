## 1.0.3
First release

## v1.1.0
_Changes:_
 - Increased max time delay, 15 -> 30.
 - Moved the llnNummer input to the authenticate tab.
 - Added "Indicate free hour" switch, which shows the start time instead of the location on your complication if you have a free hour.
 - Clicking on the complication now opens the app.
 
_Bugfixes:_
 - Fixed "Xx" displaying instead of "Done" during holiday.
 - Fixed "Xx" displaying instead of "Done" after the last lesson of the week.
 - Fixed Time being 10 minutes ahead in app.

## v1.1.1
_Bugfixes:_
 - Fixed the app not opening on complication click.

## v1.1.2
_Bugfixes:_
 - Fixed next lesson showing on a sunday.
 - Fixed done not displaying in the app when the day is done.
 - Fixed app crashing when no location is assigned to a lesson.
 - Fixed "Indicate free hour" switch always displaying time instead of the location when you don't have a free hour.
 - Added spam protection to the "Make request" button.

## v1.1.3
_Bugfixes:_
 - Fixed app not displaying a conflict (2 lessons on thesame hour)

## v1.1.4
_Bugfixes:_
 - Multiple small bugfixes

## v1.1.5
_Changes:_
 - Changed update time, 10 -> 5 min

## v1.1.6
_Changes:_
 - Added new "Delete authentication data" button in settings. Only to be used on an app failure.

_Bugfixes:_
 - Fixed complication malfunctioning on Android 13
 - Fixed "last complication update" not showing day correctly.
 - 
## v1.2
_Changes:_
 - Users can now use different portals, this makes the app usable for all zermelo users.
 - Reworked settings interface, it now has a more uniform design.

_Bugfixes:_
 - Fixed edge case where the week number would be in the format `w` instead of `ww`, which is the format accepted by the zermelo API.

