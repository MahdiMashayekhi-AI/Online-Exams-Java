# 📝 Online Exams - Java

A powerful and flexible **online exam builder** developed in **Java**, designed to help users create, manage, and evaluate quizzes seamlessly. This project includes features such as user authentication, real-time database synchronization, and a simple, responsive UI.

---

## 🚀 Features

- 👤 User authentication (Firebase)
- 📄 Create, edit, and delete quizzes
- 📋 Add multiple questions with options and correct answers
- ⏰ Timed exams
- 📊 Result calculation and display
- 🔄 Real-time database integration with Firebase Realtime Database
- 🌐 Persian language support (RTL layout, Persian fonts, messages)

---

## 🔧 Tech Stack

- 💻 Java (Android SDK)
- 🔥 Firebase Realtime Database
- 🔐 Firebase Authentication
- 🎨 XML UI Design (Android)
- 📦 RecyclerView for dynamic question lists

---

## 🛠️ Setup & Run

### Prerequisites:
- Android Studio (Arctic Fox or newer)
- Firebase project (with Authentication and Realtime Database enabled)

### Steps:

1. Clone the repository:
   ```bash
   git clone https://github.com/MahdiMashayekhi-AI/Online-Exams-Java.git
    ```
2. Open the project in Android Studio.

3. Connect your Firebase project:
    ```bash
    Add google-services.json to app/
    Enable Email/Password authentication in Firebase
    Set up your Realtime Database structure (sample structure can be provided)
    ```

4. Build and run the app on an emulator or Android device.

## 📁 Project Structure
```bash
    📦 app/
    ├── activities/           # All activity files (Login, Signup, ExamEditor, etc.)
    ├── models/               # Java model classes (User, Exam, Question, etc.)
    ├── adapters/             # RecyclerView adapters
    ├── utils/                # Helper classes and utilities
    ├── res/
    │   ├── layout/           # XML UI files
    │   ├── values/           # Colors, strings, themes
    │   └── drawable/         # Icons and drawable assets
```

## 📄 License
This project is licensed under the MIT License.
Feel free to use, modify, and distribute!

## 🙌 Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## 📬 Contact
Mahdi Mashayekhi
📧 [mahdimashayekhi.ai@gmail.com]
🔗 [mahdimashayekhi.ir]