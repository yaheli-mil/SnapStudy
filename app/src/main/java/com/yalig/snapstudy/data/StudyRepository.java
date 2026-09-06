package com.yalig.snapstudy.data;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.yalig.snapstudy.models.Flashcard;
import com.yalig.snapstudy.models.StudySet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyRepository {
    private static final String TAG = "StudyRepository";
    private static StudyRepository instance;

    private final FirebaseFirestore db;
    private final List<StudySet> studySets;
    private int streakDays = 0;
    private int cardsStudiedToday = 0;
    private int dailyGoal = 50;
    private int focusMinutes = 0;
    private boolean isLoaded = false;

    public interface DataCallback {
        void onComplete();
    }

    private StudyRepository() {
        db = FirebaseFirestore.getInstance();
        studySets = new ArrayList<>();
    }

    public static synchronized StudyRepository getInstance() {
        if (instance == null) {
            instance = new StudyRepository();
        }
        return instance;
    }

    public void syncWithFirestore(DataCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onComplete();
            return;
        }

        String uid = user.getUid();

        // 1. Sync User Stats
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long streak = documentSnapshot.getLong("streakDays");
                        Long studied = documentSnapshot.getLong("cardsStudiedToday");
                        Long goal = documentSnapshot.getLong("dailyGoal");
                        Long focus = documentSnapshot.getLong("focusMinutes");

                        // If user has old mock stats, reset to clean new user
                        if (streak != null && streak == 5 && studied != null && studied == 120 && focus != null && focus == 42) {
                            streakDays = 0;
                            cardsStudiedToday = 0;
                            focusMinutes = 0;
                            saveUserStatsToFirestore(uid, user.getDisplayName(), user.getEmail());
                        } else {
                            if (streak != null) streakDays = streak.intValue();
                            if (studied != null) cardsStudiedToday = studied.intValue();
                            if (goal != null) dailyGoal = goal.intValue();
                            if (focus != null) focusMinutes = focus.intValue();
                        }
                    } else {
                        saveUserStatsToFirestore(uid, user.getDisplayName(), user.getEmail());
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error loading user stats", e));

        // 2. Sync Study Sets
        db.collection("users").document(uid).collection("study_sets").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<StudySet> remoteSets = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        // Clean up previously seeded mock sample sets from Firestore
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
                            remoteSets.add(set);
                        }
                    }
                    
                    // Merge local sets that haven't synced to Firestore yet
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
                        }
                    }

                    studySets.clear();
                    studySets.addAll(remoteSets);
                    isLoaded = true;
                    if (callback != null) callback.onComplete();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error loading study sets from Firestore", e);
                    if (callback != null) callback.onComplete();
                });
    }

    private void saveUserStatsToFirestore(String uid, String displayName, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("streakDays", streakDays);
        userData.put("cardsStudiedToday", cardsStudiedToday);
        userData.put("dailyGoal", dailyGoal);
        userData.put("focusMinutes", focusMinutes);
        if (displayName != null) userData.put("displayName", displayName);
        if (email != null) userData.put("email", email);

        db.collection("users").document(uid).set(userData)
                .addOnFailureListener(e -> Log.w(TAG, "Error saving initial user stats", e));
    }

    private void initSampleData() {
        // Study Set 1: Biology
        StudySet bioSet = new StudySet(
                "bio_101",
                "ביולוגיה של התא וגנטיקה",
                "ביולוגיה",
                "עודכן לאחרונה אתמול",
                84
        );
        bioSet.addFlashcard(new Flashcard("b1", "מהו תפקידו העיקרי של המיטוכונדריון בתא האאוקריוטי?", "ייצור מולקולות ATP (אנרגיה זמינה לתא) בתהליך הנשימה התאית האוורובית.", "מבנה התא והאברונים"));
        bioSet.addFlashcard(new Flashcard("b2", "מה ההבדל המרכזי בין תא פרוקריוטי לתא אאוקריוטי?", "תא אאוקריוטי מכיל גרעין מוגדר בקרום ואברונים עטופי קרום, בעוד שלתא פרוקריוטי אין גרעין מוגדר או אברונים כאלה.", "מבנה התא והאברונים"));
        bioSet.addFlashcard(new Flashcard("b3", "מהו תפקיד הריבוזום בתא?", "תרגום מולקולות mRNA וסינתזה של שרשראות חלבונים.", "סינתזת חלבונים"));
        bioSet.addFlashcard(new Flashcard("b4", "מהי פוטוסינתזה ובאיזה אברון היא מתרחשת?", "תהליך המרת אנרגיית שמש לאנרגיה כימית (גלוקוז וחמצן), מתרחש בכלורופלסט בתאי צמחים.", "תהליכים אנרגטיים"));
        bioSet.addFlashcard(new Flashcard("b5", "מה ההבדל בין מיטוזה למיוזה?", "מיטוזה יוצרת 2 תאים דיפלואידיים זהים גנטית (לתאי גוף). מיוזה יוצרת 4 תאים הפלואידיים שונים גנטית (תאי רבייה).", "חלוקת התא"));
        studySets.add(bioSet);

        // Study Set 2: Economics
        StudySet econSet = new StudySet(
                "econ_101",
                "מבוא למאקרו-כלכלה",
                "כלכלה",
                "14 לתרגול היום",
                42
        );
        econSet.addFlashcard(new Flashcard("e1", "מהו תוצר מקומי גולמי (תמ\"ג / GDP)?", "סך הערך הכספי של כל המוצרים והשירותים הסופיים שיוצרו במדינה בתקופה מסוימת (בדרך כלל שנה).", "מושגי יסוד"));
        econSet.addFlashcard(new Flashcard("e2", "מהו חוק הביקוש?", "ככל שמחירו של מוצר עולה, הכמות המבוקשת ממנו יורדת, ולהיפך (בהנחה ששאר התנאים קבועים).", "היצע וביקוש"));
        econSet.addFlashcard(new Flashcard("e3", "מהי אינפלציה וכיצד מודדים אותה?", "עלייה מתמשכת ברמת המחירים הכללית במשק, נמדדת לרוב באמצעות מדד המחירים לצרכן (CPI).", "כסף ובנקאות"));
        econSet.addFlashcard(new Flashcard("e4", "מה ההבדל בין מדיניות פיסקלית למדיניות מוניטרית?", "מדיניות פיסקלית מנוהלת על ידי הממשלה (מיסים ותקציב), בעוד מדיניות מוניטרית מנוהלת על ידי הבנק המרכזי (ריבית והיצע כסף).", "מדיניות כלכלית"));
        studySets.add(econSet);

        // Study Set 3: Linear Algebra
        StudySet algSet = new StudySet(
                "alg_101",
                "אלגברה לינארית: מרחבים וקטוריים",
                "מתמטיקה",
                "מושלם! חזרה מומלצת בעוד שבוע",
                95
        );
        algSet.addFlashcard(new Flashcard("a1", "מתי קבוצת וקטורים נקראת תלויה לינארית?", "כאשר קיים צירוף לינארי שאינו טריוויאלי (שלפחות אחד ממקדמיו שונה מאפס) שמתאפס לוקטור האפס.", "תלות לינארית"));
        algSet.addFlashcard(new Flashcard("a2", "מהו הממד (Dimension) של מרחב וקטורי?", "מספר הוקטורים המרכיבים בסיס כלשהו של המרחב הוקטורי.", "בסיס וממד"));
        algSet.addFlashcard(new Flashcard("a3", "מהי מטריצה הפיכה (Invertible)?", "מטריצה ריבועית A שקיימת עבורה מטריצה B כך ש-A*B = B*A = I. תנאי שקול: הדטרמיננטה של A שונה מאפס.", "מטריצות"));
        studySets.add(algSet);
    }

    public List<StudySet> getAllStudySets() {
        return new ArrayList<>(studySets);
    }

    public StudySet getStudySetById(String id) {
        for (StudySet set : studySets) {
            if (set.getId().equals(id)) {
                return set;
            }
        }
        return studySets.isEmpty() ? null : studySets.get(0);
    }

    public void addStudySet(StudySet set) {
        if (set.getId() == null || set.getId().isEmpty()) {
            set.setId(java.util.UUID.randomUUID().toString());
        }
        for (int i = 0; i < studySets.size(); i++) {
            if (studySets.get(i).getId() != null && studySets.get(i).getId().equals(set.getId())) {
                studySets.remove(i);
                break;
            }
        }
        studySets.add(0, set);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .collection("study_sets").document(set.getId()).set(set)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Successfully persisted set " + set.getId()))
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to persist set to Firestore", e));
        }
    }

    public void updateStudySet(StudySet set) {
        for (int i = 0; i < studySets.size(); i++) {
            if (studySets.get(i).getId().equals(set.getId())) {
                studySets.set(i, set);
                break;
            }
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .collection("study_sets").document(set.getId()).set(set)
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to update set in Firestore", e));
        }
    }

    public int getStreakDays() { return streakDays; }
    public int getCardsStudiedToday() { return cardsStudiedToday; }
    public int getDailyGoal() { return dailyGoal; }
    public int getFocusMinutes() { return focusMinutes; }

    public void setDailyGoal(int goal) {
        this.dailyGoal = goal;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .update("dailyGoal", dailyGoal)
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to update daily goal in Firestore", e));
        }
    }

    public void recordCardStudied() {
        cardsStudiedToday++;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .update("cardsStudiedToday", cardsStudiedToday)
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to update studied count in Firestore", e));
        }
    }

    public void clearData() {
        studySets.clear();
        streakDays = 0;
        cardsStudiedToday = 0;
        dailyGoal = 50;
        focusMinutes = 0;
        isLoaded = false;
    }
}

