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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    public boolean isLoaded() {
        return isLoaded;
    }

    public void loadLocalData(String uid) {
        Context context = SnapStudyApp.getInstance();
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences("study_repo_" + (uid != null ? uid : "default"), Context.MODE_PRIVATE);
        String json = prefs.getString("study_sets_json", null);

        if (json == null || json.trim().isEmpty()) {
            // Check device backup
            SharedPreferences devicePrefs = context.getSharedPreferences("study_repo_device_backup", Context.MODE_PRIVATE);
            json = devicePrefs.getString("last_known_sets", null);
        }

        if (json != null && !json.trim().isEmpty()) {
            List<StudySet> loaded = jsonToSets(json);
            if (!loaded.isEmpty()) {
                studySets.clear();
                studySets.addAll(loaded);
                isLoaded = true;
                Log.d(TAG, "Loaded " + loaded.size() + " sets from local cache for uid: " + uid);
            }
        }

        streakDays = prefs.getInt("streak_days", streakDays);
        cardsStudiedToday = prefs.getInt("cards_studied", cardsStudiedToday);
        dailyGoal = prefs.getInt("daily_goal", dailyGoal);
        focusMinutes = prefs.getInt("focus_minutes", focusMinutes);
    }

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
                .apply();

        if (!studySets.isEmpty()) {
            SharedPreferences devicePrefs = context.getSharedPreferences("study_repo_device_backup", Context.MODE_PRIVATE);
            devicePrefs.edit()
                    .putString("last_known_sets", json)
                    .apply();
        }
    }

    public void syncWithFirestore(DataCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onComplete();
            return;
        }

        String uid = user.getUid();

        // Always load local cache first so UI is instant and never wiped
        if (studySets.isEmpty()) {
            loadLocalData(uid);
        }

        // 1. Sync User Stats
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long streak = documentSnapshot.getLong("streakDays");
                        Long studied = documentSnapshot.getLong("cardsStudiedToday");
                        Long goal = documentSnapshot.getLong("dailyGoal");
                        Long focus = documentSnapshot.getLong("focusMinutes");

                        if (streak != null) streakDays = streak.intValue();
                        if (studied != null) cardsStudiedToday = studied.intValue();
                        if (goal != null) dailyGoal = goal.intValue();
                        if (focus != null) focusMinutes = focus.intValue();
                        saveLocalData(uid);
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
                            sanitizeStudySet(set);
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
                            sanitizeStudySet(localSet);
                            db.collection("users").document(uid)
                                    .collection("study_sets").document(localSet.getId()).set(localSet)
                                    .addOnFailureListener(e -> Log.w(TAG, "Failed syncing local set to Firestore", e));
                        }
                    }

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

    public List<StudySet> getAllStudySets() {
        return new ArrayList<>(studySets);
    }

    public StudySet getStudySetById(String id) {
        for (StudySet set : studySets) {
            if (set.getId() != null && set.getId().equals(id)) {
                return set;
            }
        }
        return studySets.isEmpty() ? null : studySets.get(0);
    }

    public void addStudySet(StudySet set) {
        if (set == null) return;
        sanitizeStudySet(set);
        if (set.getId() == null || set.getId().isEmpty()) {
            set.setId(UUID.randomUUID().toString());
        }

        for (int i = 0; i < studySets.size(); i++) {
            if (studySets.get(i).getId() != null && studySets.get(i).getId().equals(set.getId())) {
                studySets.remove(i);
                break;
            }
        }
        studySets.add(0, set);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user != null ? user.getUid() : "default";
        saveLocalData(uid);

        if (user != null) {
            db.collection("users").document(user.getUid())
                    .collection("study_sets").document(set.getId()).set(set)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Successfully persisted set " + set.getId()))
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to persist set to Firestore", e));
        }
    }

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

    public int getStreakDays() { return streakDays; }
    public int getCardsStudiedToday() { return cardsStudiedToday; }
    public int getDailyGoal() { return dailyGoal; }
    public int getFocusMinutes() { return focusMinutes; }

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

    public void recordCardStudied() {
        cardsStudiedToday++;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user != null ? user.getUid() : "default";
        saveLocalData(uid);

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

