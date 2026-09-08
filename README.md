# 📱 SnapStudy (סנאפ-סטאדי)

<div align="center">

<img src="app/src/main/res/drawable/ic_snapstudy_logo.png" alt="SnapStudy Logo" width="120" />

**עוזר למידה חכם וסורק סיכומים מבוסס AI לסטודנטים ותלמידים**
<br>
*Transform handwritten notes, textbook pages, and study materials into smart, interactive flashcards powered by Google Gemini AI.*

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com)
[![Gemini](https://img.shields.io/badge/AI-Google%20Gemini%202.5%20Flash-8E75B2?logo=google&logoColor=white)](https://ai.google.dev)
[![Java](https://img.shields.io/badge/Language-Java-007396?logo=java&logoColor=white)](https://www.java.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

---

## 🌟 אודות הפרויקט (About)

**SnapStudy** היא אפליקציית אנדרואיד מתקדמת המיועדת להפוך את חוויית הלמידה והחזרה למבחנים לפשוטה, מהירה ואפקטיבית. 
באמצעות צילום מהיר של מחברת, דף סיכום או ספר לימוד, המערכת מזהה את התוכן ומייצרת באופן אוטונומי ערכות כרטיסיות חכמות (Flashcards) הכוללות שאלות ותשובות ממוקדות באמצעות **Google Gemini 2.5 Flash**.

האפליקציה תומכת בממשק עברי מלא (RTL), מצב כהה (Dark Mode) אלגנטי, ומציעה חוויית תרגול אינטראקטיבית עם אנימציות היפוך תלת-ממדיות ומעקב אחר יעדי למידה יומיים.

---

## ✨ פיצ'רים עיקריים (Key Features)

### 📸 1. סורק מסמכים חכם (Smart Scanner)
- **מצב אוטומטי (Auto Mode)**: זיהוי אוטומטי של דפים ומסמכים בזמן אמת עם סימון מסגרת אדומה (`Red Bounding Box`) וביצוע צילום אוטומטי ברגע שהדף מזוהה ויציב.
- **מצב ידני (Manual Mode)**: שליטה מלאה בצילום עם תמיכה בפלאש, החלפת מצלמות וייבוא תמונות מגלריית המכשיר.
- **עיבוד תמונה אופטימלי**: חיתוך, יישור והכנה לעיבוד בינה מלאכותית.

### 🤖 2. יצירת כרטיסיות בבינה מלאכותית (AI Flashcard Generation)
- שילוב ישיר עם **Google Gemini API (`gemini-2.5-flash`)**.
- ניתוח רב-מודאלי (Multimodal Vision) המזהה טקסט, כותרות ותרשימים גם מכתב יד.
- יצירה מיידית של שאלות ותשובות ממוקדות בחלוקה לנושאים ומושגי מפתח.

### 🃏 3. מערכת תרגול אינטראקטיבית (Flashcard Practice System)
- תרגול מהיר עם אנימציות היפוך כרטיסייה (3D Card Flip).
- מנגנון סימון "ידעתי" / "לחזור שוב" המעדכן את רמת השליטה בחומר.
- מעקב התקדמות בזמן אמת והצגת סיכום הישגים בסיום הסשן.

### 🔐 4. מערכת אימות ואבטחה מלאה (Firebase Authentication)
- **התחברות עם Google**: באמצעות Google Credential Manager העדכני.
- **התחברות עם Apple**: מיושמת לפי תקני ה-SDK הרשמיים של Firebase ל-Android (כולל טיפול ב-Chrome Custom Tab ומאזינים מוגנים).
- **אימייל וסיסמה**: רישום, התחברות עם אימות שם משתמש, ואיפוס סיסמה מאובטח המוודא את קיום המשתמש לפני שליחת המייל.

### ☁️ 5. סנכרון ענן בזמן אמת (Cloud Firestore)
- שמירה וסנכרון מיידי של כל ערכות הלימוד, הכרטיסיות והסטטיסטיקות של המשתמש.
- חוקי אבטחה קפדניים (`firestore.rules`) המבטיחים שכל משתמש ניגש אך ורק למידע האישי שלו.

### 🎯 6. ניהול יעדים ותזכורות (Goals & Study Reminders)
- הגדרת יעדי כרטיסיות יומיים (Daily Targets).
- תזכורות יומיות מותאמות אישית לעידוד רצף למידה (Study Streak).

---

## 🛠️ ארכיטקטורה וטכנולוגיות (Tech Stack)

| שכבה | טכנולוגיה |
| :--- | :--- |
| **שפת פיתוח** | Java 17 (Android Native) |
| **ארכיטקטורה** | Service-Oriented Architecture / MVC |
| **ממשק משתמש** | Material Design 3, ConstraintLayout, Dynamic RTL, Custom Animations |
| **מצלמה** | AndroidX CameraX (`camera-camera2`, `camera-lifecycle`, `camera-view`) |
| **בינה מלאכותית** | Google Gemini 2.5 Flash API |
| **Backend & Auth** | Firebase Authentication (Google, Apple, Email/Password), Cloud Firestore |
| **תאימות** | Min SDK 26 (Android 8.0) - Target SDK 35 (Android 15) |

---

## 📂 מבנה הפרויקט (Project Structure)

```text
SnapStudy/
├── app/
│   ├── src/main/
│   │   ├── java/com/yalig/snapstudy/
│   │   │   ├── ai/                      # מנוע ה-AI וחיבור ל-Gemini API
│   │   │   │   ├── GeminiConfig.java
│   │   │   │   └── GeminiStudyService.java
│   │   │   ├── model/                   # מודלים (Flashcard, StudySet, UserStats)
│   │   │   ├── utils/                   # כלי עזר לאנימציה, עיצוב ותאריכים
│   │   │   ├── LoginActivity.java       # מסך כניסה/הרשמה (Google, Apple, Email)
│   │   │   ├── MainActivity.java        # מסך בית, סקירת ערכות ויעדים
│   │   │   ├── ScannerActivity.java     # סורק מסמכים (אוטומטי / ידני)
│   │   │   ├── FlashcardStudyActivity.java # תרגול אינטראקטיבי
│   │   │   └── SettingsFragment.java    # הגדרות פרופיל, מצב כהה ותזכורות
│   │   ├── res/                         # משאבי עיצוב, פריסות (XML) ואייקונים
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── firestore.rules                      # חוקי אבטחה של Cloud Firestore
├── .gitignore                           # הגדרות התעלמות מותאמות ל-Android Studio
└── README.md
```

---

## 🚀 התקנה והרצה (Getting Started)

### דרישות מקדימות
- **Android Studio** (גרסת Ladybug / Meerkat ואילך).
- **JDK 17** ומעלה.
- מכשיר אנדרואיד פיזי או אמולטור עם תמיכה ב-Google Play Services (API 26 ומעלה).

### שלבי הרצה

1. **שכפול המאגר (Clone Repository)**:
   ```bash
   git clone https://github.com/yaheli-mil/SnapStudy.git
   cd SnapStudy
   ```

2. **פתיחת הפרויקט ב-Android Studio**:
   - פתח את Android Studio ובחר ב-`Open`.
   - בחר את התיקייה `SnapStudy` והמתן לסיום ה-Gradle Sync.

3. **הגדרת Firebase**:
   - צור פרויקט ב-[Firebase Console](https://console.firebase.google.com).
   - הוסף אפליקציית Android עם ה-Package Name: `com.yalig.snapstudy`.
   - הורד את קובץ `google-services.json` והצב אותו בתיקייה:
     `app/google-services.json`.
   - הפעל ב-Firebase Authentication את הספקים: Email/Password, Google ו-Apple.

4. **הרצת האפליקציה**:
   - חבר מכשיר פיזי או הפעל אמולטור.
   - לחץ על **Run** (`Shift + F10`) או בצע בנייה דרך שורת הפקודה:
     ```bash
     ./gradlew assembleDebug
     ```

---

## 🔒 אבטחה ופרטיות (Security)

- **כללי Firestore**: כל המידע מוגן על ידי כללי Firestore המאפשרים קריאה וכתיבה רק למשתמש המאומת בעל ה-UID התואם.
- **אבטחת אימות**: שימוש ב-Credential Manager מבית Google ו-OAuthProvider מבית Apple עם עמידה בתקני פרטיות מלאים.

---

## 👥 יוצר הפרויקט (Author)

פותח ועוצב על ידי **יהלי (yalim)**.
לשאלות, שיתופי פעולה או הצעות לשיפור, ניתן לפתוח Issue או Pull Request.
