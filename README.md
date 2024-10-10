# Orator AI - README

## Overview

**Orator AI** is an AI-powered public speaking coach designed to help users improve their communication skills through personalized feedback. By using the device’s camera, microphone, and other sensors, the app analyzes speech delivery and body language, offering insights to enhance the user's public speaking abilities. This includes real-time feedback on factors such as tone, pace, volume, posture, and body language.

The app supports professionals, students, entrepreneurs, and others who want to improve their presentation skills by providing professional-quality coaching accessible anytime, anywhere.

---

## Features

### 1. **AI-Powered Feedback**
- **Real-Time Analysis**: Provides real-time feedback on speech delivery, including aspects such as tone, pace, volume, and clarity.
- **Body Language Evaluation**: Using the device's camera, Orator AI evaluates posture, gestures, eye contact, and non-verbal cues.

### 2. **Split-App Architecture**
Orator AI employs a **split-app model** that leverages Google Firebase services for cloud-based operations, while core AI processing happens on-device. Key components include:
- **Firebase Authentication**: Secure user login and account management with Google authentication integration.
- **Firebase Cloud Storage**: Secure storage of user data and practice sessions with cloud synchronization and backup.
- **Firebase Remote Config**: Allows dynamic updates of AI models and app configurations without requiring full app updates.
- **Firebase Crashlytics and Analytics**: Monitors app performance and usage patterns to continuously improve user experience.

### 3. **Multi-User Support**
- **Personalized Profiles**: Each user has their own profile with individualized settings, practice history, progress tracking, and preferences.
- **Data Isolation**: Secure and private handling of user data, ensuring data isolation across accounts.
- **Cross-Device Syncing**: Seamless access to personal profiles across multiple devices via cloud sync.

### 4. **Sensor Integration**
Orator AI leverages several device sensors to enhance user feedback:
- **Camera**: Captures video for body language analysis.
- **Microphone**: Records audio for feedback on verbal communication, such as tone, pace, volume, and filler words.
- **Accelerometer/Gyroscope (Optional)**: Enhances detection of movements and provides more accurate feedback on posture and gestures.

### 5. **Offline Functionality**
Orator AI remains fully functional even without internet connectivity:
- **Practice Sessions**: Users can continue recording speeches and receive real-time feedback offline.
- **Feedback and Resources**: On-device AI models provide instant feedback, and previously downloaded resources (such as tutorials) are available offline.
- **Data Sync**: When reconnected to the internet, the app automatically syncs data and updates profiles.

---

## Usage

1. **Sign In**: Users sign in using secure Google authentication.
2. **Profile Setup**: Each user can create a personalized profile to track their progress and customize settings.
3. **Recording Practice**: Users record their speeches or presentations using their device’s camera and microphone.
4. **Receive Feedback**: AI-driven real-time feedback is provided on both speech and body language.
5. **Review and Improve**: Users can track their improvements over time and access personalized coaching materials.

---

## Requirements

- Android device with camera and microphone
- Google Firebase account for cloud-based services (Authentication, Cloud Storage)
- Internet connection for cloud sync and updates (optional for offline use)

---

## Installation

1. Clone the repository or download the APK file.
2. Ensure that Firebase services are set up and configured as described in the project documentation.
3. Install the app on your device and sign in with your Google account.
4. Begin using Orator AI by recording practice sessions and receiving real-time feedback.

---

## Technologies Used

- **Google Firebase**: For authentication, cloud storage, real-time configuration, and analytics.
- **Android SDK**: For app development and integration with device sensors.
- **Multiple APIs**: For real-time speech and body language analysis.
- **Camera and Microphone Sensors**: To capture video and audio for feedback.

---

## Authors

Developed by the **Orator AI** team.
SWENT Group 17 
