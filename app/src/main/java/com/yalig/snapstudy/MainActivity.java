package com.yalig.snapstudy;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.yalig.snapstudy.data.StudyRepository;
import com.yalig.snapstudy.models.Flashcard;
import com.yalig.snapstudy.models.StudySet;
import com.yalig.snapstudy.utils.ViewAnimationUtils;

import java.util.List;

/**
 * המסך הראשי של האפליקציה (Dashboard / MainActivity).
 * מסך זה מהווה את מרכז הבקרה של הלומד ומספק:
 * 1. תצוגת מדדי למידה אישיים: כרטיסיות שנלמדו היום מול היעד היומי, דקות ריכוז, ותגית רצף ימים (Streak 🔥).
 * 2. הצגת כרטיסי ערכות הלימוד הקיימות בצורה דינמית עם מד התקדמות (Mastery) ותגיות מקצוע צבעוניות.
 * 3. חיפוש וסינון ערכות לימוד בזמן אמת (Live Search).
 * 4. גישה מהירה לסורק החכם (ScannerActivity), יצירת ערכה ידנית, ומעבר למסך ההגדרות.
 */
public class MainActivity extends AppCompatActivity {

    // מכולה דינמית (LinearLayout) שבה מוזרקים כרטיסי ערכות הלימוד
    private LinearLayout llStudySetsContainer;

    // שדה חיפוש חי לסינון ערכות לפי שם או מקצוע
    private EditText etSearch;

    // רכיבי תצוגת מדדים בלוח הבקרה
    private TextView tvTargetMetric;
    private TextView tvFocusMetric;
    private TextView streakBadge;

    // מצבי תצוגה: מסך ריק למשתמש חדש לעומת רשימת ערכות למשתמש פעיל
    private View layoutNewUser;
    private View layoutExistingUser;

    // מחרוזת החיפוש הפעילה כעת
    private String currentSearch = "";

    // משתנה מעקב אחר מצב ערכת הנושא לצורך זיהוי שינויים בעת חזרה ממסך ההגדרות
    private int lastKnownThemeMode = -999;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        lastKnownThemeMode = SnapStudyApp.getSavedThemeMode(this);
        SnapStudyApp.applyThemeToActivity(this);
        super.onCreate(savedInstanceState);

