# How to contribute
## Coding
This project uses Firebase and therefore relies on a `google-services.json` configuration file. This file is not included in this repo and every contributor is encouraged to generate its own.

When importing the project in Android Studio the build task will fail with the following error:

`org.gradle.api.GradleException: File google-services.json is missing. The Google Services Plugin cannot function without it.`

or something similar depending on the Android Studio version you're using.

In order to generate a `google-services.json` configuration file follow these steps (Note: it requires a Google account):

- Open the [Firebase Console](https://console.firebase.google.com/).
- Login with your Google account.
- Create a new project (name doesn't matter).
- Select "_Add Firebase to your Android app_".
- Provide package name:  `com.boardgamegeek`.
- Register app.
- Download `google-services.json` file.
- Follow instructions to add file to project.
- Skip "_Add Firebase SDK step_".
- Run app to verify that the configuration is picked up correctly.

## Translating
If you want to help the project by translating the app, you have to:

1. Navigate to [/app/src/main/res](/app/src/main/res);
2. Create a folder named ```value-XX``` where ```XX``` is a valid [ISO 639-1 language code](https://developer.android.com/guide/topics/resources/providing-resources.html#AlternativeResources) and insert translated files there.
3. Copy and translate the following files in the ```value``` folder. Note that only values that are different need to be present; if a key is missing, it will use the default English text.
- arrays.xml
- strings.xml
- strings_notifications.xml
- strings_prefs.xml
- strings_sync_service.xml
