package com.yalig.snapstudy;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.yalig.snapstudy.data.StudyRepository;
import com.yalig.snapstudy.models.StudySet;

public class NewUserActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SnapStudyApp.applyThemeToActivity(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_user);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.newUserRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        ShapeableImageView ivAvatar = findViewById(R.id.ivUserAvatarNewUser);
        if (ivAvatar != null) {
            if (user != null && user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .placeholder(R.drawable.avatar_user)
                        .error(R.drawable.avatar_user)
                        .circleCrop()
                        .into(ivAvatar);
            }
            ivAvatar.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        }

        findViewById(R.id.btnEmptyScan).setOnClickListener(v -> {
            startActivity(new Intent(this, ScannerActivity.class));
        });

        findViewById(R.id.btnEmptyNewSet).setOnClickListener(v -> showCreateSetDialog());
    }

    private void showCreateSetDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);
        layout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        final EditText etTitle = new EditText(this);
        etTitle.setHint("שם ערכת הלימוד (לדוגמה: היסטוריה - מלחמת העולם)");
        layout.addView(etTitle);

        final EditText etCategory = new EditText(this);
        etCategory.setHint("תחום לימוד (לדוגמה: היסטוריה, ביולוגיה, תנ\"ך)");
        layout.addView(etCategory);

        new AlertDialog.Builder(this)
                .setTitle("ערכת לימוד חדשה")
                .setMessage("הזן פרטים ראשוניים כדי להתחיל להוסיף כרטיסיות")
                .setView(layout)
                .setPositiveButton("צור והתחל ללמוד", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String category = etCategory.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(this, "נא להזין שם לערכת הלימוד", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (category.isEmpty()) {
                        category = "כללי";
                    }

                    String newId = "set_" + System.currentTimeMillis();
                    StudySet newSet = new StudySet(newId, title, category, "נוצר הרגע", 0);
                    StudyRepository.getInstance().addStudySet(newSet);

                    Toast.makeText(this, "הערכה \"" + title + "\" נוצרה בהצלחה!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}
