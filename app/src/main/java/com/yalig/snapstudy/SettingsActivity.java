package com.yalig.snapstudy;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.yalig.snapstudy.data.StudyRepository;
import com.yalig.snapstudy.utils.ViewAnimationUtils;

import java.util.Locale;

/**
 * מסך הגדרות האפליקציה ופרופיל המשתמש (SettingsActivity).
 * במסך זה המשתמש יכול:
 * 1. לצפות בפרטי החשבון המחובר (שם, כתובת אימייל ותמונת פרופיל מ-Google/Firebase).
 * 2. לשנות את ערכת הנושא של האפליקציה (מצב בהיר, מצב כהה, או התאמה אוטומטית לפי מערכת ההפעלה).
 * 3. לקבוע את יעד הכרטיסיות היומי לתרגול.
 * 4. להפעיל או לכבות אפקטים קוליים בעת מענה על כרטיסיות.
 * 5. להגדיר תזכורת יומית ללימוד (כולל בחירת שעה מדויקת באמצעות TimePickerDialog).
 * 6. להתנתק מהחשבון (Sign Out) ולחזור למסך ההתחברות.
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // החלת ערכת הנושא הנבחרת לפני טעינת ה-Layout כדי למנוע הבהובים במעבר בין מסכים
        SnapStudyApp.applyThemeToActivity(this);
        super.onCreate(savedInstanceState);

        // תמיכה בפריסה מקצה לקצה (Edge-to-Edge) של מסך המכשיר
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        // כוונון ריווח הפריסה (Padding) לפי פסי המערכת של אנדרואיד (סטטוס בר ופס ניווט תחתון)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootSettings), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // -------------------------------------------------------------
        // טעינת פרטי המשתמש הנוכחי מ-Firebase Authentication
        // -------------------------------------------------------------
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        TextView tvProfileName = findViewById(R.id.tvProfileName);
        TextView tvProfileEmail = findViewById(R.id.tvProfileEmail);
        ShapeableImageView ivAvatar = findViewById(R.id.ivAvatar);

        if (user != null) {
            String name = user.getDisplayName();
            if (name != null && !name.trim().isEmpty()) {
                tvProfileName.setText(name);
            } else if (user.getEmail() != null && user.getEmail().contains("@")) {
                // אם המשתמש נרשם רק עם מייל ללא שם תצוגה, נציג את החלק שלפני ה-@ כשם ברירת מחדל
                tvProfileName.setText(user.getEmail().substring(0, user.getEmail().indexOf("@")));
            }

            if (user.getEmail() != null) {
                tvProfileEmail.setText(user.getEmail());
            }

            // טעינת תמונת הפרופיל של המשתמש בעזרת ספריית Glide (כולל חיתוך עגול ותמונת ברירת מחדל)
            if (user.getPhotoUrl() != null && ivAvatar != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .placeholder(R.drawable.avatar_user)
                        .error(R.drawable.avatar_user)
                        .circleCrop()
                        .into(ivAvatar);
            }
        }

        // -------------------------------------------------------------
        // איתור כפתורי הפעולה וחיבור אפקט קפיצי בלחיצה (Spring Scale)
        // -------------------------------------------------------------
        View btnBack = findViewById(R.id.btnBackSettings);
        View btnLight = findViewById(R.id.btnThemeLight);
        View btnDark = findViewById(R.id.btnThemeDark);
        View btnAuto = findViewById(R.id.btnThemeAuto);
        View btnSignOut = findViewById(R.id.btnSignOut);

        ViewAnimationUtils.applyTouchScaleEffect(btnBack);
        ViewAnimationUtils.applyTouchScaleEffect(btnLight);
        ViewAnimationUtils.applyTouchScaleEffect(btnDark);
        ViewAnimationUtils.applyTouchScaleEffect(btnAuto);
        ViewAnimationUtils.applyTouchScaleEffect(btnSignOut);

        // חזרה למסך הראשי
        btnBack.setOnClickListener(v -> finish());

        // -------------------------------------------------------------
        // ניהול ערכת נושא (Theme Mode: Light / Dark / Auto)
        // -------------------------------------------------------------
        int currentMode = SnapStudyApp.getSavedThemeMode(this);
        updateThemeButtonsUI(currentMode);

        // מעבר למצב בהיר
        btnLight.setOnClickListener(v -> {
            SnapStudyApp.saveThemeMode(this, AppCompatDelegate.MODE_NIGHT_NO);
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            updateThemeButtonsUI(AppCompatDelegate.MODE_NIGHT_NO);
        });

        // מעבר למצב כהה
        btnDark.setOnClickListener(v -> {
            SnapStudyApp.saveThemeMode(this, AppCompatDelegate.MODE_NIGHT_YES);
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            updateThemeButtonsUI(AppCompatDelegate.MODE_NIGHT_YES);
        });

        // מעבר למצב אוטומטי לפי מערכת ההפעלה
        btnAuto.setOnClickListener(v -> {
            SnapStudyApp.saveThemeMode(this, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            updateThemeButtonsUI(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        });

        // -------------------------------------------------------------
        // 1. הגדרת יעד כרטיסיות יומי (Daily Goal)
        // -------------------------------------------------------------
        View rowDailyGoal = findViewById(R.id.rowDailyGoal);
        TextView tvDailyGoalBadge = findViewById(R.id.tvDailyGoalBadge);
        int currentDailyGoal = StudyRepository.getInstance().getDailyGoal();
        if (tvDailyGoalBadge != null) {
            tvDailyGoalBadge.setText(String.format(Locale.getDefault(), "%d כרטיסיות", currentDailyGoal));
        }
        if (rowDailyGoal != null) {
            ViewAnimationUtils.applyTouchScaleEffect(rowDailyGoal);
            rowDailyGoal.setOnClickListener(v -> showEditDailyGoalDialog(tvDailyGoalBadge));
        }

        // -------------------------------------------------------------
        // 2. הפעלה או כיבוי של אפקטים קוליים בעת תרגול
        // -------------------------------------------------------------
        MaterialSwitch switchSoundEffects = findViewById(R.id.switchSoundEffects);
        if (switchSoundEffects != null) {
            switchSoundEffects.setChecked(SnapStudyApp.isSoundEnabled(this));
            switchSoundEffects.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SnapStudyApp.setSoundEnabled(this, isChecked);
            });
        }

        // -------------------------------------------------------------
        // 3. הגדרות התראות ותזכורת יומית ללימוד
        // -------------------------------------------------------------
        MaterialSwitch switchNotifications = findViewById(R.id.switchNotifications);
        View rowReminderTime = findViewById(R.id.rowReminderTime);
        TextView tvReminderTime = findViewById(R.id.tvReminderTime);

        // טעינת שעת התזכורת השמורה (ברירת מחדל 20:00)
        String savedReminderTime = SnapStudyApp.getReminderTime(this);
        if (tvReminderTime != null) {
            tvReminderTime.setText(savedReminderTime);
        }

        boolean notifsEnabled = SnapStudyApp.isNotificationsEnabled(this);
        if (switchNotifications != null) {
            switchNotifications.setChecked(notifsEnabled);
            updateReminderRowState(rowReminderTime, notifsEnabled);

            // מאזין לשינוי מצב המתג - שומר בהעדפות ומעדכן את נראות שורת השעה
            switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SnapStudyApp.setNotificationsEnabled(this, isChecked);
                updateReminderRowState(rowReminderTime, isChecked);
            });
        }

        // פתיחת דיאלוג בחירת שעה בלחיצה על שורת התזכורת (רק אם התראות מאופשרות)
        if (rowReminderTime != null) {
            ViewAnimationUtils.applyTouchScaleEffect(rowReminderTime);
            rowReminderTime.setOnClickListener(v -> {
                if (SnapStudyApp.isNotificationsEnabled(this)) {
                    showTimePickerDialog(tvReminderTime);
                } else {
                    Toast.makeText(this, "יש להפעיל התראות כדי לקבוע שעה", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // -------------------------------------------------------------
        // 4. התנתקות מהחשבון (Sign Out)
        // -------------------------------------------------------------
        btnSignOut.setOnClickListener(v -> {
            // התנתקות מ-Firebase Auth
            FirebaseAuth.getInstance().signOut();

            // איפוס נתוני הלמידה השמורים בזיכרון המקומי
            StudyRepository.getInstance().clearData();

            Toast.makeText(this, "התנתקת מהחשבון בהצלחה", Toast.LENGTH_SHORT).show();

            // מעבר למסך ההתחברות וניקוי כל ה-Activities הקודמים ממחסנית הניווט (BackStack)
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void finish() {
        super.finish();
        // אנימציית סגירת מסך מותאמת אישית
        ViewAnimationUtils.applyActivityCloseTransition(this);
    }

    /**
     * מעדכנת את העיצוב של כפתורי בחירת ערכת הנושא (Light, Dark, Auto)
     * בהתאם למצב הפעיל כרגע.
     *
     * @param currentMode קבוע המצב מתוך AppCompatDelegate
     */
    private void updateThemeButtonsUI(int currentMode) {
        TextView btnLight = findViewById(R.id.btnThemeLight);
        TextView btnDark = findViewById(R.id.btnThemeDark);
        TextView btnAuto = findViewById(R.id.btnThemeAuto);

        if (btnLight == null || btnDark == null || btnAuto == null) return;

        resetThemeButton(btnLight);
        resetThemeButton(btnDark);
        resetThemeButton(btnAuto);

        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            highlightThemeButton(btnDark);
        } else if (currentMode == AppCompatDelegate.MODE_NIGHT_NO) {
            highlightThemeButton(btnLight);
        } else {
            highlightThemeButton(btnAuto);
        }
    }

    /**
     * מדגישה את הכפתור הנבחר עם רקע ייעודי (Pill), צבע מודגש וטקסט בולט.
     */
    private void highlightThemeButton(TextView btn) {
        btn.setBackgroundResource(R.drawable.bg_pill_selected);
        btn.setTextColor(ContextCompat.getColor(this, R.color.primary));
        btn.setTypeface(null, Typeface.BOLD);
    }

    /**
     * מאפסת את עיצוב הכפתור למצב רגיל שאינו נבחר (ללא רקע וטקסט בצבע משני).
     */
    private void resetThemeButton(TextView btn) {
        btn.setBackground(null);
        btn.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
        btn.setTypeface(null, Typeface.NORMAL);
    }

    /**
     * מעדכנת את המצב הויזואלי והפונקציונלי של שורת קביעת השעה.
     * אם ההתראות כבויות, השורה הופכת לעמומה (שקיפות 0.45) ומושבתת ללחיצה.
     *
     * @param rowReminderTime רכיב ה-View של שורת השעה
     * @param isEnabled       האם ההתראות מופעלות כעת
     */
    private void updateReminderRowState(View rowReminderTime, boolean isEnabled) {
        if (rowReminderTime == null) return;
        rowReminderTime.setEnabled(isEnabled);
        rowReminderTime.setAlpha(isEnabled ? 1.0f : 0.45f);
    }

    /**
     * מציגה דיאלוג לבחירת שעה (TimePickerDialog) בפורמט 24 שעות.
     * השעה הנבחרת נשמרת בהגדרות ומעודכנת בתצוגה.
     *
     * @param tvReminderTime שדה הטקסט המציג את השעה הנבחרת
     */
    private void showTimePickerDialog(TextView tvReminderTime) {
        String currentTime = SnapStudyApp.getReminderTime(this);
        int hour = 20;
        int minute = 0;

        // פענוח השעה והדקות מתוך המחרוזת השמורה (בפורמט HH:mm)
        if (currentTime != null && currentTime.contains(":")) {
            try {
                String[] parts = currentTime.split(":");
                hour = Integer.parseInt(parts[0].trim());
                minute = Integer.parseInt(parts[1].trim());
            } catch (Exception ignored) {}
        }

        // יצירת והצגת דיאלוג בחירת השעה של מערכת אנדרואיד
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    String formatted = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                    SnapStudyApp.setReminderTime(this, formatted);
                    if (tvReminderTime != null) {
                        tvReminderTime.setText(formatted);
                    }
                },
                hour,
                minute,
                true // שימוש בשעון 24 שעות
        );
        timePickerDialog.show();
    }

    /**
     * מציגה דיאלוג להגדרת יעד כרטיסיות יומי חדש.
     * הדיאלוג יוצר שדה קלט מספרי (EditText) עם בדיקות תקינות קלט (ערך חיובי עד 2000).
     *
     * @param tvDailyGoalBadge תגית הטקסט המציגה את היעד הנוכחי במסך
     */
    private void showEditDailyGoalDialog(TextView tvDailyGoalBadge) {
        int currentGoal = StudyRepository.getInstance().getDailyGoal();

        // יצירת שדה הזנת טקסט מספרי מותאם אישית
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(currentGoal));
        input.setSelectAllOnFocus(true);
        input.setGravity(Gravity.CENTER);
        input.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
        input.setTextSize(18);

        // עטיפת ה-EditText במיכל FrameLayout כדי להבטיח שוליים נכונים בהתאם לעיצוב הדיאלוג
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        int marginPx = (int) (24 * getResources().getDisplayMetrics().density);
        params.leftMargin = marginPx;
        params.rightMargin = marginPx;
        params.topMargin = marginPx / 2;
        params.bottomMargin = marginPx / 2;
        input.setLayoutParams(params);
        container.addView(input);

        // בניית והצגת דיאלוג Material 3
        new MaterialAlertDialogBuilder(this)
                .setTitle("יעד כרטיסיות יומי")
                .setMessage("הגדר את מספר הכרטיסיות שברצונך לתרגל בכל יום:")
                .setView(container)
                .setPositiveButton("שמור", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (!text.isEmpty()) {
                        try {
                            int newGoal = Integer.parseInt(text);
                            // בדיקות תקינות (Validation) על ערך היעד
                            if (newGoal <= 0) {
                                Toast.makeText(this, "נא להזין מספר כרטיסיות חיובי", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (newGoal > 2000) {
                                Toast.makeText(this, "היעד המרבי הוא 2000 כרטיסיות", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            // שמירת היעד החדש ב-Repository ועדכון ממשק המשתמש
                            StudyRepository.getInstance().setDailyGoal(newGoal);
                            if (tvDailyGoalBadge != null) {
                                tvDailyGoalBadge.setText(String.format(Locale.getDefault(), "%d כרטיסיות", newGoal));
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "קלט לא תקין", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}

