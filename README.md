HelloWorld
==========
An Android chatting app for Computer Science students. This is just a small project for a mobile development class; Don't expect anything special to come out of this.

Website: http://k.yle.sh/  
Author: Kyle Colantonio ![email](http://i.imgur.com/pUOz6mM.png)

About
-----
HelloWorld is a simple and clean chatting app that utilizes [Firebase](https://firebase.google.com/) for authentication and real-time messaging. The goal is to allow students (primarily in Computer Science) to join chatrooms for specific classes, allowing them to ask questions and easily communicate with other students who are also in the same class.

#### Features
* Sign-in with Email or Google accounts
* Fast and lightweight
* Support back to KitKat (API 19)
* Follows Google's Material Design Guidelines*

**Attempts to follow as best as possible; Sorry I'm still new to Android development.* :smile:

Files
-----
#### google-services.json
This file is autogenerated by [Firebase](https://firebase.google.com/). You can find more details about this on their [Android Setup Guide](https://firebase.google.com/docs/android/setup).

#### tokens.xml
This file holds all tokens (public and private) for all the services in this app. This file is not synced to GitHub for security reasons, although an example is provided below.

```xml
<resources>
    <!-- Firebase tokens -->
    <string name="firebase_id">firebaseapp-XXXXX.firebaseapp.com</string>
    <string name="firebase_bucket">firebaseapp-XXXXX.appspot.com</string>

    <!-- Google tokens -->
    <string name="google_client_id">############-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX.apps.googleusercontent.com</string>
</resources>
```