        // בדיקה האם יש משתמש מחובר במערכת - אם לא, הפניה מיידית למסך ההתחברות
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // התאמת שולי המסך לפסי המערכת של Android
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            View bottomNav = findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                bottomNav.setPadding(0, 0, 0, systemBars.bottom);
            }
            return insets;
        });

        initViews();
        setupListeners();
        updateUserProfile(currentUser);

        // טעינה מקומית מיידית מה-Cache המקומי לתצוגה מהירה ללא השהייה
        if (currentUser != null) {
            StudyRepository.getInstance().loadLocalData(currentUser.getUid());
            refreshUI();
        }
    }

    /**
     * מעדכנת את פרטי המשתמש המוצגים בראש המסך: שם וברכה אישית, ותמונת פרופיל.
     * @param user המשתמש המחובר מ-Firebase Auth.
     */
    private void updateUserProfile(FirebaseUser user) {
        if (user == null) return;

        TextView tvGreeting = findViewById(R.id.tvGreeting);
        if (tvGreeting != null) {
            String name = user.getDisplayName();
            if (name == null || name.trim().isEmpty()) {
                if (user.getEmail() != null && user.getEmail().contains("@")) {
                    name = user.getEmail().substring(0, user.getEmail().indexOf("@"));
                } else {
                    name = "תלמיד";
                }
            }
            tvGreeting.setText("שלום, " + name + " 👋");
        }

        // טעינת תמונת הפרופיל באמצעות ספריית Glide החיצונית
        ShapeableImageView ivUserAvatar = findViewById(R.id.ivUserAvatar);
        if (ivUserAvatar != null && user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .placeholder(R.drawable.avatar_user)
                    .error(R.drawable.avatar_user)
                    .circleCrop()
                    .into(ivUserAvatar);
        }
    }

    /**
     * במחזור החיים onResume - המסך חוזר לקדמת הבמה.
     * בודקת האם הנושא (Theme) השתנה ומסנכרנת נתונים מול הענן.
     */
    @Override
    protected void onResume() {
        super.onResume();
        int currentTheme = SnapStudyApp.getSavedThemeMode(this);
        if (lastKnownThemeMode != -999 && lastKnownThemeMode != currentTheme) {
            lastKnownThemeMode = currentTheme;
            getDelegate().setLocalNightMode(currentTheme);
            recreate();
            return;
        }
        lastKnownThemeMode = currentTheme;
        updateUserProfile(FirebaseAuth.getInstance().getCurrentUser());
        refreshUI();

        // סנכרון רקע מול Cloud Firestore
        StudyRepository.getInstance().syncWithFirestore(() -> runOnUiThread(this::refreshUI));
    }

    /**
     * אתחול רכיבי הממשק והפעלת אנימציות כניסה מדורגות (Staggered Entrance).
     */
    private void initViews() {
        llStudySetsContainer = findViewById(R.id.llStudySetsContainer);
        etSearch = findViewById(R.id.etSearch);
        tvTargetMetric = findViewById(R.id.tvTargetMetric);
        tvFocusMetric = findViewById(R.id.tvFocusMetric);
        streakBadge = findViewById(R.id.streakBadge);
        layoutNewUser = findViewById(R.id.layoutNewUser);
        layoutExistingUser = findViewById(R.id.layoutExistingUser);

        TextView tvTipContent = findViewById(R.id.tvTipContent);
        if (tvTipContent != null) {
            android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
            sb.append("טיפ מהיר: ");
            sb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, sb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append("האפליקציה תסכם עבורך דפי מחברת ותייצר כרטיסיות חזרה באופן אוטומטי!");
            tvTipContent.setText(sb);
        }

        // אנימציות כניסה מדורגות לרכיבי הממשק השונים
        ViewAnimationUtils.animateStaggeredEntrance(findViewById(R.id.cardTodayTarget), 80);
        ViewAnimationUtils.animateStaggeredEntrance(findViewById(R.id.cardFocusTime), 140);
        ViewAnimationUtils.animateStaggeredEntrance(findViewById(R.id.cardSearch), 200);
        if (layoutNewUser != null) {
            ViewAnimationUtils.animateStaggeredEntrance(layoutNewUser, 260);
        }
    }

    /**
     * הגדרת מאזיני לחיצה, חיפוש חי, ואפקטי מגע לכל כפתורי המסך.
     */
    private void setupListeners() {
        // החלת אפקט לחיצה קפיצי
        ViewAnimationUtils.applyTouchScaleEffect(findViewById(R.id.btnEmptyScan));
        ViewAnimationUtils.applyTouchScaleEffect(findViewById(R.id.btnEmptyNewSet));
        ViewAnimationUtils.applyTouchScaleEffect(findViewById(R.id.btnScanDoc));
        ViewAnimationUtils.applyTouchScaleEffect(findViewById(R.id.btnNewSet));
        ViewAnimationUtils.applyTouchScaleEffect(findViewById(R.id.ivUserAvatar));
        ViewAnimationUtils.applyTouchScaleEffect(findViewById(R.id.btnViewAll));
        ViewAnimationUtils.applyTouchScaleEffect(findViewById(R.id.tabHome));
        ViewAnimationUtils.applyTouchScaleEffect(findViewById(R.id.tabScanner));
        ViewAnimationUtils.applyTouchScaleEffect(findViewById(R.id.tabSettings));

        // פעולות במסך משתמש חדש (ללא ערכות)
        View btnEmptyScan = findViewById(R.id.btnEmptyScan);
        if (btnEmptyScan != null) {
            btnEmptyScan.setOnClickListener(v -> ViewAnimationUtils.startActivityWithTransition(this, new Intent(this, ScannerActivity.class)));
        }

        View btnEmptyNewSet = findViewById(R.id.btnEmptyNewSet);
        if (btnEmptyNewSet != null) {
            btnEmptyNewSet.setOnClickListener(v -> showCreateSetDialog());
        }

        // סינון וחיפוש חי בעת הקלדה בתיבת החיפוש
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString().trim();
                populateStudySets(currentSearch);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // כפתור סריקת מסמך מהיר
        findViewById(R.id.btnScanDoc).setOnClickListener(v -> {
            ViewAnimationUtils.startActivityWithTransition(this, new Intent(this, ScannerActivity.class));
        });

        // כפתור יצירת ערכה חדשה ידנית
        findViewById(R.id.btnNewSet).setOnClickListener(v -> showCreateSetDialog());

        // כפתור "צפה בהכל" - מאפס את החיפוש
        findViewById(R.id.btnViewAll).setOnClickListener(v -> {
            etSearch.setText("");
        });

        // לחיצה על תמונת המשתמש פותחת את מסך ההגדרות
        findViewById(R.id.ivUserAvatar).setOnClickListener(v -> {
            ViewAnimationUtils.startActivityWithTransition(this, new Intent(this, SettingsActivity.class));
        });

        // לשוניות הניווט בסרגל התחתון
        View tabHome = findViewById(R.id.tabHome);
        if (tabHome != null) {
            tabHome.setOnClickListener(v -> ViewAnimationUtils.animateTabSelection(tabHome, null));
        }

        findViewById(R.id.tabScanner).setOnClickListener(v -> {
            ViewAnimationUtils.startActivityWithTransition(this, new Intent(this, ScannerActivity.class));
        });

        findViewById(R.id.tabSettings).setOnClickListener(v -> {
            ViewAnimationUtils.startActivityWithTransition(this, new Intent(this, SettingsActivity.class));
        });
    }

    /**
     * מרעננת את כל ממשק המשתמש במסך בהתאם לנתונים המעודכנים ב-Repository.
     */
    private void refreshUI() {
        refreshMetrics();
        List<StudySet> allSets = StudyRepository.getInstance().getAllStudySets();
        if (allSets.isEmpty()) {
            if (layoutNewUser != null) layoutNewUser.setVisibility(View.VISIBLE);
            if (layoutExistingUser != null) layoutExistingUser.setVisibility(View.GONE);
            if (streakBadge != null) streakBadge.setVisibility(View.GONE);
        } else {
            if (layoutNewUser != null) layoutNewUser.setVisibility(View.GONE);
            if (layoutExistingUser != null) layoutExistingUser.setVisibility(View.VISIBLE);
            populateStudySets(currentSearch);
        }
    }

    /**
     * מעדכנת את תצוגת המדדים: מספר כרטיסיות היום מול יעד יומי, דקות ריכוז, ותגית ה-Streak.
     */
    private void refreshMetrics() {
        StudyRepository repo = StudyRepository.getInstance();
        if (tvTargetMetric != null) {
            tvTargetMetric.setText(String.valueOf(repo.getCardsStudiedToday()));
        }
        TextView tvTargetGoal = findViewById(R.id.tvTargetGoal);
        if (tvTargetGoal != null) {
            tvTargetGoal.setText(String.format("/ %d", repo.getDailyGoal()));
        }
        if (tvFocusMetric != null) {
            tvFocusMetric.setText(String.valueOf(repo.getFocusMinutes()));
        }

        // הצגת תגית רצף (Streak) אם המשתמש למד לפחות יומיים ברצף
        int streak = repo.getStreakDays();
        if (streakBadge != null) {
            if (streak >= 2 && !repo.getAllStudySets().isEmpty()) {
                streakBadge.setVisibility(View.VISIBLE);
                streakBadge.setText(String.format("%d ימים ברצף 🔥", streak));
            } else {
                streakBadge.setVisibility(View.GONE);
            }
        }
    }

    /**
     * מאכלסת את רשימת ערכות הלימוד במסך באופן דינמי (Dynamic Inflation):
     * עוברת על הערכות במאגר, מסננת לפי מילת החיפוש, ומייצרת View עבור כל ערכה.
     * @param query מחרוזת החיפוש לסינון.
     */
    private void populateStudySets(String query) {
        llStudySetsContainer.removeAllViews();
        List<StudySet> allSets = StudyRepository.getInstance().getAllStudySets();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (StudySet set : allSets) {
            // בדיקת התאמה לכותרת או לקטגוריה
            if (!query.isEmpty()) {
                boolean matchesTitle = set.getTitle().toLowerCase().contains(query.toLowerCase());
                boolean matchesCat = set.getCategory().toLowerCase().contains(query.toLowerCase());
                if (!matchesTitle && !matchesCat) {
                    continue;
                }
            }

            // ניפוח ה-Layout של כרטיס ערכת לימוד בודדת
            View cardView = inflater.inflate(R.layout.item_study_set, llStudySetsContainer, false);

            TextView tvCategoryBadge = cardView.findViewById(R.id.tvCategoryBadge);
            TextView tvMastery = cardView.findViewById(R.id.tvMastery);
            TextView tvSetTitle = cardView.findViewById(R.id.tvSetTitle);
            TextView tvSetSubtitle = cardView.findViewById(R.id.tvSetSubtitle);
            ProgressBar pbMastery = cardView.findViewById(R.id.pbMastery);

            tvSetTitle.setText(set.getTitle());
            tvCategoryBadge.setText(set.getCategory());
            tvMastery.setText(String.format("%d%% שליטה", set.getMasteryPercentage()));
            tvSetSubtitle.setText(String.format("%d כרטיסיות • %s", set.getCardCount(), set.getUpdatedInfo()));
            pbMastery.setProgress(set.getMasteryPercentage());

            // עיצוב תגית הקטגוריה ופס ההתקדמות לפי מקצוע הלימוד
            if ("כלכלה".equals(set.getCategory())) {
                tvCategoryBadge.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.tag_economics));
                tvCategoryBadge.setTextColor(ContextCompat.getColor(this, R.color.tag_economics_text));
                pbMastery.setProgressTintList(ContextCompat.getColorStateList(this, R.color.secondary));
                tvMastery.setTextColor(ContextCompat.getColor(this, R.color.secondary));
            } else if ("מתמטיקה".equals(set.getCategory())) {
                tvCategoryBadge.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.tag_algebra));
                tvCategoryBadge.setTextColor(ContextCompat.getColor(this, R.color.tag_algebra_text));
                pbMastery.setProgressTintList(ContextCompat.getColorStateList(this, R.color.primary));
                tvMastery.setTextColor(ContextCompat.getColor(this, R.color.primary));
            } else {
                tvCategoryBadge.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.tag_biology));
                tvCategoryBadge.setTextColor(ContextCompat.getColor(this, R.color.tag_biology_text));
                pbMastery.setProgressTintList(ContextCompat.getColorStateList(this, R.color.primary));
                tvMastery.setTextColor(ContextCompat.getColor(this, R.color.primary));
            }

            // החלת אפקט לחיצה ומעבר למסך תרגול הכרטיסיות (FlashcardsActivity) עבור ערכה זו
            ViewAnimationUtils.applyTouchScaleEffect(cardView);
            cardView.setOnClickListener(v -> {
                Intent intent = new Intent(this, FlashcardsActivity.class);
                intent.putExtra(FlashcardsActivity.EXTRA_SET_ID, set.getId());
                ViewAnimationUtils.startActivityWithTransition(this, intent);
            });

            llStudySetsContainer.addView(cardView);
        }

        // הצגת הודעה כאשר אין תוצאות מתאימות לחיפוש
        if (llStudySetsContainer.getChildCount() == 0) {
            TextView noResults = new TextView(this);
            noResults.setText("לא נמצאו ערכות התואמות לחיפוש.");
            noResults.setTextColor(ContextCompat.getColor(this, R.color.outline));
            noResults.setTextSize(13);
            noResults.setPadding(16, 32, 16, 32);
            llStudySetsContainer.addView(noResults);
        }
    }

    /**
     * פותחת דיאלוג מותאם ליצירת ערכת לימוד חדשה באופן ידני,
     * הכולל שם ערכה, נושא, ושאלה ותשובה לכרטיסייה ראשונה.
     */
    private void showCreateSetDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);
        layout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        final EditText etTitle = new EditText(this);
        etTitle.setHint("שם ערכת הלימוד (לדוגמה: היסטוריה - מלחמת העולם)");
        layout.addView(etTitle);

        final EditText etCategory = new EditText(this);
        etCategory.setHint("נושא / קורס (לדוגמה: היסטוריה)");
        layout.addView(etCategory);

        final EditText etQuestion = new EditText(this);
        etQuestion.setHint("שאלה ראשונה לכרטיסייה");
        layout.addView(etQuestion);

        final EditText etAnswer = new EditText(this);
        etAnswer.setHint("תשובה לכרטיסייה");
        layout.addView(etAnswer);

        new AlertDialog.Builder(this)
                .setTitle("✨ יצירת ערכת לימוד חדשה")
                .setView(layout)
                .setPositiveButton("צור ערכה", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String cat = etCategory.getText().toString().trim();
                    String q = etQuestion.getText().toString().trim();
                    String a = etAnswer.getText().toString().trim();

                    if (title.isEmpty()) {
                        title = "ערכת לימוד ללא שם";
                    }
                    if (cat.isEmpty()) {
                        cat = "כללי";
                    }

                    String newId = "manual_" + System.currentTimeMillis();
                    StudySet newSet = new StudySet(newId, title, cat, "נוצר זה עתה", 0);

                    if (!q.isEmpty() && !a.isEmpty()) {
                        newSet.addFlashcard(new Flashcard("m1", q, a, cat));
                    } else {
                        newSet.addFlashcard(new Flashcard("m1", "מושג ראשון ב" + title, "הסבר מפורט למושג הראשון.", cat));
                    }

                    StudyRepository.getInstance().addStudySet(newSet);
                    refreshUI();
                    Toast.makeText(this, "ערכת '" + title + "' נוצרה בהצלחה!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}