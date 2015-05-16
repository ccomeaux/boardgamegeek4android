Release Notes
=============

Coming Soon
-----------
 * Export/import some data (under Advanced Settings)
 * Improve list usability
 * Updated look and feel of nav drawer
 * Time-based data is updated automatically (e.g. 5 mins ago becomes 6 mins ago)
 * Fix forum and GeekList links and images
 * Bug fixes and stability improvements

Version 5.0.2
-------------
 * Darken font in a couple of areas for readability
 * Cap game's plays per month at total play count
 * More bug fixes and stability improvements

Version 5.0.1
-------------
 * Suppress H-Index notification if it's 0
 * Bug fixes and stability improvements

Version 5.0
-----------
 * Material design and support for Android Lollipop
 * GeekLists
 * Play stats
 * Improved sync and sync notification messages
 * Use Google Now to search by voice by saying "search *game name* on BoardGameGeek"
 * Access and edit play locations, players, and stats from the navigation drawer
 * Zoom and pan game images
 * Default collection view shows only items you've requested to sync
 * Sort collection by rank
 * Filter collection by play count
 * Quick log multiple plays when viewing a list
 * Probably lots of other improvements
 * Miscellaneous bug fixes

Version 4.8.4
-------------
 * Syncing logic is more tolerant of API changes

Version 4.8.3
-------------
 * Syncing improvements (fewer 503s)

Version 4.8.2
-------------
 * Fixed displaying images

Version 4.8.1
-------------
 * Fixed incorrect statistics (including rating)
 * Can now select a random game from a buddy's collection
 * Miscellaneous bug fixes

Version 4.8
-------------
 * Now syncs played games that aren't otherwise in your collection
 * Now syncs accessories (though this will break the base game/expansion filter until the next complete collection sync)
 * Lots of usability improvements around logging plays
 * View all players along with GeekBuddies
 * Completely rewritten syncing logic
 * Dropped support for Froyo (API level 8)

Version 4.7.3
-------------
 * Fix crashes in forums and ordering players
 * Change look and feel of navigation drawer
 * Modernize the look of the slider in the filter dialog
 * Show the collection properly when adding a shortcut

Version 4.7.2
-------------
 * Fix crash when viewing buddy collection
 * Don't allow clicking on forum header
 * When saving a view, enable the Save button correctly
 * When deleting a view, go back to the default view
 * Improve format of forum for narrow screens
 * Clicking the h-index notification takes you to a BGG thread

Version 4.7.1
-------------
 * Fixed the crashing

Version 4.7
-------------
 * Sticky headers and bigger images in lists
 * Set a collection view as the default
 * When adding players to a play, a list of most common players is displayed, enabling quicker entry
 * Consider a play started within 2 hours of the previous play ending as part of the same session, and copy the location and players to the new play
 * Calculates your games played h-index
 * Ability to add a color to a game
 * Option to show the full date/time in the forums (under Settings > Advanced)
 * When syncing, remove games not in your collection if they haven't been viewed in 3 days
 * When enabling custom player order, prompt to remove the existing starting positions
 * Miscellaneous fixes and improvements

Version 4.6.1
-------------
 * When logging a play, players are automatically placed in seating 
 * Prevent crash when viewing a Buddy's collection or plays
 * Improve performance when loading forums
 * Correctly display the votes for Best with 1 Player
 * Update a game's play count after each logged play
 * Fix miscellaneous crashes

Version 4.6
-----------
 * When logging a play, players are automatically placed in seating order with the ability to drag and drop
 * Ability to refresh plays for a specific day
 * When adding a player, keep adding them until the user cancels
 * Menu options to share and view forums in a browser
 * Improve the settings screen and move some of the standard menu options there
 * When logged in, always show all of the menu options in the drawer, and improve the message shown for an empty list
 * Fix lots of reported crashes

Version 4.5
-----------
 * *NEW:* Show a play timer
 * *NEW:* Option to play a game again
 * *MOD:* Play count is now more accurate
 * *MOD:* Update collection items more completely during a sync
 * *BUG:* Fix yet another auth problem

Version 4.4.1
-------------
 * *MOD:* Fixed the login problem for real
 * *MOD:* Add sorting by my rating and play count

