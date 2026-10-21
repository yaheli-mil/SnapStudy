# SnapStudy

אפליקציית אנדרואיד (Native Java) ללמידה ותרגול בעזרת כרטיסיות חכמות (Flashcards), שפותחה כפרויקט גמר 5 יח"ל בהנדסת תוכנה.

הרעיון המרכזי של האפליקציה הוא לקצר את הזמן שלוקח להכין כרטיסיות לימוד: במקום להקליד שאלות ותשובות ידנית, מצלמים עמוד מספר לימוד או מחברת (או בוחרים תמונה מהגלריה), והאפליקציה מפענחת את הטקסט ומייצרת כרטיסיות שאלות ותשובות ממוקדות באמצעות Google Gemini API.

---

## מה האפליקציה כוללת?

- **סורק מסמכים חכם (`ScannerActivity`)**:
  - צילום דף באפליקציית המצלמה של הטלפון (`ActivityResultContracts.TakePicture` עם `FileProvider`), או בחירת תמונה מהגלריה (`GetContent`).
  - בקשת הרשאת מצלמה בזמן ריצה, רק כשלוחצים על "צלם".
  - זיהוי טקסט על גבי המכשיר באמצעות **Google ML Kit Text Recognition**, בתמונה שצולמה או נבחרה.
  - שכבת תצוגה מותאמת אישית ([`DocumentOverlayView`](app/src/main/java/com/yalig/snapstudy/ui/DocumentOverlayView.java)) שמציירת את התמונה ומסגרת אדומה סביב הטקסט שזוהה.

- **יצירת כרטיסיות עם Gemini AI (`GeminiStudyService`)**:
  - שליחת התמונה ב-Base64 בבקשת HTTP (`HttpURLConnection` ב-Thread נפרד) מול מודל `gemini-2.5-flash` של Google.
  - אכיפת פלט JSON מובנה (`response_mime_type: application/json`) שמחזיר כותרת לסט ורשימת כרטיסיות (שאלה + תשובה).
  - עיבוד התמונה והקטנת הרזולוציה לפני השליחה כדי לחסוך ברוחב פס ולמנוע שגיאות זיכרון (OutOfMemory).

- **תרגול כרטיסיות פעיל (`FlashcardsActivity`)**:
  - לימוד בשיטת Active Recall: הכרטיס מסתובב סביב ציר Y (`rotationYBy`) וחושף את התשובה.
  - מענה לפי 4 רמות קושי (שוב, קשה, טוב, קל). כל דירוג מעדכן את הכרטיסייה (רמת קושי, מספר חזרות ומרווח חזרה מומלץ: עוד היום / יומיים / 4 ימים / 7 ימים) ואת אחוז השליטה בסט, וההתקדמות נשמרת ב-Firestore.
  - מרווח החזרה נשמר לכל כרטיסייה, אבל עדיין אין תזמון חזרות מלא (Spaced Repetition): כל תרגול מציג את כל הכרטיסיות בסט.
  - צליל קצר בתשובה נכונה ("טוב" או "קל"), שאפשר לכבות במסך ההגדרות.
  - חלון סיום בסוף התרגול עם ציון: כמה כרטיסיות סומנו "טוב" או "קל" ואחוז ההצלחה.

- **לוח בקרה ומעקב למידה (`MainActivity`)**:
  - הצגת הסטים ב-`RecyclerView` (עם [`StudySetAdapter`](app/src/main/java/com/yalig/snapstudy/ui/StudySetAdapter.java)) וחיפוש מהיר תוך כדי הקלדה.
  - מעקב אחר רצף ימי למידה (Daily Streak), יעד כרטיסיות יומי ודקות ריכוז. הנתונים נשמרים בטלפון ב-`SharedPreferences` ([`StatsManager`](app/src/main/java/com/yalig/snapstudy/data/StatsManager.java)).
  - אפשרות לפתוח סט קיים לתרגול או להוסיף סט חדש ידנית.
  - שינוי שם של סט ומחיקתו עדיין לא קיימים באפליקציה ומתוכננים להמשך ([Issue #28](https://github.com/yaheli-mil/SnapStudy/issues/28)).

- **אימות ושמירה בענן (Firebase)**:
  - התחברות מהירה עם חשבון Google באמצעות ספריית `CredentialManager` של אנדרואיד.
  - הרשמה והתחברות רגילה עם מייל וסיסמה, כולל מנגנון שחזור סיסמה.
  - מחלקת עזר ([`StudyRepository`](app/src/main/java/com/yalig/snapstudy/data/StudyRepository.java)) שטוענת ושומרת את הסטים של המשתמש ב-**Cloud Firestore** (`users/{uid}/study_sets/{setId}`).

- **הגדרות והתאמה אישית (`SettingsActivity`)**:
  - מעבר בין ערכת נושא בהירה (Light), כהה (Dark) או מעקב אחרי הגדרות המערכת (Auto) באמצעות `AppCompatDelegate`.
  - קביעת שעת תזכורת יומית ללימוד בעזרת `TimePickerDialog`. השעה נשמרת, ושליחת ההתראה עצמה עוד לא קיימת ומתוכננת ב-[Issue #27](https://github.com/yaheli-mil/SnapStudy/issues/27).
  - התאמת יעד הכרטיסיות היומי.
  - אפשרות לכיבוי והדלקת צליל התשובה הנכונה בתרגול.

---

## טכנולוגיות וספריות בפרויקט

- **שפה וסביבה**: Java 11, Android SDK (Min SDK 34, Target SDK 37).
- **ספריות ממשק ועיצוב**: Material Design 3, ConstraintLayout, RecyclerView, AndroidX.
- **צילום ומדיה**: Activity Result API (`TakePicture`, `GetContent`, `RequestPermission`), `FileProvider`, Google ML Kit Text Recognition.
- **תקשורת ורשת**: `HttpURLConnection` (חלק מ-Java) ב-Thread נפרד, לשליחת בקשות ל-Gemini REST API.
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
│   ├── StatsManager.java           # סטטיסטיקות למידה בטלפון (רצף, כרטיסיות היום, יעד, דקות ריכוז)
│   └── StudyRepository.java        # טעינה ושמירה של הסטים ב-Firestore
├── models/
│   ├── Flashcard.java              # מודל כרטיסיית לימוד (שאלה, תשובה, נתוני חזרה)
│   └── StudySet.java               # מודל סט כרטיסיות (שם, תאריך עדכון, רשימת כרטיסיות)
├── ui/
│   ├── DocumentOverlayView.java    # Custom View שמצייר את התמונה ומסגרת סביב הטקסט
│   └── StudySetAdapter.java        # Adapter לרשימת הסטים ב-RecyclerView
├── FlashcardsActivity.java         # מסך תרגול כרטיסיות ודירוג קושי
├── LoginActivity.java              # מסך התחברות והרשמה (Google / Email)
├── MainActivity.java               # מסך ראשי, רשימת הסטים ומעקב התקדמות
├── ScannerActivity.java            # מסך סריקה: מצלמה / גלריה + ML Kit + Gemini
├── SettingsActivity.java           # מסך הגדרות, ערכת נושא ותזכורות
└── SnapStudyApp.java               # מחלקת Application (ערכת נושא והגדרות המשתמש)
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
