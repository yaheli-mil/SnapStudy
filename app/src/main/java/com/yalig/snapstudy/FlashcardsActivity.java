package com.yalig.snapstudy;

import android.app.AlertDialog;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.yalig.snapstudy.data.StatsManager;
import com.yalig.snapstudy.data.StudyRepository;
import com.yalig.snapstudy.models.Flashcard;
import com.yalig.snapstudy.models.StudySet;

/**
 * מסך התרגול: מציג שאלה, חושף את התשובה, והמשתמש מדרג כמה קל היה לו.
 * הערכה מגיעה מהמסך הקודם דרך ה-Intent (StudySet הוא Serializable).
 */
public class FlashcardsActivity extends AppCompatActivity {

    private StudySet studySet;
    private int currentIndex = 0;       // הכרטיסייה שמוצגת עכשיו
    private int correctCount = 0;       // כמה כרטיסיות סומנו "טוב" או "קל"
    private long startTime;             // מתי התחיל התרגול (לחישוב דקות הריכוז)
    private ToneGenerator tone;         // משמיע צליל קצר כשהתשובה נכונה

    private TextView tvCardProgress, tvTopicTag, tvQuestion, tvAnswer, tvHint;
    private ProgressBar progressBar;
    private View cardView, btnReveal, layoutRatings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcards);

        studySet = getIntent().getSerializableExtra("studySet", StudySet.class);
        if (studySet == null || studySet.getFlashcards().isEmpty()) {
            Toast.makeText(this, "אין כרטיסיות בערכה הזו", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView tvDeckTitle = findViewById(R.id.tvDeckTitle);
        tvDeckTitle.setText(studySet.getTitle());
        tvCardProgress = findViewById(R.id.tvCardProgress);
        progressBar = findViewById(R.id.progressBar);
        cardView = findViewById(R.id.cardFlashcard);
        tvTopicTag = findViewById(R.id.tvTopicTag);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvAnswer = findViewById(R.id.tvAnswer);
        tvHint = findViewById(R.id.tvHint);
        btnReveal = findViewById(R.id.btnReveal);
        layoutRatings = findViewById(R.id.layoutRatings);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
        cardView.setOnClickListener(v -> revealAnswer());
        btnReveal.setOnClickListener(v -> revealAnswer());

        // דרגות הקושי: 1 = קל, 2 = טוב, 3 = קשה, 4 = שוב
        findViewById(R.id.btnEasy).setOnClickListener(v -> rateCard(1));
        findViewById(R.id.btnGood).setOnClickListener(v -> rateCard(2));
        findViewById(R.id.btnHard).setOnClickListener(v -> rateCard(3));
        findViewById(R.id.btnAgain).setOnClickListener(v -> rateCard(4));

        tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 80);
        showCard();
    }

    // מודדים כמה זמן המשתמש נמצא במסך התרגול
    @Override
    protected void onResume() {
        super.onResume();
        startTime = System.currentTimeMillis();
    }

    @Override
    protected void onPause() {
        super.onPause();
        int seconds = (int) ((System.currentTimeMillis() - startTime) / 1000);
        StatsManager.addFocusSeconds(this, seconds);
    }

    private void showCard() {
        Flashcard card = studySet.getFlashcards().get(currentIndex);
        int total = studySet.getFlashcards().size();

        tvTopicTag.setText(card.getTopic());
        tvQuestion.setText(card.getQuestion());
        tvAnswer.setText(card.getAnswer());
        tvCardProgress.setText("כרטיסייה " + (currentIndex + 1) + " מתוך " + total);
        progressBar.setProgress((currentIndex + 1) * 100 / total);

        // בהתחלה רואים רק את השאלה
        tvAnswer.setVisibility(View.GONE);
        tvHint.setVisibility(View.VISIBLE);
        btnReveal.setVisibility(View.VISIBLE);
        layoutRatings.setVisibility(View.GONE);
    }

    private void revealAnswer() {
        if (tvAnswer.getVisibility() == View.VISIBLE) {
            return;
        }
        // אנימציה פשוטה: הכרטיס מסתובב סיבוב שלם סביב ציר Y
        cardView.animate().rotationYBy(360).setDuration(400).start();
        tvAnswer.setVisibility(View.VISIBLE);
        tvHint.setVisibility(View.GONE);
        btnReveal.setVisibility(View.GONE);
        layoutRatings.setVisibility(View.VISIBLE);
    }

    private void rateCard(int difficulty) {
        Flashcard card = studySet.getFlashcards().get(currentIndex);
        card.applyRating(difficulty);
        StatsManager.recordCardStudied(this);

        if (difficulty <= 2) {   // "טוב" או "קל" נחשב כתשובה נכונה
            correctCount++;
            playSuccessSound();
        }

        // שמירת ההתקדמות בענן אחרי כל כרטיסייה
        studySet.updateMasteryPercentage();
        StudyRepository.saveSet(studySet);

        if (currentIndex < studySet.getFlashcards().size() - 1) {
            currentIndex++;
            showCard();
        } else {
            showFinishDialog();
        }
    }

    private void playSuccessSound() {
        if (SnapStudyApp.isSoundEnabled(this)) {
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 150);
        }
    }

    // משחררים את מחולל הצליל כשהמסך נסגר
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tone != null) {
            tone.release();
        }
    }

    private void showFinishDialog() {
        int total = studySet.getFlashcards().size();
        int score = correctCount * 100 / total;

        new AlertDialog.Builder(this)
                .setTitle("כל הכבוד! 🎉")
                .setMessage("סיימת את הערכה \"" + studySet.getTitle() + "\".\n"
                        + "ידעת " + correctCount + " מתוך " + total + " (" + score + "%).")
                .setPositiveButton("חזרה ללוח הבקרה", (dialog, which) -> finish())
                .setNegativeButton("תרגל שוב", (dialog, which) -> {
                    currentIndex = 0;
                    correctCount = 0;
                    showCard();
                })
                .setCancelable(false)
                .show();
    }
}
