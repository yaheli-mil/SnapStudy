package com.yalig.snapstudy;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.gms.tasks.Task;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import android.graphics.Typeface;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.OAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.yalig.snapstudy.utils.ViewAnimationUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * מסך ההתחברות וההרשמה (LoginActivity) של אפליקציית SnapStudy.
 * מסך זה מאפשר כניסה למערכת בשתי דרכים מאובטחות:
 * 1. הזנת כתובת אימייל וסיסמה (באמצעות Firebase Authentication) כולל הרשמה ואיפוס סיסמה.
 * 2. התחברות מהירה בלחיצה אחת עם חשבון Google באמצעות ספריית ה-Credential Manager המודרנית.
 * המסך מציג טופס אינטראקטיבי המתחלף בצורה מונפשת בין מצב כניסה (Sign In) למצב הרשמה (Sign Up).
 */
public class LoginActivity extends AppCompatActivity {

    // תגית רישום לוגים לניפוי שגיאות
    private static final String TAG = "LoginActivity";

    // מזהה הלקוח של Google Web Client ID לאימות OAuth מול Firebase
    private static final String WEB_CLIENT_ID = "32193446763-8v30jhv8h0plj9iffjcmt53rthf6f5he.apps.googleusercontent.com";

    // מופע שירות האימות של Firebase
    private FirebaseAuth mAuth;

    // מנהל הרשאות וחשבונות של Android (Credential Manager) עבור התחברות Google
    private CredentialManager credentialManager;

    // רכיבי ממשק משתמש (Views)
    private MaterialCardView cardLogin;
    private TabLayout tabLayoutAuth;
    private View formFieldsContainer;
    private TextInputLayout tilFullName;
    private TextInputEditText etFullName;
    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etPassword;
    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etConfirmPassword;
    private View layoutRememberForgot;
    private MaterialButton btnLogin;
    private TextView tvSubtitle;
    private TextView tvSocialDivider;
    private TextView tvNoAccountPrompt;
    private TextView tvSignUp;
    private TextView tvForgotPassword;

    // משתנה מצב: האם הטופס נמצא כעת במצב הרשמה (true) או התחברות (false)
    private boolean isSignUpMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // החלת ערכת הנושא השמורה (בהיר/כהה)
        SnapStudyApp.applyThemeToActivity(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        // בדיקה האם המשתמש כבר מחובר למערכת מסשן קודם - אם כן, מעבר ישיר למסך הראשי
        if (mAuth.getCurrentUser() != null) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        // התאמת שולי המסך לפסי המערכת (Status Bar ו-Navigation Bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupListeners();
        // הגדרת מצב התחלתי: התחברות (ללא אנימציה)
        setAuthMode(false, false);
    }

