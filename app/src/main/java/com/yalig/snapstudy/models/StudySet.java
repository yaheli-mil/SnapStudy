package com.yalig.snapstudy.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * מודל נתונים המייצג ערכת לימוד שלמה (Study Set / Deck) במערכת SnapStudy.
 * כל ערכה כוללת כותרת, מקצוע לימוד (קטגוריה), אחוז שליטה כללי,
 * ורשימה של כרטיסיות זיכרון (Flashcards) השייכות לאותה ערכה (יחס הכלה / Composition).
 * המחלקה מממשת את ממשק Serializable כדי לאפשר העברת הערכה בין מסכים.
 */
public class StudySet implements Serializable {

    // מזהה ייחודי של ערכת הלימוד (משמש כמזהה המסמך ב-Cloud Firestore)
    private String id;

    // כותרת ערכת הלימוד (לדוגמה: "מבוא למאקרו-כלכלה", "ביולוגיה של התא")
    private String title;

    // קטגוריית או מקצוע הלימוד (לדוגמה: "היסטוריה", "ביולוגיה", "מדעי המחשב")
    private String category;

    // מידע טקסטואלי אודות עדכון אחרון או תיאור קצר לתצוגה בכרטיס
    private String updatedInfo;

    // אחוז השליטה הממוצע של המשתמש בחומר הערכה (0 עד 100)
    private int masteryPercentage;

    // רשימת כרטיסיות הזיכרון המרכיבות את הערכה
    private List<Flashcard> flashcards;

    /**
     * בנאי ריק (ברירת מחדל) - חובה לצורך המרה אוטומטית ממסמכי Firestore לאובייקט.
     * מאתחל רשימת כרטיסיות ריקה למניעת חריגות NullPointerException.
     */
    public StudySet() {
        this.flashcards = new ArrayList<>();
    }

    /**
     * בנאי ליצירת ערכת לימוד חדשה עם פרטים ראשוניים.
     * @param id מזהה ייחודי של הערכה.
     * @param title שם הערכה.
     * @param category תחום הדעת או המקצוע.
     * @param updatedInfo תיאור תמציתי או מועד עדכון.
     * @param masteryPercentage אחוז שליטה התחלתי.
     */
    public StudySet(String id, String title, String category, String updatedInfo, int masteryPercentage) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.updatedInfo = updatedInfo;
        this.masteryPercentage = masteryPercentage;
        this.flashcards = new ArrayList<>();
    }

    // --- פונקציות גישה ועדכון (Getters & Setters) ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUpdatedInfo() { return updatedInfo; }
    public void setUpdatedInfo(String updatedInfo) { this.updatedInfo = updatedInfo; }

    public int getMasteryPercentage() { return masteryPercentage; }
    public void setMasteryPercentage(int masteryPercentage) { this.masteryPercentage = masteryPercentage; }

    public List<Flashcard> getFlashcards() { return flashcards; }
    public void setFlashcards(List<Flashcard> flashcards) { this.flashcards = flashcards; }

    /**
     * מחזירה את מספר הכרטיסיות הקיימות בערכה זו.
     * הערה: תגית @Exclude מודיעה ל-Firebase Firestore להתעלם מפונקציה זו
     * ולא לנסות לשמור שדה מחושב זה כערך במסמך בבסיס הנתונים.
     * @return כמות הכרטיסיות בערכה.
     */
    @com.google.firebase.firestore.Exclude
    public int getCardCount() { 
        return flashcards != null ? flashcards.size() : 0; 
    }

    /**
     * מוסיפה כרטיסיית זיכרון חדשה לרשימת הכרטיסיות בערכה.
     * מוודאת שהרשימה אינה Null לפני ההוספה.
     * @param card הכרטיסייה להוספה.
     */
    public void addFlashcard(Flashcard card) {
        if (this.flashcards == null) {
            this.flashcards = new ArrayList<>();
        }
        this.flashcards.add(card);
    }

    /**
     * מחשבת מחדש את אחוז השליטה בערכה:
     * אחוז הכרטיסיות שכבר תורגלו וסומנו בפעם האחרונה כ"טוב" או "קל".
     */
    public void updateMasteryPercentage() {
        if (flashcards == null || flashcards.isEmpty()) {
            masteryPercentage = 0;
            return;
        }
        int mastered = 0;
        for (Flashcard card : flashcards) {
            if (card.getRepetitionCount() > 0 && card.getDifficulty() <= 2) {
                mastered++;
            }
        }
        masteryPercentage = mastered * 100 / flashcards.size();
    }
}

