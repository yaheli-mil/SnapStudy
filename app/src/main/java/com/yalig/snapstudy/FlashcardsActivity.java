package com.yalig.snapstudy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.view.animation.DecelerateInterpolator;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.yalig.snapstudy.data.StudyRepository;
import com.yalig.snapstudy.models.Flashcard;
import com.yalig.snapstudy.models.StudySet;
import com.yalig.snapstudy.utils.ViewAnimationUtils;

import java.util.List;

/**
 * מסך תרגול כרטיסיות זיכרון אינטראקטיבי (FlashcardsActivity).
 * מסך זה מממש את מתודולוגיית ה-"Active Recall" (שליפה פעילה מהזיכרון):
 * 1. הצגת השאלה או המושג ללומד מבלי לחשוף את התשובה.
 * 2. בלחיצה על הכרטיסייה, מופעלת אנימציית היפוך תלת-ממדית (3D Flip Animation סביב ציר ה-Y) החושפת את התשובה.
 * 3. חשיפת 4 כפתורי דירוג קושי (Again, Hard, Good, Easy) המעדכנים את מדדי הלמידה ואת מרווח החזרה של הכרטיסייה,
 *    ושמירת ההתקדמות (מקומית וב-Firestore) אחרי כל דירוג.
 * 4. מעקב התקדמות ויזואלי באמצעות פס התקדמות (ProgressBar) ומונה כרטיסיות.
 * 5. הצגת דיאלוג סיום מעודד בסיום הערכה עם ציון (כמה כרטיסיות סומנו "טוב" או "קל").
 */
public class FlashcardsActivity extends AppCompatActivity {

    // מפתח לקבלת מזהה ערכת הלימוד ב-Intent Extras
    public static final String EXTRA_SET_ID = "extra_set_id";

    // נתוני ערכת הלימוד הפעילה
    private StudySet studySet;
    private List<Flashcard> cards;
    private int currentIndex = 0;          // אינדקס הכרטיסייה הנוכחית שמוצגת
    private boolean isAnswerRevealed = false; // האם התשובה של הכרטיסייה הנוכחית כבר נחשפה
    private int correctCount = 0;          // מספר הכרטיסיות שסומנו "טוב" או "קל" בסבב הנוכחי

    // מחולל צליל קצר למשוב על תשובה נכונה
    private ToneGenerator toneGenerator;

    // רכיבי ממשק משתמש
    private TextView tvDeckTitle;
    private TextView tvCardProgress;
    private ProgressBar progressBar;
    private MaterialCardView cardView;
    private TextView tvTopicTag;
    private TextView tvQuestion;
    private TextView tvAnswer;
    private TextView tvHint;
    private MaterialButton btnReveal;
    private View layoutRatings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SnapStudyApp.applyThemeToActivity(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_flashcards);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.flashcardsRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // שליפת מזהה הערכה מה-Intent שהעביר המסך הקורא
        String setId = getIntent().getStringExtra(EXTRA_SET_ID);
        if (setId == null) {
            setId = "bio_101";
        }

        // איתור ערכת הלימוד מתוך מאגר הנתונים
        studySet = StudyRepository.getInstance().getStudySetById(setId);
        if (studySet == null) {
            List<StudySet> all = StudyRepository.getInstance().getAllStudySets();
            if (!all.isEmpty()) {
                studySet = all.get(0);
            }
        }

        if (studySet != null) {
            cards = studySet.getFlashcards();
        }