    /**
     * קישור כל משתני המחלקה לרכיבי ה-XML המתאימים במסך.
     */
    private void initViews() {
        cardLogin = findViewById(R.id.cardLogin);
        tabLayoutAuth = findViewById(R.id.tabLayoutAuth);
        formFieldsContainer = findViewById(R.id.formFieldsContainer);
        tilFullName = findViewById(R.id.tilFullName);
        etFullName = findViewById(R.id.etFullName);
        tilEmail = findViewById(R.id.tilEmail);
        etEmail = findViewById(R.id.etEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etPassword = findViewById(R.id.etPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        layoutRememberForgot = findViewById(R.id.layoutRememberForgot);
        btnLogin = findViewById(R.id.btnLogin);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvSocialDivider = findViewById(R.id.tvSocialDivider);
        tvNoAccountPrompt = findViewById(R.id.tvNoAccountPrompt);
        tvSignUp = findViewById(R.id.tvSignUp);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
    }

    /**
     * הגדרת מאזינים ללחיצות, אפקטי מגע ואירועי שינוי טאבים.
     */
    private void setupListeners() {
        View btnGoogle = findViewById(R.id.btnGoogle);

        // החלת אפקט לחיצה קפיצי על כל הכפתורים האינטראקטיביים
        ViewAnimationUtils.applyTouchScaleEffect(btnLogin);
        ViewAnimationUtils.applyTouchScaleEffect(btnGoogle);
        ViewAnimationUtils.applyTouchScaleEffect(tvSignUp);
        ViewAnimationUtils.applyTouchScaleEffect(tvForgotPassword);

        // מאזין למעבר בין טאב "התחברות" לטאב "הרשמה"
        tabLayoutAuth.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                boolean isSignUp = (tab.getPosition() == 1);
                if (isSignUpMode != isSignUp) {
                    setAuthMode(isSignUp, true);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // לחיצה על טקסט הקישור בתחתית המסך ("הרשם עכשיו" / "התחבר עכשיו")
        tvSignUp.setOnClickListener(v -> {
            boolean nextMode = !isSignUpMode;
            TabLayout.Tab targetTab = tabLayoutAuth.getTabAt(nextMode ? 1 : 0);
            if (targetTab != null) {
                targetTab.select();
            } else {
                setAuthMode(nextMode, true);
            }
        });

        // כפתור אישור כניסה / הרשמה בדוא"ל וסיסמה
        btnLogin.setOnClickListener(v -> handleEmailAuth());

        // כפתור התחברות עם חשבון Google
        findViewById(R.id.btnGoogle).setOnClickListener(v -> signInWithGoogle());

        // כפתור "שכחת סיסמה?" לפתיחת דיאלוג איפוס
        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
    }

    /**
     * מעבר חלק בין מצב התחברות (Sign In) למצב הרשמה (Sign Up).
     * כולל אנימציית גובה לכרטיס, תנועה אופקית עדינה לשדות ודעיכת טקסט הכותרת.
     * @param isSignUp אמת אם עוברים למצב הרשמה, שקר אם להתחברות.
     * @param animate האם לבצע את המעבר בליווי אנימציה.
     */
    private void setAuthMode(boolean isSignUp, boolean animate) {
        if (this.isSignUpMode == isSignUp && !animate) return;
        this.isSignUpMode = isSignUp;

        // איפוס שגיאות קלט קודמות
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        // סנכרון הטאב הנבחר ב-TabLayout
        int targetTabPos = isSignUp ? 1 : 0;
        if (tabLayoutAuth != null && tabLayoutAuth.getSelectedTabPosition() != targetTabPos) {
            TabLayout.Tab tab = tabLayoutAuth.getTabAt(targetTabPos);
            if (tab != null && !tab.isSelected()) {
                tab.select();
            }
        }

        // ביצוע מעבר מונפש לרכיבי הטופס וגובה הכרטיס
        if (animate) {
            AutoTransition transition = new AutoTransition();
            transition.setDuration(260);
            transition.setInterpolator(new FastOutSlowInInterpolator());
            TransitionManager.beginDelayedTransition(cardLogin, transition);

            // אנימציית החלקה ודעיכה לשדות הטופס
            float slideOffset = isSignUp ? -25f : 25f;
            formFieldsContainer.animate()
                    .alpha(0.15f)
                    .translationX(slideOffset)
                    .setDuration(120)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        updateFormVisibility(isSignUp);
                        formFieldsContainer.setTranslationX(-slideOffset);
                        formFieldsContainer.animate()
                                .alpha(1f)
                                .translationX(0f)
                                .setDuration(160)
                                .setInterpolator(new FastOutSlowInInterpolator())
                                .start();
                    }).start();

            // אנימציית שינוי טקסט כותרת המשנה
            tvSubtitle.animate().alpha(0f).setDuration(100).withEndAction(() -> {
                updateTextLabels(isSignUp);
                tvSubtitle.animate().alpha(1f).setDuration(150).start();
            }).start();
        } else {
            updateFormVisibility(isSignUp);
            updateTextLabels(isSignUp);
        }
    }

    /**
     * עדכון נראות השדות הרלוונטיים (שם מלא ואימות סיסמה מופיעים רק בהרשמה).
     */
    private void updateFormVisibility(boolean isSignUp) {
        tilFullName.setVisibility(isSignUp ? View.VISIBLE : View.GONE);
        tilConfirmPassword.setVisibility(isSignUp ? View.VISIBLE : View.GONE);
        layoutRememberForgot.setVisibility(isSignUp ? View.GONE : View.VISIBLE);
    }

    /**
     * עדכון הטקסטים בכפתורים ובכותרות בהתאם למצב הנוכחי.
     */
    private void updateTextLabels(boolean isSignUp) {
        if (isSignUp) {
            tvSubtitle.setText("צור חשבון חדש והתחל ללמוד");
            btnLogin.setText("יצירת חשבון חדש");
            tvSocialDivider.setText("או הרשם באמצעות");
            tvNoAccountPrompt.setText("כבר יש לך חשבון?");
            tvSignUp.setText("התחבר עכשיו");
        } else {
            tvSubtitle.setText(getString(R.string.welcome_back));
            btnLogin.setText(getString(R.string.login_btn));
            tvSocialDivider.setText(getString(R.string.or_continue_with));
            tvNoAccountPrompt.setText(getString(R.string.no_account));
            tvSignUp.setText(getString(R.string.sign_up));
        }
    }

    /**
     * טיפול באימות משתמש באמצעות אימייל וסיסמה:
     * 1. בדיקת תקינות הקלט (Validation) - אימייל תקין, אורך סיסמה, התאמת סיסמאות.
     * 2. שליחת בקשת יצירת משתמש או התחברות ל-Firebase Auth.
     * 3. עדכון שם המשתמש בפרופיל Firebase במידה ומדובר בהרשמה חדשה.
     */
    private void handleEmailAuth() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (isSignUpMode) {
            String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
            String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

            // בדיקת שם משתמש
            if (TextUtils.isEmpty(fullName)) {
                tilFullName.setError("נא להזין שם משתמש");
                etFullName.requestFocus();
                return;
            } else {
                tilFullName.setError(null);
            }

            // בדיקת תקינות פורמט אימייל
            if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.setError("נא להזין כתובת אימייל תקינה");
                etEmail.requestFocus();
                return;
            } else {
                tilEmail.setError(null);
            }

            // בדיקת אורך סיסמה מינימלי
            if (TextUtils.isEmpty(password) || password.length() < 6) {
                tilPassword.setError("סיסמה חייבת להכיל לפחות 6 תווים");
                etPassword.requestFocus();
                return;
            } else {
                tilPassword.setError(null);
            }

            // בדיקת התאמת סיסמה לאימות סיסמה
            if (!password.equals(confirmPassword)) {
                tilConfirmPassword.setError("הסיסמאות אינן תואמות");
                etConfirmPassword.requestFocus();
                return;
            } else {
                tilConfirmPassword.setError(null);
            }

            btnLogin.setEnabled(false);
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        btnLogin.setEnabled(true);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(fullName)
                                        .build();
                                user.updateProfile(profileUpdates);
                            }
                            Toast.makeText(this, "נרשמת בהצלחה! ברוך הבא ל-SnapStudy", Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        } else {
                            String error = task.getException() != null ? task.getException().getLocalizedMessage() : "שגיאה ברישום";
                            Toast.makeText(this, "שגיאה: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            // בדיקת קלט בהתחברות
            if (TextUtils.isEmpty(email)) {
                tilEmail.setError("נא להזין כתובת אימייל");
                etEmail.requestFocus();
                return;
            } else {
                tilEmail.setError(null);
            }

            if (TextUtils.isEmpty(password) || password.length() < 6) {
                tilPassword.setError("סיסמה חייבת להכיל לפחות 6 תווים");
                etPassword.requestFocus();
                return;
            } else {
                tilPassword.setError(null);
            }

            btnLogin.setEnabled(false);
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        btnLogin.setEnabled(true);
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "התחברת בהצלחה!", Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        } else {
                            String error = task.getException() != null ? task.getException().getLocalizedMessage() : "שגיאה בהתחברות";
                            Toast.makeText(this, "שגיאה: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    /**
     * התחברות באמצעות חשבון Google תוך שימוש בספריית ה-Credential Manager של Android.
     * ספרייה זו מחליפה את ה-Google Sign-In הישן ומספקת חוויית משתמש מאובטחת ובלחיצה אחת.
     */
    private void signInWithGoogle() {
        GetSignInWithGoogleOption signInWithGoogleOption = new GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        Credential credential = result.getCredential();
                        if (credential instanceof CustomCredential &&
                                credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                            try {
                                GoogleIdTokenCredential googleIdTokenCredential =
                                        GoogleIdTokenCredential.createFrom(credential.getData());
                                String idToken = googleIdTokenCredential.getIdToken();
                                firebaseAuthWithGoogle(idToken);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing Google credential", e);
                                Toast.makeText(LoginActivity.this, "שגיאה בפענוח נתוני Google", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.w(TAG, "Unexpected credential type: " + credential.getType());
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        if (e instanceof GetCredentialCancellationException) {
                            Log.d(TAG, "Google sign in cancelled by user");
                        } else if (e instanceof NoCredentialException) {
                            Log.e(TAG, "No credentials available", e);
                            Toast.makeText(LoginActivity.this, "לא נמצא חשבון Google מחובר במכשיר. יש לוודא שהמכשיר/האמולטור מחובר לחשבון Google בהגדרות.", Toast.LENGTH_LONG).show();
                        } else {
                            Log.e(TAG, "CredentialManager error: " + e.getMessage(), e);
                            Toast.makeText(LoginActivity.this, "התחברות נכשלה: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    /**
     * אימות ה-Token שהתקבל מחשבון Google מול שרת ה-Firebase Auth.
     * @param idToken ה-Token המאומת של גוגל.
     */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "התחברת עם Google בהצלחה!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        String error = task.getException() != null ? task.getException().getLocalizedMessage() : "שגיאה";
                        Toast.makeText(this, "שגיאה בהתחברות עם Google: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * פתיחת דיאלוג ייעודי לאיפוס סיסמה עבור משתמש ששכח את סיסמתו.
     * הדיאלוג בודק תחילה האם המשתמש קיים והאם הוא מחובר באמצעות סיסמה או ספק חיצוני (כמו Google).
     */
    private void handleForgotPassword() {
        String currentEmail = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        TextInputLayout tilResetEmail = dialogView.findViewById(R.id.tilResetEmail);
        TextInputEditText etResetEmail = dialogView.findViewById(R.id.etResetEmail);

        if (!TextUtils.isEmpty(currentEmail)) {
            etResetEmail.setText(currentEmail);
            etResetEmail.setSelection(currentEmail.length());
        }

        etResetEmail.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilResetEmail.setError(null);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("🔑 איפוס סיסמה")
                .setMessage("הזן את כתובת האימייל שאיתה נרשמת ל-SnapStudy, ונשלח אליך קישור לאיפוס הסיסמה:")
                .setView(dialogView)
                .setPositiveButton("שלח קישור ✨", null)
                .setNegativeButton("ביטול", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnPositive.setOnClickListener(v -> {
                String email = etResetEmail.getText() != null ? etResetEmail.getText().toString().trim() : "";

                if (TextUtils.isEmpty(email)) {
                    tilResetEmail.setError("נא להזין כתובת אימייל");
                    etResetEmail.requestFocus();
                    return;
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    tilResetEmail.setError("כתובת האימייל אינה תקינה");
                    etResetEmail.requestFocus();
                    return;
                }

                tilResetEmail.setError(null);
                btnPositive.setEnabled(false);
                btnPositive.setText("בודק...");

                mAuth.useAppLanguage();
                // בדיקת ספקי ההתחברות המשויכים לאימייל זה
                mAuth.fetchSignInMethodsForEmail(email)
                        .addOnCompleteListener(task -> {
                            if (!dialog.isShowing()) return;

                            if (task.isSuccessful()) {
                                List<String> methods = task.getResult() != null ? task.getResult().getSignInMethods() : null;
                                if (methods == null || methods.isEmpty()) {
                                    btnPositive.setEnabled(true);
                                    btnPositive.setText("שלח קישור ✨");
                                    tilResetEmail.setError("לא נמצא משתמש המשויך לכתובת זו. נא לבדוק את הכתובת או להירשם.");
                                    etResetEmail.requestFocus();
                                    return;
                                }

                                // אם החשבון נוצר באמצעות Google בלבד, אין לו סיסמה לאפס
                                if (!methods.contains("password")) {
                                    btnPositive.setEnabled(true);
                                    btnPositive.setText("שלח קישור ✨");
                                    String provider = methods.contains("google.com") ? "Google" : "חשבון חיצוני";
                                    new AlertDialog.Builder(LoginActivity.this)
                                            .setTitle("חשבון ללא סיסמה")
                                            .setMessage("חשבון זה מחובר באמצעות " + provider + ".\nאין צורך באיפוס סיסמה — ניתן להתחבר ישירות באמצעות כפתור " + provider + ".")
                                            .setPositiveButton("הבנתי", null)
                                            .show();
                                    return;
                                }

                                btnPositive.setText("שולח קישור...");
                                sendResetEmailDirectly(dialog, btnPositive, tilResetEmail, etResetEmail, email);
                            } else {
                                Exception ex = task.getException();
                                if (ex instanceof FirebaseAuthInvalidUserException) {
                                    btnPositive.setEnabled(true);
                                    btnPositive.setText("שלח קישור ✨");
                                    tilResetEmail.setError("לא נמצא משתמש המשויך לכתובת זו. נא לבדוק את הכתובת או להירשם.");
                                    etResetEmail.requestFocus();
                                } else {
                                    sendResetEmailDirectly(dialog, btnPositive, tilResetEmail, etResetEmail, email);
                                }
                            }
                        });
            });
        });

        dialog.show();
    }

    /**
     * שליחה ישירה של הודעת הדוא"ל לאיפוס סיסמה באמצעות Firebase Authentication.
     */
    private void sendResetEmailDirectly(AlertDialog dialog, Button btnPositive, TextInputLayout tilResetEmail, TextInputEditText etResetEmail, String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (!dialog.isShowing()) return;

                    if (task.isSuccessful()) {
                        dialog.dismiss();
                        new AlertDialog.Builder(LoginActivity.this)
                                .setTitle("✉️ קישור לאיפוס נשלח!")
                                .setMessage("קישור לאיפוס סיסמה נשלח אל:\n" + email +
                                        "\n\n⚠️ שימו לב: אם המייל לא מופיע בתיבת הדואר הנכנס תוך מספר דקות, בדקו בתיקיית דואר זבל / ספאם (Spam) או בתיקיית קידומי מכירות (Promotions).")
                                .setPositiveButton("הבנתי", null)
                                .show();
                    } else {
                        btnPositive.setEnabled(true);
                        btnPositive.setText("שלח קישור ✨");

                        Exception ex = task.getException();
                        if (ex instanceof FirebaseAuthInvalidUserException ||
                                (ex != null && ex.getMessage() != null && ex.getMessage().contains("EMAIL_NOT_FOUND"))) {
                            tilResetEmail.setError("לא נמצא משתמש המשויך לכתובת זו. נא לבדוק את הכתובת או להירשם.");
                            etResetEmail.requestFocus();
                        } else {
                            String errorMsg = ex != null ? ex.getLocalizedMessage() : "שגיאה בשליחת המייל";
                            new AlertDialog.Builder(LoginActivity.this)
                                    .setTitle("שגיאה בשליחת קישור איפוס")
                                    .setMessage(errorMsg)
                                    .setPositiveButton("סגור", null)
                                    .show();
                        }
                    }
                });
    }

    /**
     * מעבר למסך הראשי של האפליקציה וניקוי מחסנית המסכים (Clear Task).
     */
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ViewAnimationUtils.startActivityWithTransition(this, intent);
        finish();
    }
}


