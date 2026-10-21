package com.yalig.snapstudy.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.yalig.snapstudy.models.StudySet;

import java.util.ArrayList;

/**
 * מחלקת עזר לעבודה מול Cloud Firestore.
 * כל ערכות הלימוד של המשתמש נשמרות בנתיב: users/{uid}/study_sets/{setId}
 * (הכרטיסיות נשמרות בתוך המסמך של הערכה).
 */
public class StudyRepository {

    // ממשק שבעזרתו מחזירים למסך את הערכות אחרי שהן נטענו מהענן
    public interface SetsCallback {
        void onSetsLoaded(ArrayList<StudySet> sets);
    }

    // מחזירה את האוסף של הערכות של המשתמש המחובר
    private static CollectionReference getSetsCollection() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("study_sets");
    }

    // טעינת כל הערכות של המשתמש מ-Firestore
    public static void loadSets(SetsCallback callback) {
        getSetsCollection().get().addOnCompleteListener(task -> {
            ArrayList<StudySet> sets = new ArrayList<>();
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    StudySet set = doc.toObject(StudySet.class);
                    set.setId(doc.getId());
                    sets.add(set);
                }
            }
            callback.onSetsLoaded(sets);
        });
    }

    // שמירת ערכה (חדשה או מעודכנת) ב-Firestore
    public static void saveSet(StudySet set) {
        getSetsCollection().document(set.getId()).set(set);
    }
}
