package com.yalig.snapstudy;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.yalig.snapstudy.data.StatsManager;
import com.yalig.snapstudy.data.StudyRepository;
import com.yalig.snapstudy.models.Flashcard;
import com.yalig.snapstudy.models.StudySet;
import com.yalig.snapstudy.ui.StudySetAdapter;

import java.util.ArrayList;

/**
 * המסך הראשי: רשימת ערכות הלימוד, חיפוש, והנתונים של היום (יעד, דקות ריכוז, רצף).
 */
public class MainActivity extends AppCompatActivity {

    private ArrayList<StudySet> allSets = new ArrayList<>();   // כל הערכות שנטענו מהענן
    private StudySetAdapter adapter;

    private EditText etSearch;
    private TextView tvTargetMetric, tvTargetGoal, tvFocusMetric, streakBadge, tvNoResults;
    private View layoutNewUser, layoutExistingUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        etSearch = findViewById(R.id.etSearch);
        tvTargetMetric = findViewById(R.id.tvTargetMetric);
        tvTargetGoal = findViewById(R.id.tvTargetGoal);
        tvFocusMetric = findViewById(R.id.tvFocusMetric);
        streakBadge = findViewById(R.id.streakBadge);
        tvNoResults = findViewById(R.id.tvNoResults);
        layoutNewUser = findViewById(R.id.layoutNewUser);
        layoutExistingUser = findViewById(R.id.layoutExistingUser);

        // שם ותמונה של המשתמש
        TextView tvGreeting = findViewById(R.id.tvGreeting);
        String name = user.getDisplayName();
        if (name == null || name.isEmpty()) {
            name = "תלמיד";
        }
        tvGreeting.setText("שלום, " + name + " 👋");
        ImageView ivUserAvatar = findViewById(R.id.ivUserAvatar);
        if (user.getPhotoUrl() != null) {
            Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(ivUserAvatar);
        }

        // הרשימה: לחיצה על ערכה פותחת את מסך התרגול
        RecyclerView rvStudySets = findViewById(R.id.rvStudySets);
        rvStudySets.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudySetAdapter(new ArrayList<>(), set -> {
            Intent intent = new Intent(this, FlashcardsActivity.class);
            intent.putExtra("studySet", set);
            startActivity(intent);
        });
        rvStudySets.setAdapter(adapter);

        // חיפוש: בכל שינוי בטקסט מסננים את הרשימה מחדש
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                showSets();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // כפתורים
        findViewById(R.id.btnScanDoc).setOnClickListener(v -> openScanner());
        findViewById(R.id.btnEmptyScan).setOnClickListener(v -> openScanner());
        findViewById(R.id.tabScanner).setOnClickListener(v -> openScanner());
        findViewById(R.id.btnNewSet).setOnClickListener(v -> showCreateSetDialog());
        findViewById(R.id.btnEmptyNewSet).setOnClickListener(v -> showCreateSetDialog());
        findViewById(R.id.btnViewAll).setOnClickListener(v -> etSearch.setText(""));
        findViewById(R.id.ivUserAvatar).setOnClickListener(v -> openSettings());
        findViewById(R.id.tabSettings).setOnClickListener(v -> openSettings());
    }

    // onResume רץ גם כשחוזרים למסך (למשל אחרי תרגול), ולכן טוענים כאן את הנתונים מחדש
    @Override
    protected void onResume() {
        super.onResume();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }
        showStats();
        StudyRepository.loadSets(sets -> {
            allSets = sets;
            showSets();
        });
    }

    private void showStats() {
        tvTargetMetric.setText(String.valueOf(StatsManager.getCardsToday(this)));
        tvTargetGoal.setText("/ " + StatsManager.getDailyGoal(this));
        tvFocusMetric.setText(String.valueOf(StatsManager.getFocusMinutes(this)));

        int streak = StatsManager.getStreakDays(this);
        if (streak >= 2) {
            streakBadge.setText(streak + " ימים ברצף 🔥");
            streakBadge.setVisibility(View.VISIBLE);
        } else {
            streakBadge.setVisibility(View.GONE);
        }
    }

    // מציג רק את הערכות שמתאימות לטקסט החיפוש
    private void showSets() {
        if (allSets.isEmpty()) {
            layoutNewUser.setVisibility(View.VISIBLE);
            layoutExistingUser.setVisibility(View.GONE);
            return;
        }
        layoutNewUser.setVisibility(View.GONE);
        layoutExistingUser.setVisibility(View.VISIBLE);

        String query = etSearch.getText().toString().trim().toLowerCase();
        ArrayList<StudySet> filtered = new ArrayList<>();
        for (StudySet set : allSets) {
            if (set.getTitle().toLowerCase().contains(query)
                    || set.getCategory().toLowerCase().contains(query)) {
                filtered.add(set);
            }
        }
        adapter.setSets(filtered);
        tvNoResults.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // דיאלוג ליצירת ערכה ידנית עם כרטיסייה ראשונה
    private void showCreateSetDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        EditText etTitle = new EditText(this);
        etTitle.setHint("שם ערכת הלימוד");
        layout.addView(etTitle);
        EditText etCategory = new EditText(this);
        etCategory.setHint("נושא / קורס");
        layout.addView(etCategory);
        EditText etQuestion = new EditText(this);
        etQuestion.setHint("שאלה ראשונה לכרטיסייה");
        layout.addView(etQuestion);
        EditText etAnswer = new EditText(this);
        etAnswer.setHint("תשובה לכרטיסייה");
        layout.addView(etAnswer);

        new AlertDialog.Builder(this)
                .setTitle("יצירת ערכת לימוד חדשה")
                .setView(layout)
                .setPositiveButton("צור ערכה", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String category = etCategory.getText().toString().trim();
                    String question = etQuestion.getText().toString().trim();
                    String answer = etAnswer.getText().toString().trim();
                    if (title.isEmpty() || question.isEmpty() || answer.isEmpty()) {
                        Toast.makeText(this, "נא למלא שם, שאלה ותשובה", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (category.isEmpty()) {
                        category = "כללי";
                    }

                    String id = "manual_" + System.currentTimeMillis();
                    StudySet set = new StudySet(id, title, category, "נוצר ידנית", 0);
                    set.addFlashcard(new Flashcard(id + "_0", question, answer, category));
                    StudyRepository.saveSet(set);

                    allSets.add(0, set);
                    showSets();
                    Toast.makeText(this, "הערכה נוצרה!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void openScanner() {
        startActivity(new Intent(this, ScannerActivity.class));
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }
}