Version 4.4
-----------
 * *MOD:* New authentication; you will be logged out
 * *NEW:* Navigation drawer replaces the dashboard
 * *NEW:* Collection tab in the game view
 * *MOD:* Improved usability in lists, especially the Collection
 * *NEW:* Options to sync only when charging or on WiFi
 * *NEW:* Sort games by last viewed date/time
 * *NEW:* Sort comments by rating instead of user
 * *NEW:*  Now you can cancel a sync (through the notification or a menu option on the home screen)
 * *MOD:* Collection list now shows collection thumbnail (not the game thumbnail)
### GeekBuddy Enhancements
 * *NEW:* Show plays you've logged with them
 * *MOD:* View collection statuses other than owned
 * *NEW:* Refresh
 * *NEW:* Change nickname used while logging plays

Version 4.3
-----------
 * *NEW:* You can now sign out from the home screen
 * *MOD:* Brought back confirmation of uploaded plays
 * *MOD:* Player names now auto-complete
 * *MOD:* Forum UX improvements (including lists now remember their last position)
 * *BUG:* Fix overlapping location and date in the Plays list
 * *BUG:* Now correctly prompts when your password has expired
 * *BUG:* A forum with more than 50 threads will now load completely

Version 4.2
-----------
 * *NEW:* Option to disable capturing poll information to prevent crashes
 * *NEW:* Option to show crash errors when polls are on - this will help the developers to track down the crashes

Version 4.1
-----------
 * *NEW:* Option to turn on advanced debugging - please do this if you are experiencing crashes
 * *NEW:* Options to set the player order when logging a play
 * *MOD:* New look for the about screen
 * *MOD:* Option to show password while logging in
 * *MOD:* Plays list sorted more logically
 * *BUG:* Fix crash viewing a game on low density devices
 * *BUG:* Fix game update when not signed in
 * *BUG:* Fix crash when attempting to clear or perform a full sync from the Settings screen while not logged in

Version 4.0
-----------
 * *NEW:* Updated look and feel including an action bar and other Android standards
 * *NEW:* Uses built-in account manager to handle authenticating to the 'Geek and synchronizes in the background (You will have to re-enter your username and password)
 * *MOD:* Lots of performance improvements under the hood
 * *MOD:* Rearranged settings
 * *MOD:* Can filter Plays by sync status
 * *MOD:* Play locations and !GeekBuddy nicknames are saved during a sync (not just when modifying the game locally)
 * *BUG:* Improve layout of dashboard in landscape mode with an odd number of icons
 * *BUG:* Don't save multiple copies of a play in progress
 * Too many more to list!

Version 3.6
-----------
 * *NEW:* Sort your collection
 * *NEW:* Save and load the collection view (sort and list of filters)
 * *NEW:* Added a menu option to contact the development team
 * *MOD:* Many new collection filters
 * *MOD:* While viewing a play, clicking on the thumbnail will show the game
 * *BUG:* Fix occasional double logging of plays
 * *BUG:* Improve the cropping of shortcut icons
 * *BUG:* Lots of miscellaneous crashes should be fixed

Version 3.5
-----------
 * *NEW:* Now you can save a play without syncing it
 * *MOD:* Usability improvements to play logging
 * *MOD:* Automatically sync collection, buddies, or plays when viewing the respective list
 * *MOD:* Icon support for Android 4
 * *BUG:* Correctly update game ranks
 * *BUG:* Lots of miscellaneous fixes

Version 3.4
-----------
 * *NEW:* Ability to sync, view, edit, and delete plays
 * *NEW:* Ability to see GeekBuddies' collection<
 * *NEW:* See a game's expansions and base games
 * *NEW:* Ability to create a shortcut
 * *NEW:* Show the first letter of the game when scrolling the collection list
 * *MOD:* Move poll information from its own tab to buttons on the info tab
 * *MOD:* Syncing improvements (faster and more complete)
 * *MOD:* General UI improvements
 * *BUG:* Fix sync issue in ICS

Version 3.3
-----------
 * *NEW:* Forums
 * *BUG:* Searching for an exact match with "Skip results" turned off now works

