# Orator AI - README

## About

**Orator AI** is an AI-powered public speaking coach that helps users enhance their communication skills through personalized feedback. Using the device's microphone, the app analyzes not only the content of the user's speech or interview answers, but also provides insights to improve public speaking and interview skills.

The app is designed for everyone, from beginners to experienced entrepreneurs and pitchers, offering professional-quality coaching that’s accessible anytime, anywhere.
---

## Features
- **Practice your speech skills**: The user has 3 different practice modes: Sales pitch, public speaking, and interview. The last one works in real-time, meaning the interview questions can change depending on the user's answers thanks to the Chat-GPT API we use.
- **Real-Time Analysis**: Provides real-time feedback on speech delivery, including aspects such as the emotions that can be derived from the words given as a response as well as what points the user can work on better and how good the response to an interview question/the speech given was.
- **Friends**: Users can have friends and see how they performed on the same interviews, giving them a sense of competition and a drive to work more on their public speaking skills.
To add friends, the user should send a request to the person he wants to add and they should accept it, but for now we have a "follow system" where the user can add people as friends and will see them as a friend, but the person added doesn't have them as a friend until they follow back.
-**Battle of Speech**: Challenge your friends in a Speech Mock Interview, and let the best win !
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

[Click here](https://www.figma.com/design/OvKRhZaDIyr1hJv4Nmcsks/swent?node-id=36-15&t=3CUvDmxivQfOh4vY-1) to access our first Figma draft and explore our design.
