# Orator AI - README

## About

**Orator AI** is an AI-powered public speaking coach that helps users enhance their communication skills through personalized feedback. Using the device's microphone, the app analyzes not only the content of the user's speech or interview answers, but also provides insights to improve public speaking and interview skills.

The app is designed for everyone, from beginners to experienced entrepreneurs and pitchers, offering professional-quality coaching that’s accessible anytime, anywhere.
---

## Features
- **Practice your speech skills**: The user has 3 different practice modes: Sales pitch, public speaking, and interview. The scenario changes depending on the user's answers thanks to the Chat-GPT API we use.
- **Real-Time Analysis**: Provides real-time feedback on speech delivery, including aspects such as the emotions that can be derived from the words given as a response as well as what points the user can work on better and how good the response to an interview question/the speech given was.
- **Friends**: To add friends, the user should send a request to the person he wants to add and they should accept it. A user can compare his global achievements to his friend's, and see who is the best !
-**Battle of Interviews**: Challenge your friends in a Speech Mock Interview, and let the best win !
-**Track your progress**: The app allows you to track your progress by maintaing a streak, speech-related stats and other entertaining features :)

### **Architecture diagram**
Below is the architecture diagram we have this far for our application, which we will change as the app progresses:
![image](https://github.com/user-attachments/assets/cb5c6d83-80de-4c9c-991f-56f5e9d5ce7f)


### **Offline Functionality**
Orator AI remains fully functional even without internet connectivity:
- **Practice Sessions**: Users can continue recording speeches which will be saved on the device, and will recieve feedback once they connect to the internet.
- **Feedback and Resources**: On-device AI models provide instant feedback, and previously downloaded resources (such as tutorials) are available offline.
- **Data Sync**: When reconnected to the internet, the app automatically syncs data and updates profiles. 

---

## Figma Project

[Click here](https://www.figma.com/design/OvKRhZaDIyr1hJv4Nmcsks/swent?version-id=2163970124783976182&node-id=36-15&p=f&t=jX47A8MDzibRyVlN-0) to access the Figma of the application and explore our design.