Version 3.2
-----------
 * *NEW:* Collection filtering
 * *NEW:* View game comments with ratings
 * *MOD:* Logging games
   * *NEW:* Add players
   * *NEW:* Options to hide fields by default (can be re-added)
   * *NEW:* Save Teams and Colors by game
 * *NEW:* Long-click menu options
 * *BUG:* Fixed crash when rotating while viewing Stats tab.

Version 3.1
-----------
 * *NEW:* The Hotness!
 * *NEW:* Help text displayed when first viewing a board game
 * *NEW:* Ratings for each subdomain
 * *MOD:* Change look of stats and poll bars
 * *MOD:* Change board game loading message
 * *BUG:* Correct syncing for usernames with spaces
 * *BUG:* Correct syncing of polls for player age 21+
 * *BUG:* Increase timeouts for more reliable syncing

Version 3.0
-----------
 * *NEW:* Brand new look and feel
 * *NEW:* Ability to sync collection and geek buddies
 * *NEW:* Ability to log a single play on the current day
 * *MOD:* Improved graphics for high and low density screens
 * *BUG:* Fixed displaying of ordinal numbers
 * *BUG:* Fixed parsing of rank information
 * *BUG:* Improved formatting of long descriptions

Version 2.4.1
-------------
 * *BUG:* Correct display of rank

Version 2.4
-----------
 * *NEW:* Ability to log plays
 * *NEW:* Viewing the database now shows thumbnails when available
 * *NEW:* Search and View Database buttons on the main screen
 * *MOD:* Formatting improvements
 * *MOD:* Thumbnail cache is set to hidden
 * *BUG:* Improve image loading time

Version 2.3
-----------
 * *NEW:* Viewing a game places it in a local database. This database can be configured in the settings menu.
 * *NEW:* When enabled in the phone's search settings (Menu > Settings > Search > Searchable items), the database will be searched by the phone's Quick Search Box. (If you're unfamiliar, see this [http://googlemobile.blogspot.com/2009/10/quick-search-box-for-android-search.html article].)
 * *NEW:* Searching while [BoardGameGeek.com] is down returns an error message instead of no results
 * *MOD:* Android framework of 1.6 is required
 * *MOD:* Performance enhancements to downloading images
 * *MOD:* Search results include year (again)
 * *MOD:* Search box is no longer displayed on initial launch
 * *MOD:* Search can now be invoked using Menu+s and Setting using Menu+p
 * *MOD:* The eBay link now directs to the mobile site
 * *MOD:* Version number now displayed on the main page
 * *MOD:* The about screen is reformatted
 * *MOD:* Progress bars have been moved from dialog boxes to directly in the activities
 * *BUG:* Improved description formatting

Version 2.2
-----------
  * *BUG:* Fixed crash when poll information was missing

Version 2.1
-----------
  * *BUG:* Improved description when special characters are present
  * *BUG:* Going back to the search results from the game page no longer re-runs the search
  * *NEW:* Added tab that displays poll information
  * *MOD:* Clicking on the designer, artist, or publisher in the Extra tab displays more information
  * *MOD:* Improve the display of the Stats tab

Version 2.0
-----------
  * *NEW:* Tabs on the game page now display more information (stats, links, and extra information)
  * *BUG:* Fix display of game description to format special characters properly
  * *MOD:* Various look and feel changes

Version 1.9
-----------
 * *BUG:* Fixed force crash due to a change in the API (Thanks to tomasz.luch for the bug report!)

Version 1.8
 * *MOD:* Changed the search bar in preparation for future updates

Version 1.7
-----------
 * *BUG:* Fixed a few orientation issues
 * *BUG:* Fixed using the enter key to submit a search

Version 1.6
-----------
 * *NEW:* Ranking system
 * *MOD:* Re-download on orientation change
 * *BUG:* Fixed year in search results

Version 1.5
-----------
 * *BUG:* Fixed age display
 * *BUG:* Fixed force close due to missing image or site being down

Version 1.4
-----------
 * *BUG:* Fixed image display

Version 1.3
-----------
 * *BUG:* Fixed force crashing due to BGG 2.0 updates

Version 1.2
-----------
 * *BUG:* Fixed force crash on orientation change

Version 1.1
-----------
 * *MOD:* Minor search improvement
