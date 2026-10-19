package com.yalig.snapstudy.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.yalig.snapstudy.SnapStudyApp;
import com.yalig.snapstudy.models.Flashcard;
import com.yalig.snapstudy.models.StudySet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * שכבת מאגר הנתונים המרכזית (Repository Pattern) של האפליקציה.
 * מחלקה זו מנהלת את כל נתוני המערכת: ערכות הלימוד, כרטיסיות הזיכרון, וסטטיסטיקות המשתמש.
 * היא מיישמת ארכיטקטורת Offline-First:
 * 1. שמירה וטעינה מקומית מיידית דרך SharedPreferences (בפורמט JSON) לחוויית משתמש חלקה ללא השהיות.
 * 2. סנכרון דו-כיווני מאובטח מול בסיס הנתונים בענן Cloud Firestore של Firebase.
 * המחלקה בנויה בתבנית Singleton כדי להבטיח מופע יחיד ומקור אמת אחד (Single Source of Truth) לכל חלקי האפליקציה.
 */
public class StudyRepository {

    // תגית לוגים לניפוי שגיאות
    private static final String TAG = "StudyRepository";

    // המופע היחיד של ה-Repository (Singleton)
    private static StudyRepository instance;

    // הפנייה למופע בסיס הנתונים בענן Cloud Firestore
    private final FirebaseFirestore db;

    // רשימת ערכות הלימוד השמורות בזיכרון התוכנית (RAM) לגישה מהירה
    private final List<StudySet> studySets;

    // נתוני סטטיסטיקה ומעקב למידה של המשתמש
    private int streakDays = 0;           // רצף ימי למידה
    private int cardsStudiedToday = 0;    // מספר כרטיסיות שתורגלו היום
    private int dailyGoal = 50;           // יעד כרטיסיות יומי (ברירת מחדל: 50)
    private int focusMinutes = 0;         // דקות למידה וריכוז מצטברות
    private String lastStudyDate = "";    // תאריך הלמידה האחרון (yyyy-MM-dd)
    private boolean isLoaded = false;     // דגל המציין האם הנתונים כבר נטענו

    /**
     * ממשק Callback להודעה כאשר פעולת סנכרון הרשת מול Firestore הושלמה
     */
    public interface DataCallback {
        void onComplete();
    }

    /**
     * בנאי פרטי למניעת יצירת מופעים מחוץ למחלקה (מימוש תבנית Singleton).
     */
    private StudyRepository() {
        db = FirebaseFirestore.getInstance();
        studySets = new ArrayList<>();
    }

    /**
     * מחזירה את המופע היחיד והמשותף של ה-Repository.
     * מסומנת ב-synchronized למניעת מצבי מרוץ (Race Conditions) בעבודה בתהליכונים שונים.
     * @return מופע StudyRepository.
     */
    public static synchronized StudyRepository getInstance() {
        if (instance == null) {
            instance = new StudyRepository();
        }
        return instance;
    }

    /**
     * בודקת האם המאגר כבר סיים טעינה ראשונית.
     */
    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * טוענת את המידע המקומי מקובץ ה-SharedPreferences של המכשיר.
     * פעולה זו מהירה ביותר ומאפשרת למסך הבית להציג את הערכות מיד עם פתיחת האפליקציה.
     * @param uid מזהה המשתמש הייחודי ב-Firebase Auth.
     */
    public void loadLocalData(String uid) {
        Context context = SnapStudyApp.getInstance();
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences("study_repo_" + (uid != null ? uid : "default"), Context.MODE_PRIVATE);
        String json = prefs.getString("study_sets_json", null);

        // אם אין עדיין מידע למשתמש זה, נבדוק בגיבוי המקומי של המכשיר
        if (json == null || json.trim().isEmpty()) {
            SharedPreferences devicePrefs = context.getSharedPreferences("study_repo_device_backup", Context.MODE_PRIVATE);
            json = devicePrefs.getString("last_known_sets", null);
        }

        // פענוח ה-JSON לאובייקטי StudySet ואכלוס הרשימה בזיכרון
        if (json != null && !json.trim().isEmpty()) {
            List<StudySet> loaded = jsonToSets(json);
            if (!loaded.isEmpty()) {
                studySets.clear();
                studySets.addAll(loaded);
                isLoaded = true;
                Log.d(TAG, "Loaded " + loaded.size() + " sets from local cache for uid: " + uid);
            }
        }

        // טעינת מדדי הלמידה השמורים
        streakDays = prefs.getInt("streak_days", streakDays);
        cardsStudiedToday = prefs.getInt("cards_studied", cardsStudiedToday);
        dailyGoal = prefs.getInt("daily_goal", dailyGoal);
        focusMinutes = prefs.getInt("focus_minutes", focusMinutes);
        lastStudyDate = prefs.getString("last_study_date", lastStudyDate);

        // אם התחיל יום חדש - המונה היומי יוצג כ-0
        rollOverDayIfNeeded();
    }

