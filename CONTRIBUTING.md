# How To Contribute
This project uses Firebase and therefore relies on a `google-services.json` configuration file. This file is not included in this repo and every contributor is encouraged to generate it's own.

When importing the project in Android Studio the build task will fail with the following error:

`org.gradle.api.GradleException: File google-services.json is missing. The Google Services Plugin cannot function without it.`

Or something similar depending on the Android Studio version you're using.

In order to generate a `google-services.json` configuration file follow these steps (Note: requires a Google account):

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