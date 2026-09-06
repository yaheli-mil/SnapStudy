package com.yalig.snapstudy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
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

public class FlashcardsActivity extends AppCompatActivity {

    public static final String EXTRA_SET_ID = "extra_set_id";

    private StudySet studySet;
    private List<Flashcard> cards;
    private int currentIndex = 0;
    private boolean isAnswerRevealed = false;

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

        String setId = getIntent().getStringExtra(EXTRA_SET_ID);
        if (setId == null) {
            setId = "bio_101";
        }

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

        if (cards == null || cards.isEmpty()) {
            Toast.makeText(this, "אין עדיין כרטיסיות לתרגול. צור או סרוק ערכה ראשונה!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        displayCurrentCard();
    }

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

        ViewAnimationUtils.applyTouchScaleEffect(btnBack);
        ViewAnimationUtils.applyTouchScaleEffect(btnClose);
        ViewAnimationUtils.applyTouchScaleEffect(cardView);
        ViewAnimationUtils.applyTouchScaleEffect(btnReveal);
        ViewAnimationUtils.applyTouchScaleEffect(btnAgain);
        ViewAnimationUtils.applyTouchScaleEffect(btnHard);
        ViewAnimationUtils.applyTouchScaleEffect(btnGood);
        ViewAnimationUtils.applyTouchScaleEffect(btnEasy);

        tvDeckTitle.setText(studySet.getTitle());

        cardView.setOnClickListener(v -> revealAnswer());
        btnReveal.setOnClickListener(v -> revealAnswer());

        btnAgain.setOnClickListener(v -> onRatingSelected(4, "נחזור על הכרטיסייה בקרוב"));
        btnHard.setOnClickListener(v -> onRatingSelected(3, "סומן כקשה"));
        btnGood.setOnClickListener(v -> onRatingSelected(2, "סומן כטוב"));
        btnEasy.setOnClickListener(v -> onRatingSelected(1, "מצוין! סומן כקל"));
    }

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

        tvAnswer.setVisibility(View.GONE);
        tvHint.setVisibility(View.VISIBLE);
        btnReveal.setVisibility(View.VISIBLE);
        layoutRatings.setVisibility(View.GONE);

        int displayNum = currentIndex + 1;
        int total = cards.size();
        tvCardProgress.setText(String.format("כרטיסייה %d מתוך %d", displayNum, total));

        int progressPercent = (int) (((float) displayNum / total) * 100);
        progressBar.setProgress(progressPercent);

        // Smooth slide & fade-in of card view
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

    private void revealAnswer() {
        if (isAnswerRevealed) return;
        isAnswerRevealed = true;

        // Smooth flip / transition animation
        cardView.animate()
                .rotationY(90f)
                .setDuration(150)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        tvAnswer.setVisibility(View.VISIBLE);
                        tvHint.setVisibility(View.GONE);
                        btnReveal.setVisibility(View.GONE);
                        layoutRatings.setVisibility(View.VISIBLE);

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

    private void onRatingSelected(int difficulty, String toastMessage) {
        StudyRepository.getInstance().recordCardStudied();
        Flashcard card = cards.get(currentIndex);
        card.setDifficulty(difficulty);
        card.incrementRepetition();

        if (currentIndex < cards.size() - 1) {
            currentIndex++;
            displayCurrentCard();
        } else {
            showDeckCompletedDialog();
        }
    }

    private void showDeckCompletedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🎉 כל הכבוד!")
                .setMessage("השלמת את כל " + cards.size() + " הכרטיסיות בערכת " + studySet.getTitle() + "!\nהרצף והיעד היומי שלך עודכנו.")
                .setPositiveButton("חזרה ללוח הבקרה", (dialog, which) -> finish())
                .setNegativeButton("תרגל שוב", (dialog, which) -> {
                    currentIndex = 0;
                    displayCurrentCard();
                })
                .setCancelable(false)
                .show();
    }
}