    /**
     * שומרת את כל ערכות הלימוד והסטטיסטיקות בקובץ SharedPreferences מקומי כ-JSON.
     * @param uid מזהה המשתמש.
     */
    public void saveLocalData(String uid) {
        Context context = SnapStudyApp.getInstance();
        if (context == null) return;

        String json = setsToJson(studySets);
        SharedPreferences prefs = context.getSharedPreferences("study_repo_" + (uid != null ? uid : "default"), Context.MODE_PRIVATE);
        prefs.edit()
                .putString("study_sets_json", json)
                .putInt("streak_days", streakDays)
                .putInt("cards_studied", cardsStudiedToday)
                .putInt("daily_goal", dailyGoal)
                .putInt("focus_minutes", focusMinutes)
                .putString("last_study_date", lastStudyDate)
                .apply();

        // שמירת עותק נוסף למגירת הגיבוי של המכשיר
        if (!studySets.isEmpty()) {
            SharedPreferences devicePrefs = context.getSharedPreferences("study_repo_device_backup", Context.MODE_PRIVATE);
            devicePrefs.edit()
                    .putString("last_known_sets", json)
                    .apply();
        }
    }

    /**
     * מסנכרנת את נתוני המשתמש וערכות הלימוד מול בסיס הנתונים בענן Cloud Firestore.
     * התהליך כולל:
     * 1. שליפת מדדי המשתמש (Streak, יעד יומי) מענן ועדכונם המקומי.
     * 2. שליפת ערכות הלימוד מתת-האוסף study_sets של המשתמש.
     * 3. מיזוג (Merge) של ערכות שנוצרו במצב לא-מקוון והעלאתן לענן.
     * @param callback ממשק להודעה בסיום הסנכרון.
     */
    public void syncWithFirestore(DataCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onComplete();
            return;
        }

        String uid = user.getUid();

        // וידוא שטעינה מקומית בוצעה כדי שהמסך לא יהיה ריק
        if (studySets.isEmpty()) {
            loadLocalData(uid);
        }

        // שלב א': סנכרון מדדי המשתמש הכלליים
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long streak = documentSnapshot.getLong("streakDays");
                        Long studied = documentSnapshot.getLong("cardsStudiedToday");
                        Long goal = documentSnapshot.getLong("dailyGoal");
                        Long focus = documentSnapshot.getLong("focusMinutes");
                        String lastDate = documentSnapshot.getString("lastStudyDate");

                        if (streak != null) streakDays = streak.intValue();
                        if (studied != null) cardsStudiedToday = studied.intValue();
                        if (goal != null) dailyGoal = goal.intValue();
                        if (focus != null) focusMinutes = focus.intValue();
                        if (lastDate != null) lastStudyDate = lastDate;

