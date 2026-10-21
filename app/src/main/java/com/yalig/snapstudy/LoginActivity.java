package com.yalig.snapstudy;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * מסך כניסה והרשמה: אימייל וסיסמה, שחזור סיסמה, והתחברות עם Google.
 */
public class LoginActivity extends AppCompatActivity {

    // המזהה של האפליקציה ב-Google (מהגדרות Firebase), נדרש להתחברות עם Google
    private static final String WEB_CLIENT_ID = "32193446763-8v30jhv8h0plj9iffjcmt53rthf6f5he.apps.googleusercontent.com";

    private FirebaseAuth auth;
    private boolean isSignUpMode = false;   // false = התחברות, true = הרשמה

    private EditText etFullName, etEmail, etPassword, etConfirmPassword;
    private View tilFullName, tilConfirmPassword, layoutRememberForgot;
    private Button btnLogin;
    private TextView tvSubtitle, tvNoAccountPrompt, tvSignUp;
    private TabLayout tabLayoutAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();

        // אם המשתמש כבר מחובר - עוברים ישר למסך הראשי
        if (auth.getCurrentUser() != null) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tilFullName = findViewById(R.id.tilFullName);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        layoutRememberForgot = findViewById(R.id.layoutRememberForgot);
        btnLogin = findViewById(R.id.btnLogin);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvNoAccountPrompt = findViewById(R.id.tvNoAccountPrompt);
        tvSignUp = findViewById(R.id.tvSignUp);
        tabLayoutAuth = findViewById(R.id.tabLayoutAuth);

        // מעבר בין הלשונית "התחברות" ללשונית "הרשמה"
        tabLayoutAuth.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                setSignUpMode(tab.getPosition() == 1);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        tvSignUp.setOnClickListener(v -> tabLayoutAuth.selectTab(tabLayoutAuth.getTabAt(isSignUpMode ? 0 : 1)));

        btnLogin.setOnClickListener(v -> {
            if (isSignUpMode) {
                signUp();
            } else {
                signIn();
            }
        });
        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> showForgotPasswordDialog());
        findViewById(R.id.btnGoogle).setOnClickListener(v -> signInWithGoogle());

        setSignUpMode(false);
    }

    // מציג או מסתיר את השדות של ההרשמה
    private void setSignUpMode(boolean signUp) {
        isSignUpMode = signUp;
        if (signUp) {
            tilFullName.setVisibility(View.VISIBLE);
            tilConfirmPassword.setVisibility(View.VISIBLE);
            layoutRememberForgot.setVisibility(View.GONE);
            tvSubtitle.setText("צור חשבון חדש והתחל ללמוד");
            btnLogin.setText("יצירת חשבון");
            tvNoAccountPrompt.setText("כבר יש לך חשבון?");
            tvSignUp.setText("התחבר עכשיו");
        } else {
            tilFullName.setVisibility(View.GONE);
            tilConfirmPassword.setVisibility(View.GONE);
            layoutRememberForgot.setVisibility(View.VISIBLE);
            tvSubtitle.setText(R.string.welcome_back);
            btnLogin.setText(R.string.login_btn);
            tvNoAccountPrompt.setText(R.string.no_account);
            tvSignUp.setText(R.string.sign_up);
        }
    }

    private void signIn() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "נא למלא אימייל וסיסמה", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                goToMain();
            } else {
                Toast.makeText(this, "האימייל או הסיסמה לא נכונים", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void signUp() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "נא למלא שם ואימייל", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "הסיסמה צריכה להיות לפחות 6 תווים", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, "הסיסמאות לא זהות", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // שמירת השם של המשתמש בפרופיל שלו ב-Firebase
                UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build();
                auth.getCurrentUser().updateProfile(profile);
                goToMain();
            } else {
                Toast.makeText(this, "ההרשמה נכשלה: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // דיאלוג ששולח למשתמש מייל לאיפוס הסיסמה
    private void showForgotPasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        EditText etResetEmail = dialogView.findViewById(R.id.etResetEmail);
        etResetEmail.setText(etEmail.getText());

        new AlertDialog.Builder(this)
                .setTitle("איפוס סיסמה")
                .setMessage("הזן את האימייל שאיתו נרשמת, ונשלח אליך קישור לאיפוס הסיסמה:")
                .setView(dialogView)
                .setPositiveButton("שלח קישור", (dialog, which) -> {
                    String email = etResetEmail.getText().toString().trim();
                    if (email.isEmpty()) {
                        Toast.makeText(this, "נא להזין אימייל", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "נשלח קישור לאיפוס אל " + email, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "לא הצלחנו לשלוח את הקישור", Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // התחברות עם Google: מקבלים טוקן מ-Google ומתחברים איתו ל-Firebase
    private void signInWithGoogle() {
        GetSignInWithGoogleOption googleOption = new GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID).build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build();

        CredentialManager credentialManager = CredentialManager.create(this);
        credentialManager.getCredentialAsync(this, request, new CancellationSignal(), getMainExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        GoogleIdTokenCredential googleCredential =
                                GoogleIdTokenCredential.createFrom(result.getCredential().getData());
                        AuthCredential firebaseCredential =
                                GoogleAuthProvider.getCredential(googleCredential.getIdToken(), null);

                        auth.signInWithCredential(firebaseCredential).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                goToMain();
                            } else {
                                Toast.makeText(LoginActivity.this, "ההתחברות עם Google נכשלה", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Toast.makeText(LoginActivity.this, "ההתחברות עם Google בוטלה או נכשלה", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
