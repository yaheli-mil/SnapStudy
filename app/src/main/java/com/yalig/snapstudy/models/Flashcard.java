package com.yalig.snapstudy.models;

import java.io.Serializable;

/**
 * מודל נתונים המייצג כרטיסיית לימוד בודדת (Flashcard) במערכת.
 * המחלקה מממשת את ממשק Serializable על מנת שניתן יהיה להעביר אובייקטים מסוג זה
 * בין מסכים שונים (Activities) באמצעות Intent Extras, וכן לשמור אותם ב-JSON.
 * בנוסף, המודל כולל שדות למימוש אלגוריתם חזרה מרווחת (Spaced Repetition).
 */
public class Flashcard implements Serializable {

    // מזהה ייחודי של הכרטיסייה במערכת
    private String id;

    // תוכן השאלה או המושג לבדיקה (הצד הקדמי של הכרטיסייה)
    private String question;

    // התשובה המלאה וההסבר (הצד האחורי של הכרטיסייה)
    private String answer;

    // נושא או תת-נושא אליו משויכת הכרטיסייה (למשל: תא בעלי חיים, מלחמת העולם)
    private String topic;

    // מרווח הימים המומלץ לחזרה הבאה לפי אלגוריתם הלמידה המרווחת
    private int intervalDays;

    // מספר הפעמים שהמשתמש תרגל כרטיסייה זו
    private int repetitionCount;

    // רמת הקושי שנקבעה על ידי המשתמש (1: קל, 2: טוב, 3: קשה, 4: שוב)
    private int difficulty;

    /**
     * בנאי ריק (ברירת מחדל) - נדרש על ידי ספריית Firebase Firestore
     * לצורך המרה אוטומטית (Deserialization) ממסמך בענן לאובייקט Java.
     */
    public Flashcard() {
        this.intervalDays = 1;
        this.repetitionCount = 0;
        this.difficulty = 2; // ברירת מחדל: רמת קושי "טוב"
    }

    /**
     * בנאי ליצירת כרטיסיית לימוד חדשה עם נתוני בסיס.
     * @param id מזהה ייחודי של הכרטיסייה.
     * @param question נוסח השאלה.
     * @param answer נוסח התשובה.
     * @param topic נושא הכרטיסייה.
     */
    public Flashcard(String id, String question, String answer, String topic) {
        this.id = id;
        this.question = question;
        this.answer = answer;
        this.topic = topic;
        this.intervalDays = 1;
        this.repetitionCount = 0;
        this.difficulty = 2;
    }

    // --- פונקציות גישה ועדכון (Getters & Setters) ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public int getIntervalDays() { return intervalDays; }
    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }

    public int getRepetitionCount() { return repetitionCount; }
    public void setRepetitionCount(int repetitionCount) { this.repetitionCount = repetitionCount; }

    /**
     * מעלה ב-1 את מונה הפעמים שהכרטיסייה תורגלה.
     */
    public void incrementRepetition() { this.repetitionCount++; }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

    /**
     * מעדכנת את הכרטיסייה לפי הדירוג שהמשתמש בחר בתרגול:
     * שומרת את דרגת הקושי, מעלה את מונה החזרות וקובעת את מרווח החזרה הבא
     * (לפי מה שכתוב על כפתורי הדירוג: שוב - עוד היום, קשה - יומיים, טוב - 4 ימים, קל - 7 ימים).
     * @param difficulty דרגת הקושי שנבחרה (1: קל, 2: טוב, 3: קשה, 4: שוב).
     */
    public void applyRating(int difficulty) {
        this.difficulty = difficulty;
        incrementRepetition();
        switch (difficulty) {
            case 1: this.intervalDays = 7; break;  // קל
            case 2: this.intervalDays = 4; break;  // טוב
            case 3: this.intervalDays = 2; break;  // קשה
            default: this.intervalDays = 0; break; // שוב - חזרה עוד היום
        }
    }
}