                        // הערכים מהענן עלולים להיות מיום קודם
                        rollOverDayIfNeeded();
                        saveLocalData(uid);
                    } else {
                        // אם זה משתמש חדש לגמרי, ניצור עבורו מסמך ראשוני בענן
                        saveUserStatsToFirestore(uid, user.getDisplayName(), user.getEmail());
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error loading user stats", e));

        // שלב ב': סנכרון ערכות הלימוד מתת-האוסף study_sets
        db.collection("users").document(uid).collection("study_sets").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<StudySet> remoteSets = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        // ניקוי ערכות דוגמה ישנות אם נותרו
                        if ("bio_101".equals(id) || "econ_101".equals(id) || "alg_101".equals(id)) {
                            db.collection("users").document(uid)
                                    .collection("study_sets").document(id).delete();
                            continue;
                        }
                        StudySet set = doc.toObject(StudySet.class);
                        if (set != null) {
                            if (set.getId() == null) {
                                set.setId(doc.getId());
                            }
                            sanitizeStudySet(set);
                            remoteSets.add(set);
                        }
                    }

                    // שלב ג': מיזוג ערכות מקומיות שטרם עלו לענן ושמירתן ב-Firestore
                    for (StudySet localSet : studySets) {
                        boolean exists = false;
                        for (StudySet remoteSet : remoteSets) {
                            if (remoteSet.getId() != null && remoteSet.getId().equals(localSet.getId())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            remoteSets.add(0, localSet);
                            sanitizeStudySet(localSet);
                            db.collection("users").document(uid)
                                    .collection("study_sets").document(localSet.getId()).set(localSet)
                                    .addOnFailureListener(e -> Log.w(TAG, "Failed syncing local set to Firestore", e));
                        }
                    }

                    // עדכון הרשימה בזיכרון ובקאש המקומי
                    studySets.clear();
                    studySets.addAll(remoteSets);
                    saveLocalData(uid);
                    isLoaded = true;
                    if (callback != null) callback.onComplete();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error loading study sets from Firestore", e);
                    isLoaded = true;
                    if (callback != null) callback.onComplete();
                });
    }

    /**
     * שמירה ראשונית של פרטי המשתמש ב-Firestore עבור משתמש שנרשם זה עתה.
     */
    private void saveUserStatsToFirestore(String uid, String displayName, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("streakDays", streakDays);
        userData.put("cardsStudiedToday", cardsStudiedToday);
        userData.put("dailyGoal", dailyGoal);
        userData.put("focusMinutes", focusMinutes);
        userData.put("lastStudyDate", lastStudyDate);
        if (displayName != null) userData.put("displayName", displayName);
        if (email != null) userData.put("email", email);

        db.collection("users").document(uid).set(userData)
                .addOnFailureListener(e -> Log.w(TAG, "Error saving initial user stats", e));
    }

    /**
     * מחזירה עותק של כל ערכות הלימוד הקיימות במערכת.
     * מחזירה עותק חדש כדי להגן על שלמות הרשימה המקורית (Encapsulation).
     * @return רשימת אובייקטי StudySet.
     */
    public List<StudySet> getAllStudySets() {
        return new ArrayList<>(studySets);
    }

    /**
     * מאתרת ערכת לימוד לפי המזהה הייחודי שלה.
     * @param id מזהה הערכה.
     * @return ערכת הלימוד אם נמצאה, או הערכה הראשונה כברירת מחדל.
     */
    public StudySet getStudySetById(String id) {
        for (StudySet set : studySets) {
            if (set.getId() != null && set.getId().equals(id)) {
                return set;
            }
        }
        return studySets.isEmpty() ? null : studySets.get(0);
    }

    /**
     * מוסיפה ערכת לימוד חדשה למאגר, שומרת אותה מקומית ומסנכרנת לענן Firestore.
     * @param set ערכת הלימוד להוספה.
     */
    public void addStudySet(StudySet set) {
        if (set == null) return;
        sanitizeStudySet(set);
        if (set.getId() == null || set.getId().isEmpty()) {
            set.setId(UUID.randomUUID().toString());
        }

        // הסרת ערכה בעלת מזהה זהה אם קיימת, והוספה בראש הרשימה
        for (int i = 0; i < studySets.size(); i++) {
            if (studySets.get(i).getId() != null && studySets.get(i).getId().equals(set.getId())) {
                studySets.remove(i);
                break;
            }
        }
        studySets.add(0, set);

        // שמירה מקומית מיידית
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user != null ? user.getUid() : "default";
        saveLocalData(uid);

        // סנכרון למסמך המשתמש ב-Cloud Firestore
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .collection("study_sets").document(set.getId()).set(set)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Successfully persisted set " + set.getId()))
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to persist set to Firestore", e));
        }
    }

    /**
     * מעדכנת ערכת לימוד קיימת לאחר תרגול או עריכה.
     * @param set הערכה המעודכנת.
     */
    public void updateStudySet(StudySet set) {
        if (set == null) return;
        sanitizeStudySet(set);
        for (int i = 0; i < studySets.size(); i++) {
            if (studySets.get(i).getId() != null && studySets.get(i).getId().equals(set.getId())) {
                studySets.set(i, set);
                break;
            }
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user != null ? user.getUid() : "default";
        saveLocalData(uid);

        if (user != null) {
            db.collection("users").document(user.getUid())
                    .collection("study_sets").document(set.getId()).set(set)
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to update set in Firestore", e));
        }
    }

    /**
     * פונקציית סניטיזציה ובדיקת תקינות נתונים:
     * מוודאת ששדות הכותרת, הקטגוריה והכרטיסיות אינם ריקים או ארוכים מדי,
     * למניעת שגיאות תצוגה ושגיאות שמירה ב-Firestore.
     * @param set הערכה לבדיקה ותיקון.
     */
    private void sanitizeStudySet(StudySet set) {
        if (set == null) return;
        if (set.getTitle() == null || set.getTitle().trim().isEmpty()) {
            set.setTitle("ערכת לימוד");
        } else if (set.getTitle().length() > 190) {
            set.setTitle(set.getTitle().substring(0, 190));
        }

        if (set.getCategory() == null || set.getCategory().trim().isEmpty()) {
            set.setCategory("כללי");
        } else if (set.getCategory().length() > 90) {
            set.setCategory(set.getCategory().substring(0, 90));
        }

        if (set.getUpdatedInfo() == null || set.getUpdatedInfo().trim().isEmpty()) {
            set.setUpdatedInfo("נוצר זה עתה");
        } else if (set.getUpdatedInfo().length() > 90) {
            set.setUpdatedInfo(set.getUpdatedInfo().substring(0, 87) + "...");
        }

        if (set.getFlashcards() == null) {
            set.setFlashcards(new ArrayList<>());
        }
        for (Flashcard fc : set.getFlashcards()) {
            if (fc.getQuestion() == null) fc.setQuestion("");
            if (fc.getAnswer() == null) fc.setAnswer("");
            if (fc.getTopic() == null) fc.setTopic(set.getCategory());
        }
    }

    // --- שיטות שליפת מדדי למידה ---

    public int getStreakDays() { return streakDays; }
    public int getCardsStudiedToday() { return cardsStudiedToday; }
    public int getDailyGoal() { return dailyGoal; }
    public int getFocusMinutes() { return focusMinutes; }

    /**
     * מעדכנת את היעד היומי לכרטיסיות, שומרת מקומית ומעדכנת בענן.
     * @param goal מספר הכרטיסיות הרצוי ליום.
     */
    public void setDailyGoal(int goal) {
        this.dailyGoal = goal;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user != null ? user.getUid() : "default";
        saveLocalData(uid);

        if (user != null) {
            db.collection("users").document(user.getUid())
                    .update("dailyGoal", dailyGoal)
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to update daily goal in Firestore", e));
        }
    }

    /**
     * מקדמת ב-1 את מונה הכרטיסיות שנלמדו היום, מעדכנת את רצף הימים ומעדכנת את ה-Firestore.
     */
    public void recordCardStudied() {
        // ביום חדש: איפוס המונה היומי (ואיפוס הרצף אם דילגו על יום)
        rollOverDayIfNeeded();

        // הלמידה הראשונה היום: אם למדו אתמול הרצף ממשיך, אחרת הוא מתחיל מחדש מ-1
        String today = getDateString(0);
        if (!today.equals(lastStudyDate)) {
            streakDays++;
            lastStudyDate = today;
        }

        cardsStudiedToday++;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user != null ? user.getUid() : "default";
        saveLocalData(uid);

        if (user != null) {
            db.collection("users").document(user.getUid())
                    .update("cardsStudiedToday", cardsStudiedToday,
                            "streakDays", streakDays,
                            "lastStudyDate", lastStudyDate)
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to update studied count in Firestore", e));
        }
    }

    /**
     * בודקת האם התחיל יום חדש מאז הלמידה האחרונה.
     * ביום חדש המונה היומי מתאפס, ואם לא למדו אתמול - גם רצף הימים מתאפס.
     */
    private void rollOverDayIfNeeded() {
        if (getDateString(0).equals(lastStudyDate)) return;
        cardsStudiedToday = 0;
        if (!getDateString(-1).equals(lastStudyDate)) {
            streakDays = 0;
        }
    }

    /**
     * מחזירה תאריך בפורמט yyyy-MM-dd ביחס להיום.
     * @param daysOffset הזזה בימים (0 = היום, -1 = אתמול).
     * @return מחרוזת התאריך.
     */
    private String getDateString(int daysOffset) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, daysOffset);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
    }

    /**
     * מאפסת את נתוני המאגר המקומי (למשל בעת התנתקות משתמש מהמערכת).
     */
    public void clearData() {
        studySets.clear();
        streakDays = 0;
        cardsStudiedToday = 0;
        dailyGoal = 50;
        focusMinutes = 0;
        lastStudyDate = "";
        isLoaded = false;
    }

    /**
     * ממירה רשימת ערכות לימוד למחרוזת JSON מובנית לשמירה מקומית ב-SharedPreferences.
     * @param sets רשימת הערכות.
     * @return מחרוזת JSON.
     */
    private String setsToJson(List<StudySet> sets) {
        JSONArray arr = new JSONArray();
        if (sets == null) return arr.toString();
        for (StudySet set : sets) {
            if (set == null) continue;
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", set.getId() != null ? set.getId() : UUID.randomUUID().toString());
                obj.put("title", set.getTitle() != null ? set.getTitle() : "ערכת לימוד");
                obj.put("category", set.getCategory() != null ? set.getCategory() : "כללי");
                obj.put("updatedInfo", set.getUpdatedInfo() != null ? set.getUpdatedInfo() : "נוצר זה עתה");
                obj.put("masteryPercentage", set.getMasteryPercentage());

                JSONArray cardsArr = new JSONArray();
                if (set.getFlashcards() != null) {
                    for (Flashcard card : set.getFlashcards()) {
                        if (card == null) continue;
                        JSONObject cardObj = new JSONObject();
                        cardObj.put("id", card.getId() != null ? card.getId() : "");
                        cardObj.put("question", card.getQuestion() != null ? card.getQuestion() : "");
                        cardObj.put("answer", card.getAnswer() != null ? card.getAnswer() : "");
                        cardObj.put("topic", card.getTopic() != null ? card.getTopic() : set.getCategory());
                        cardObj.put("intervalDays", card.getIntervalDays());
                        cardObj.put("repetitionCount", card.getRepetitionCount());
                        cardObj.put("difficulty", card.getDifficulty());
                        cardsArr.put(cardObj);
                    }
                }
                obj.put("flashcards", cardsArr);
                arr.put(obj);
            } catch (Exception e) {
                Log.e(TAG, "Error serializing set to JSON", e);
            }
        }
        return arr.toString();
    }

    /**
     * מפענחת מחרוזת JSON ומייצרת ממנה רשימה של אובייקטי StudySet ו-Flashcard.
     * @param json מחרוזת ה-JSON המקומית.
     * @return רשימת ערכות לימוד.
     */
    private List<StudySet> jsonToSets(String json) {
        List<StudySet> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                StudySet set = new StudySet(
                        obj.optString("id", UUID.randomUUID().toString()),
                        obj.optString("title", "ערכת לימוד"),
                        obj.optString("category", "כללי"),
                        obj.optString("updatedInfo", "נוצר זה עתה"),
                        obj.optInt("masteryPercentage", 0)
                );
                JSONArray cardsArr = obj.optJSONArray("flashcards");
                if (cardsArr != null) {
                    for (int j = 0; j < cardsArr.length(); j++) {
                        JSONObject cObj = cardsArr.getJSONObject(j);
                        Flashcard fc = new Flashcard(
                                cObj.optString("id", "fc_" + (j + 1)),
                                cObj.optString("question", ""),
                                cObj.optString("answer", ""),
                                cObj.optString("topic", set.getCategory())
                        );
                        fc.setIntervalDays(cObj.optInt("intervalDays", 1));
                        fc.setRepetitionCount(cObj.optInt("repetitionCount", 0));
                        fc.setDifficulty(cObj.optInt("difficulty", 2));
                        set.addFlashcard(fc);
                    }
                }
                list.add(set);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing sets from JSON", e);
        }
        return list;
    }
}


