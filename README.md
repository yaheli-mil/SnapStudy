# SnapStudy

אפליקציית אנדרואיד (Native Java) ללמידה ותרגול בעזרת כרטיסיות חכמות (Flashcards), שפותחה כפרויקט גמר 5 יח"ל בהנדסת תוכנה.

הרעיון המרכזי של האפליקציה הוא לקצר את הזמן שלוקח להכין כרטיסיות לימוד: במקום להקליד שאלות ותשובות ידנית, מצלמים עמוד מספר לימוד או מחברת (או בוחרים תמונה מהגלריה), והאפליקציה מפענחת את הטקסט ומייצרת כרטיסיות שאלות ותשובות ממוקדות באמצעות Google Gemini API.

---

## מה האפליקציה כוללת?

- **סורק מסמכים חכם (`ScannerActivity`)**:
  - שימוש ב-**CameraX** עם תצוגה מקדימה וזיהוי יציבות.
  - זיהוי טקסט מקומי על גבי המכשיר באמצעות **Google ML Kit Text Recognition** ב-Frames חיים של המצלמה.
  - צילום אוטומטי (Auto-Capture) ברגע שהדף מזוהה ויציב מול המצלמה למשך כשנייה, לצד אפשרות לצילום ידני או בחירת תמונה מהגלריה.
  - שכבת תצוגה מותאמת אישית ([`DocumentOverlayView`](app/src/main/java/com/yalig/snapstudy/ui/DocumentOverlayView.java)) שמציירת מסגרת מודגשת חלקה מסביב לטקסט בזמן אמת.

- **יצירת כרטיסיות עם Gemini AI (`GeminiStudyService`)**:
  - שליחת התמונה ב-Base64 לבקשת HTTP מול מודל `gemini-2.5-flash` של Google.
  - אכיפת פלט JSON מובנה (`responseMimeType: application/json`) שמחזיר כותרת לסט ורשימת כרטיסיות (שאלה + תשובה).
  - עיבוד התמונה והקטנת הרזולוציה לפני השליחה כדי לחסוך ברוחב פס ולמנוע שגיאות זיכרון (OutOfMemory).

- **תרגול כרטיסיות פעיל (`FlashcardsActivity`)**:
  - לימוד בשיטת Active Recall עם כרטיסיות מתהפכות באנימציית תלת-ממד (Flip 3D).
  - מענה לפי 4 רמות קושי (שוב, קשה, טוב, קל) המעדכן מרווחי חזרה (Spaced Repetition) לתכנון שינון יעיל.
  - צלילים ואפקטים בלחיצה על כפתורים ומענה על שאלות.
  - מסך סיכום בסיום התרגול עם ציון ואחוזי הצלחה.

- **לוח בקרה ומעקב למידה (`MainActivity`)**:
  - הצגת סטים קיימים וחיפוש מהיר בזמן אמת.
  - מעקב אחר רצף ימי למידה (Daily Streak) ויעד כרטיסיות יומי.
  - אפשרות לפתוח סט, לערוך את שמו, למחוק אותו או להוסיף סט ידנית.

- **אימות וסנכרון ענן (Firebase)**:
  - התחברות מהירה עם חשבון Google באמצעות ספריית `CredentialManager` של אנדרואיד.
  - הרשמה והתחברות רגילה עם מייל וסיסמה, כולל מנגנון שחזור סיסמה.
  - שכבת נתונים ([`StudyRepository`](app/src/main/java/com/yalig/snapstudy/data/StudyRepository.java)) בשיטת Offline-First: כל השינויים נשמרים מקומית ב-`SharedPreferences` ומסתנכרנים מול **Cloud Firestore**.

- **הגדרות והתאמה אישית (`SettingsActivity`)**:
  - מעבר בין ערכת נושא בהירה (Light), כהה (Dark) או מעקב אחרי הגדרות המערכת (Auto) באמצעות `AppCompatDelegate`.
  - קביעת שעת תזכורת יומית ללימוד בעזרת `TimePickerDialog`.
  - התאמת יעד הכרטיסיות היומי.
  - אפשרות לכיבוי והדלקת צלילים.

---

## טכנולוגיות וספריות בפרויקט

- **שפה וסביבה**: Java 17, Android SDK (Min SDK 26, Target SDK 35).
- **ספריות ממשק ועיצוב**: Material Design 3, ConstraintLayout, AndroidX.
- **צילום ומדיה**: AndroidX CameraX (`camera-camera2`, `camera-lifecycle`, `camera-view`), Google ML Kit Text Recognition.
- **תקשורת ורשת**: OkHttp 4 לשליחת בקשות רשת אסינכרוניות ל-Gemini REST API.
- **Backend וענן**: 
  - Firebase Authentication (Google Sign-In + Email/Password)
  - Cloud Firestore (מסד נתונים NoSQL עם אבטחת נתונים לפי משתמש)
- **טעינת תמונות**: Bumptech Glide.

---

## מבנה התיקיות בקוד (`com.yalig.snapstudy`)

```
com.yalig.snapstudy/
├── ai/
│   ├── GeminiConfig.java          # הגדרות מפתח ה-API והפרמטרים של Gemini
│   └── GeminiStudyService.java     # שירות שליחת תמונות ופיענוח כרטיסיות מה-AI
├── data/
│   └── StudyRepository.java        # ניהול נתונים (שמירה מקומית וסנכרון מול Firestore)
├── models/
│   ├── Flashcard.java              # מודל כרטיסיית לימוד (שאלה, תשובה, נתוני חזרה)
│   └── StudySet.java               # מודל סט כרטיסיות (שם, תאריך עדכון, רשימת כרטיסיות)
├── ui/
│   └── DocumentOverlayView.java    # תצוגה מותאמת אישית (Custom View) לציור מסגרת סריקה
├── utils/
│   └── ViewAnimationUtils.java     # אנימציות מגע (Spring), כניסה מדורגת ומעברי מסכים
├── FlashcardsActivity.java         # מסך תרגול כרטיסיות והיפוך תלת-ממדי
├── LoginActivity.java              # מסך התחברות והרשמה (Google / Email)
├── MainActivity.java               # מסך ראשי, רשימת הסטים ומעקב התקדמות
├── ScannerActivity.java            # מסך סריקה ומצלמה (CameraX + ML Kit)
├── SettingsActivity.java           # מסך הגדרות, ערכת נושא ותזכורות
└── SnapStudyApp.java               # מחלקת Application גלובלית (ניהול ערכת נושא ונעילת כיוון)
```

---

## איך להריץ את הפרויקט?

1. **שכפול המאגר**:
   ```bash
   git clone https://github.com/yaheli-mil/SnapStudy.git
   ```
2. **פתיחה ב-Android Studio**:
   - פותחים את Android Studio ובוחרים ב-`Open` על תיקיית הפרויקט.
   - ממתינים לסיום סנכרון ה-Gradle.
3. **הגדרת Firebase**:
   - הפרויקט מבוסס על Firebase עבור התחברות ו-Firestore.
   - יש לוודא שקובץ `google-services.json` נמצא בתיקיית `app/`.
4. **מפתח Gemini API**:
   - מפתח ה-API מוגדר בקובץ [`GeminiConfig.java`](app/src/main/java/com/yalig/snapstudy/ai/GeminiConfig.java).
5. **הרצה**:
   - מחברים מכשיר אנדרואיד פיזי (מומלץ לבדיקת מצלמה) או מפעילים אמולטור, ולוחצים על **Run** (`Shift + F10`).

---

## מחבר

**יהלי מילרמן**  
פרויקט גמר 5 יח"ל בהנדסת תוכנה.
