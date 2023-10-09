# (AR)droid
This project showcases Augmented Reality (AR) on Android by projecting a cube onto a reference object. 
It uses the OpenCV 4.8.0 SDK for Android and Kotlin. 

The Computer Vision algorithms have been taken from [Augmented Reality on Webcam](https://github.com/Wissiak/augmented-reality-on-webcam).
The mobile version has been rewritten to Kotlin and has been enhanced with some mobile functionalities. 
The goal of this project was not to improve performance but rather to show the feasibility of a simple AR project on mobile devices.

## Showcase


## Project Setup
1. Download the OpenCV 4.8.0 Android SDK from [OpenCV Releases](https://opencv.org/releases/)
2. Unpack the downloaded folder to a folder of your choice (e.g. ~/tools/OpenCV-android-sdk)
3. Clone this repository
4. Open this repository in Android Studio
5. Open `Project Structure... > Modules` and click on `New Module`
---
**NOTE**
You might have to remove the module "openCV" first in order to add it again. For this, do the following: 
1. Comment out the following line in `app/build.gralde` and try again:
`implementation project(path: ':openCV')`
2. Go to `Project Structure... > Modules` and remove the openCV module.
3. Run Gradle sync

Afterwards you should be able to add it normally.

---
6. Click on `Import Module` and specify the unpacked folder with "/sdk" appended (e.g. ~/tools/OpenCV-android-sdk/sdk)
![Import Module](./opencv-installation.png)

---
Remember to uncomment the line in `app/build.gralde` if you commented it out before:
`implementation project(path: ':openCV')`

---

7. Wait for Gradle build to (hopefully) finish successfully
8. Start the App! :)
