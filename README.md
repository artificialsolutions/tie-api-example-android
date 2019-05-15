# Example Android chat app for Teneo
This sample project shows the capabilities of the TIE SDK, working in conjunction with 3rd party ASR, TTS and UI elements, as the form of a Chat app. These are the 4 combined aspects of the app that are described ahead:
   - TIE SDK connectivity.
   - Google ASR implementation and a typical Textbox bar to capture user input. 
   - Text to Speech (TTS) capability to speak bot replies out loud.
   - A message adapter that handles tasks related to the Chat UI.


## Prerequisites
   - You need to know the engine url of a published bot.
   - Android Studio 3.4 or later.
   - Android 6 Marshmallow (API 23) device, or better. (Recommended)

## Installation
   - Clone the repository.
   - Point the app at your bot's Teneo engine by setting the following variables in the `ChatActivity.java` class:
      - `baseUrl`, the base url of your engine, for example `https://myteam-4fe77f.bots.teneo.ai`
      - `solutionEndpoint`, the path or endpoint of your engine, like `/longberry_baristas_0x383bjp5a8e6tscbjd9x03tvb/` **Note: make sure it ends with a slash (/)**


## Project elements Documentation
### TIE SDK connectivity.
This dependency in the ```app.gradle``` file enables the app to use the TIE SDK and communicate with Teneo Engine:
```
implementation 'com.artificialsolutions.tie-sdk-android:tie-sdk-android:1.x.x'
```
The ```onCreate``` method of the app initializes, among other things, the ```TieApiService```

Within this app, a text can be sent to Teneo Engine anytime, with this helper method:
```
sendInputToTiE(String text, HashMap<String, String> params)
```
This project follows TIE SDK connectivity guidelines which are fully detailed in [```tie-api-example-android```](https://github.com/artificialsolutions/tie-api-example-android) example.

### Speech Recognition (ASR)
This project implements Google's Android native ASR with [```RecognizerIntent```](https://developer.android.com/reference/android/speech/RecognizerIntent).
The method ```launchGoogleVoice``` initializes RecognizerIntent with parameters such as Language, number of results, confidence scores, and is called from the microphone button's XML code in ```activity_chat.xml``` It also stops any TTS playback before launching ASR.
Final ASR results are received at the ```onActivity``` callback, sent to Engine as text with:

```consumeASRresults(int resultCode, Intent data)```
Localized ASR language is customizable, for example, in this project, ASR language is set to UK English with: ``asrIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en_GB")```

### TTS
TTS is implemented with Android's native [TTS](https://developer.android.com/reference/android/speech/tts/TextToSpeech). The object ```googleTextToSpeech``` within the project is the center of voice synthesis, and is initialized, launched and released throughout the lifecycle.
In this project, the method ```saySomething(String text)```  speaks out loud the bot responses received from Teneo Engine.
Localized TTS language is customizable. In this project, for example, TTS language is set to UK English with: ```googleTextToSpeech.setLanguage(Locale.UK);```

### Chat UI
Chat UI is implemented in the ```MesageAdapter``` class. You may customize message bubble color, avatar, and sender in that class, if you wish. 
In the project, user input from the ASR or the keyboard are posted into the chat window with:
```
addMessageToChatWindow(String messageText, String color, boolean isUser)
```
Likewise, messages incoming from engine are also posted to the Chat Window.