        // בדיקה האם יש כרטיסיות בערכה זו
        if (cards == null || cards.isEmpty()) {
            Toast.makeText(this, "אין עדיין כרטיסיות לתרגול. צור או סרוק ערכה ראשונה!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        displayCurrentCard();
    }

    /**
     * אתחול רכיבי הממשק, כפתורי חזרה, והגדרת מאזינים ללחיצות ואפקטי מגע.
     */
    private void initViews() {
        View btnBack = findViewById(R.id.btnBack);
        View btnClose = findViewById(R.id.btnClose);

        btnBack.setOnClickListener(v -> finish());
        btnClose.setOnClickListener(v -> finish());

        tvDeckTitle = findViewById(R.id.tvDeckTitle);
        tvCardProgress = findViewById(R.id.tvCardProgress);
        progressBar = findViewById(R.id.progressBar);
        cardView = findViewById(R.id.cardFlashcard);
        tvTopicTag = findViewById(R.id.tvTopicTag);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvAnswer = findViewById(R.id.tvAnswer);
        tvHint = findViewById(R.id.tvHint);
        btnReveal = findViewById(R.id.btnReveal);
        layoutRatings = findViewById(R.id.layoutRatings);

        View btnAgain = findViewById(R.id.btnAgain);
        View btnHard = findViewById(R.id.btnHard);
        View btnGood = findViewById(R.id.btnGood);
        View btnEasy = findViewById(R.id.btnEasy);

        // החלת אפקט לחיצה קפיצי על כל הכפתורים ועל הכרטיסייה עצמה
        ViewAnimationUtils.applyTouchScaleEffect(btnBack);
        ViewAnimationUtils.applyTouchScaleEffect(btnClose);
        ViewAnimationUtils.applyTouchScaleEffect(cardView);
        ViewAnimationUtils.applyTouchScaleEffect(btnReveal);
        ViewAnimationUtils.applyTouchScaleEffect(btnAgain);
        ViewAnimationUtils.applyTouchScaleEffect(btnHard);
        ViewAnimationUtils.applyTouchScaleEffect(btnGood);
        ViewAnimationUtils.applyTouchScaleEffect(btnEasy);

        tvDeckTitle.setText(studySet.getTitle());

        // לחיצה על גוף הכרטיסייה או על כפתור "חשוף תשובה" מפעילה את אנימציית ההיפוך
        cardView.setOnClickListener(v -> revealAnswer());
        btnReveal.setOnClickListener(v -> revealAnswer());

        // הגדרת מאזינים ל-4 כפתורי דירוג הקושי (1: קל, 2: טוב, 3: קשה, 4: שוב)
        btnAgain.setOnClickListener(v -> onRatingSelected(4, "נחזור על הכרטיסייה בקרוב"));
        btnHard.setOnClickListener(v -> onRatingSelected(3, "סומן כקשה"));
        btnGood.setOnClickListener(v -> onRatingSelected(2, "סומן כטוב"));
        btnEasy.setOnClickListener(v -> onRatingSelected(1, "מצוין! סומן כקל"));
    }

    /**
     * מציגה את נתוני הכרטיסייה הנוכחית על פי האינדקס currentIndex:
     * מאפסת את מצב חשיפת התשובה, מעדכנת את הטקסטים ומפעילה אנימציית כניסה.
     */
    private void displayCurrentCard() {
        if (cards == null || cards.isEmpty()) {
            Toast.makeText(this, "אין כרטיסיות בערכה זו", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        isAnswerRevealed = false;
        Flashcard currentCard = cards.get(currentIndex);

        tvTopicTag.setText(currentCard.getTopic());
        tvQuestion.setText(currentCard.getQuestion());
        tvAnswer.setText(currentCard.getAnswer());

        // החבאת התשובה והצגת רמז החשיפה
        tvAnswer.setVisibility(View.GONE);
        tvHint.setVisibility(View.VISIBLE);
        btnReveal.setVisibility(View.VISIBLE);
        layoutRatings.setVisibility(View.GONE);

        // עדכון טקסט ההתקדמות ("כרטיסייה X מתוך Y")
        int displayNum = currentIndex + 1;
        int total = cards.size();
        tvCardProgress.setText(String.format("כרטיסייה %d מתוך %d", displayNum, total));

        // עדכון אחוז ההתקדמות ב-ProgressBar
        int progressPercent = (int) (((float) displayNum / total) * 100);
        progressBar.setProgress(progressPercent);

        // אנימציית החלקה והופעה חלקה (Slide & Fade) של הכרטיסייה החדשה
        cardView.setAlpha(0f);
        cardView.setTranslationX(60f);
        cardView.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(240)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    @Override
    public void finish() {
        super.finish();
        ViewAnimationUtils.applyActivityCloseTransition(this);
    }

    @Override
    protected void onDestroy() {
        // שחרור מחולל הצליל כדי לא להשאיר משאבי שמע תפוסים
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
        super.onDestroy();
    }

    /**
     * הופכת את הכרטיסייה וחושפת את התשובה בליווי אנימציית היפוך תלת-ממדית (3D Flip Animation).
     * שלב 1: סיבוב הכרטיס ב-90 מעלות על ציר ה-Y (חצי היפוך).
     * שלב 2: החלפת התצוגה (הסתרת שאלה, הצגת תשובה וכפתורי דירוג).
     * שלב 3: המשך הסיבוב מ-90- מעלות חזרה ל-0 מעלות לסיום ההיפוך.
     */
    private void revealAnswer() {
        if (isAnswerRevealed) return;
        isAnswerRevealed = true;

        // חצי ראשון של ההיפוך
        cardView.animate()
                .rotationY(90f)
                .setDuration(150)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // החלפת תוכן הכרטיסייה בדיוק בנקודת האמצע
                        tvAnswer.setVisibility(View.VISIBLE);
                        tvHint.setVisibility(View.GONE);
                        btnReveal.setVisibility(View.GONE);
                        layoutRatings.setVisibility(View.VISIBLE);

                        // חצי שני של ההיפוך: התחלה מזווית נגדית וחזרה לאמצע
                        cardView.setRotationY(-90f);
                        cardView.animate()
                                .rotationY(0f)
                                .setDuration(150)
                                .setListener(null)
                                .start();
                    }
                })
                .start();
    }

    /**
     * מטפלת בבחירת דירוג קושי עבור הכרטיסייה:
     * מעדכנת את מונה הכרטיסיות שנלמדו ב-Repository, מעדכנת את הכרטיסייה (קושי, חזרות ומרווח חזרה),
     * מחשבת מחדש את אחוז השליטה ושומרת את הערכה דרך ה-Repository (מקומית וב-Firestore),
     * ומקדמת לכרטיסייה הבאה (או פותחת דיאלוג סיום אם תורגלו כל הכרטיסיות).
     * @param difficulty דרגת הקושי שנבחרה (1 עד 4).
     * @param toastMessage הודעת חיווי קצרה.
     */
    private void onRatingSelected(int difficulty, String toastMessage) {
        StudyRepository.getInstance().recordCardStudied();
        Flashcard card = cards.get(currentIndex);
        card.applyRating(difficulty);

        // תשובה נכונה ("טוב" או "קל"): ספירה לציון הסיום והשמעת צליל משוב
        if (difficulty <= 2) {
            correctCount++;
            playSuccessSound();
        }

        // עדכון אחוז השליטה ושמירת ההתקדמות כדי שלא תאבד ביציאה מהמסך
        studySet.updateMasteryPercentage();
        StudyRepository.getInstance().updateStudySet(studySet);

        if (currentIndex < cards.size() - 1) {
            currentIndex++;
            displayCurrentCard();
        } else {
            showDeckCompletedDialog();
        }
    }

    /**
     * משמיעה צליל קצר של תשובה נכונה, רק אם הצלילים מופעלים במסך ההגדרות.
     */
    private void playSuccessSound() {
        if (!SnapStudyApp.isSoundEnabled(this)) return;
        try {
            if (toneGenerator == null) {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 80);
            }
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 150);
        } catch (RuntimeException e) {
            // במכשירים מסוימים לא ניתן ליצור ToneGenerator - במקרה כזה פשוט מוותרים על הצליל
        }
    }

    /**
     * מציגה דיאלוג חגיגי בעת סיום תרגול כל הכרטיסיות בערכה, כולל ציון הסבב,
     * עם אפשרות לחזור למסך הראשי או לתרגל את הערכה שוב מההתחלה.
     */
    private void showDeckCompletedDialog() {
        int total = cards.size();
        int scorePercent = correctCount * 100 / total;
        new AlertDialog.Builder(this)
                .setTitle("🎉 כל הכבוד!")
                .setMessage("השלמת את כל " + total + " הכרטיסיות בערכת " + studySet.getTitle() + "!\n"
                        + "ידעת " + correctCount + " מתוך " + total + " (" + scorePercent + "%).\n"
                        + "היעד היומי שלך עודכן.")
                .setPositiveButton("חזרה ללוח הבקרה", (dialog, which) -> finish())
                .setNegativeButton("תרגל שוב", (dialog, which) -> {
                    currentIndex = 0;
                    correctCount = 0;
                    displayCurrentCard();
                })
                .setCancelable(false)
                .show();
    }
}

