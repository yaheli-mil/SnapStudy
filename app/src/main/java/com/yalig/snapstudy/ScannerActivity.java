package com.yalig.snapstudy;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.yalig.snapstudy.ai.GeminiStudyService;
import com.yalig.snapstudy.data.StudyRepository;
import com.yalig.snapstudy.models.StudySet;
import com.yalig.snapstudy.ui.DocumentOverlayView;

import java.io.File;

/**
 * מסך הסריקה: מצלמים דף (באפליקציית המצלמה של הטלפון) או בוחרים תמונה מהגלריה,
 * ML Kit מזהה איפה יש טקסט, ו-Gemini יוצר מהתמונה ערכת כרטיסיות.
 */
public class ScannerActivity extends AppCompatActivity {

    private DocumentOverlayView overlayView;
    private TextView tvInstruction, tvPlaceholder;
    private Button btnCreateCards;

    private Bitmap photo;        // התמונה שנבחרה
    private Uri cameraPhotoUri;  // לאן המצלמה שומרת את התמונה

    // בקשת הרשאת מצלמה בזמן ריצה
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "בלי הרשאת מצלמה אפשר לבחור תמונה מהגלריה", Toast.LENGTH_LONG).show();
                }
            });

    // צילום עם אפליקציית המצלמה של הטלפון
    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success) {
                    loadPhoto(cameraPhotoUri);
                }
            });

    // בחירת תמונה מהגלריה
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    loadPhoto(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        overlayView = findViewById(R.id.documentOverlayView);
        tvInstruction = findViewById(R.id.tvScanInstruction);
        tvPlaceholder = findViewById(R.id.tvPlaceholder);
        btnCreateCards = findViewById(R.id.btnCreateCards);

        findViewById(R.id.btnCloseScanner).setOnClickListener(v -> finish());
        findViewById(R.id.btnGallery).setOnClickListener(v -> galleryLauncher.launch("image/*"));
        findViewById(R.id.btnTakePhoto).setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });
        btnCreateCards.setOnClickListener(v -> createCards());
    }

    private void openCamera() {
        // המצלמה שומרת את התמונה בקובץ בתיקיית ה-cache של האפליקציה
        File photoFile = new File(getCacheDir(), "photo.jpg");
        cameraPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
        takePictureLauncher.launch(cameraPhotoUri);
    }

    // טעינת התמונה, הצגה שלה וחיפוש טקסט בתוכה
    private void loadPhoto(Uri uri) {
        try {
            ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
            photo = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                // תמונה מאוד גדולה - טוענים אותה בחצי גודל כדי לחסוך בזיכרון
                if (info.getSize().getWidth() > 2000) {
                    decoder.setTargetSampleSize(2);
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "לא הצלחנו לטעון את התמונה", Toast.LENGTH_SHORT).show();
            return;
        }

        tvPlaceholder.setVisibility(View.GONE);
        overlayView.setPhoto(photo);
        btnCreateCards.setEnabled(true);
        findText(photo);
    }

    // ML Kit מוצא את כל בלוקי הטקסט, ומסמנים מסגרת אחת שמקיפה את כולם
    private void findText(Bitmap bitmap) {
        tvInstruction.setText("מחפש טקסט בתמונה...");
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener(result -> {
                    Rect textArea = null;
                    for (Text.TextBlock block : result.getTextBlocks()) {
                        Rect box = block.getBoundingBox();
                        if (box == null) {
                            continue;
                        }
                        if (textArea == null) {
                            textArea = new Rect(box);
                        } else {
                            textArea.union(box);
                        }
                    }

                    if (textArea != null) {
                        overlayView.setTextBounds(textArea);
                        tvInstruction.setText("זוהה טקסט בתמונה (מסומן במסגרת). אפשר ליצור כרטיסיות.");
                    } else {
                        tvInstruction.setText("לא זוהה טקסט ברור. אפשר לנסות בכל זאת או לצלם שוב.");
                    }
                })
                .addOnFailureListener(e -> tvInstruction.setText("אפשר ליצור כרטיסיות מהתמונה."));
    }

    // שליחת התמונה ל-Gemini ושמירת הערכה שנוצרה
    private void createCards() {
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setTitle("סריקה חכמה")
                .setMessage("Gemini קורא את הדף ומכין כרטיסיות...")
                .setCancelable(false)
                .show();

        GeminiStudyService.generateFlashcards(this, photo, new GeminiStudyService.AiStudyCallback() {
            @Override
            public void onSuccess(StudySet studySet) {
                loadingDialog.dismiss();
                StudyRepository.saveSet(studySet);

                new AlertDialog.Builder(ScannerActivity.this)
                        .setTitle("הערכה נוצרה! 🎉")
                        .setMessage("נוצרו " + studySet.getCardCount() + " כרטיסיות בנושא \"" + studySet.getTitle() + "\".")
                        .setPositiveButton("תרגל עכשיו", (dialog, which) -> {
                            Intent intent = new Intent(ScannerActivity.this, FlashcardsActivity.class);
                            intent.putExtra("studySet", studySet);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton("חזרה למסך הראשי", (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }

            @Override
            public void onError(String message) {
                loadingDialog.dismiss();
                new AlertDialog.Builder(ScannerActivity.this)
                        .setTitle("שגיאה")
                        .setMessage(message)
                        .setPositiveButton("נסה שוב", (dialog, which) -> createCards())
                        .setNegativeButton("סגור", null)
                        .show();
            }
        });
    }
}
