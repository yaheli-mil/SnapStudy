package com.yalig.snapstudy;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.yalig.snapstudy.data.StatsManager;

/**
 * מסך ההגדרות: פרופיל, ערכת נושא, יעד יומי, שעת תזכורת, צלילים והתנתקות.
 */
public class SettingsActivity extends AppCompatActivity {

    private TextView btnLight, btnDark, btnAuto;
    private TextView tvDailyGoalBadge, tvReminderTime;
    private View rowReminderTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // פרטי המשתמש
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        TextView tvProfileName = findViewById(R.id.tvProfileName);
        TextView tvProfileEmail = findViewById(R.id.tvProfileEmail);
        ImageView ivAvatar = findViewById(R.id.ivAvatar);
        if (user != null) {
            tvProfileName.setText(user.getDisplayName());
            tvProfileEmail.setText(user.getEmail());
            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(ivAvatar);
            }
        }

        findViewById(R.id.btnBackSettings).setOnClickListener(v -> finish());

        // ערכת נושא
        btnLight = findViewById(R.id.btnThemeLight);
        btnDark = findViewById(R.id.btnThemeDark);
        btnAuto = findViewById(R.id.btnThemeAuto);
        btnLight.setOnClickListener(v -> changeTheme(AppCompatDelegate.MODE_NIGHT_NO));
        btnDark.setOnClickListener(v -> changeTheme(AppCompatDelegate.MODE_NIGHT_YES));
        btnAuto.setOnClickListener(v -> changeTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
        markSelectedTheme(SnapStudyApp.getThemeMode(this));

        // יעד יומי
        tvDailyGoalBadge = findViewById(R.id.tvDailyGoalBadge);
        tvDailyGoalBadge.setText(StatsManager.getDailyGoal(this) + " כרטיסיות");
        findViewById(R.id.rowDailyGoal).setOnClickListener(v -> showDailyGoalDialog());

        // צלילים
        CompoundButton switchSound = findViewById(R.id.switchSoundEffects);
        switchSound.setChecked(SnapStudyApp.isSoundEnabled(this));
        switchSound.setOnCheckedChangeListener((button, isChecked) ->
                SnapStudyApp.setSoundEnabled(this, isChecked));

        // תזכורת יומית (נשמרת השעה שנבחרה)
        tvReminderTime = findViewById(R.id.tvReminderTime);
        rowReminderTime = findViewById(R.id.rowReminderTime);
        tvReminderTime.setText(SnapStudyApp.getReminderTime(this));
        CompoundButton switchReminder = findViewById(R.id.switchNotifications);
        switchReminder.setChecked(SnapStudyApp.isReminderEnabled(this));
        rowReminderTime.setEnabled(switchReminder.isChecked());
        switchReminder.setOnCheckedChangeListener((button, isChecked) -> {
            SnapStudyApp.setReminderEnabled(this, isChecked);
            rowReminderTime.setEnabled(isChecked);
        });
        rowReminderTime.setOnClickListener(v -> showTimePicker());

        // התנתקות וחזרה למסך הכניסה
        findViewById(R.id.btnSignOut).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void changeTheme(int mode) {
        SnapStudyApp.setThemeMode(this, mode);
        markSelectedTheme(mode);
    }

    // מסמן את הכפתור של ערכת הנושא שנבחרה, ומחזיר את השניים האחרים למצב רגיל
    private void markSelectedTheme(int mode) {
        TextView[] buttons = {btnLight, btnDark, btnAuto};
        for (TextView button : buttons) {
            button.setBackground(null);
            button.setTextColor(getColor(R.color.on_surface_variant));
        }

        TextView selected;
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            selected = btnLight;
        } else if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            selected = btnDark;
        } else {
            selected = btnAuto;
        }
        selected.setBackgroundResource(R.drawable.bg_pill_selected);
        selected.setTextColor(getColor(R.color.primary));
    }

    private void showDailyGoalDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(StatsManager.getDailyGoal(this)));

        new AlertDialog.Builder(this)
                .setTitle("יעד כרטיסיות יומי")
                .setMessage("כמה כרטיסיות תרצה לתרגל בכל יום?")
                .setView(input)
                .setPositiveButton("שמור", (dialog, which) -> {
                    String text = input.getText().toString();
                    if (text.isEmpty() || Integer.parseInt(text) <= 0) {
                        Toast.makeText(this, "נא להזין מספר גדול מ-0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int goal = Integer.parseInt(text);
                    StatsManager.setDailyGoal(this, goal);
                    tvDailyGoalBadge.setText(goal + " כרטיסיות");
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void showTimePicker() {
        // השעה השמורה היא מחרוזת בפורמט "HH:mm"
        String[] parts = SnapStudyApp.getReminderTime(this).split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
            String time = String.format("%02d:%02d", selectedHour, selectedMinute);
            SnapStudyApp.setReminderTime(this, time);
            tvReminderTime.setText(time);
        }, hour, minute, true).show();
    }
}
