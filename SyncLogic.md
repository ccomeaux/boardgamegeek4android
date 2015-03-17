# Sync Logic

# Introduction

A sync is requested every 8 hours. The OS controls when the sync actually occurs, but this schedule is generally observed. Under certain failures, a retry is attempted with a back off (controlled by the OS). The sync goes through 3 major steps:
  * Collection
  * Plays
  * Buddies

# Details

## Collection
### Collection Items
  * **Initially and every 7 days**, sync the entire collection (weekly syncing will remove old collection items as well as ensure incremental syncs didn't skip anything). to reduce the amount of data returned,
    * each configured status is synced separately. (_played_ is synced first due to quirk in API)
    * stats and private info are not synced
  * For each requested sync status, sync both games, then accessories
  * **Otherwise**, sync both games, then accessories modified since the last sync datetime
    * Include stats and private info
  * Get the stats and private info for all collection items that haven't been updated with this info yet
    * 25 at a time, no more than 100 iterations
    * For sync both games, then accessories
### Games
#### Stale
  * Get the game detail for the 16 games that are the oldest synced - 1 API call for all games
#### Unupdated
  * Get the game info for all collection items that haven't been updated with this info yet; 16 at a time, no more than 100 iterations
  * These can fail with a timeout exception, keep retrying with 1/2 the number of games until successful or until 1 game fails

## Plays
### Upload
  * For each game pending update, submit an update/insert through JavaScript (no API for this exists)
  * After each successful update, immediately sync all plays for the game and date just update. No other way to get the BGG-assigned ID or the validated usernames.
  * For each game pending delete, submits a delete through JavaScript
  * Upon success delete them from the local database
### Download
  * Initially, sync all plays, one page at a time, starting with the most recent plays
  * Record the oldest synced date in order to resume in case of failure. Eventually, all old plays are synced and only new dates are synced.
  * Ongoing, sync from the newest date (inclusive) forward and from the oldest data backward in case of failure above

## Buddies
### List
  * Fetches the users's entire list of buddies - both adding and removing buddies
  * Only syncs every 3 days (not every 8 hours)
### Stale
  * Updates the oldest user (the user that hasn't been updated in the longest)
  * It used to updates the 25 oldest users, but this resulted in 25 API calls, and the data never changed that much
### Unupdated
  * Fetches all users that haven't been fully updated
  * Results in an API call per user
  * This is the only way to get the first and last name of the user
