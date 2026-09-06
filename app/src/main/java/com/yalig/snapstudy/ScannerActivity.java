package com.yalig.snapstudy;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;
import com.google.common.util.concurrent.ListenableFuture;
import com.yalig.snapstudy.ai.GeminiStudyService;
import com.yalig.snapstudy.data.StudyRepository;
import com.yalig.snapstudy.models.StudySet;
import com.yalig.snapstudy.utils.ViewAnimationUtils;

import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.graphics.RectF;
import androidx.camera.core.ImageAnalysis;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.yalig.snapstudy.ui.DocumentOverlayView;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScannerActivity extends AppCompatActivity {

    private static final String TAG = "ScannerActivity";

    private int flashMode = 0;
    private final String[] flashLabels = {"כבוי", "פעיל", "אוטו"};
    private ObjectAnimator laserAnimator;

    private PreviewView cameraPreview;
    private ImageCapture imageCapture;
    private Camera camera;

    // Document detection & overlay state
    private boolean isAutoMode = true;
    private volatile boolean isCapturingOrProcessing = false;
    private long documentFirstDetectedTime = 0;
    private int consecutiveEmptyFrames = 0;

    private DocumentOverlayView documentOverlayView;
    private TextView tvScanInstruction;
    private TextView btnModeAuto;
    private TextView btnModeManual;
    private View scanFrame;

    private TextRecognizer textRecognizer;
    private ExecutorService analysisExecutor;

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "דרושה הרשאת מצלמה לצילום מסמכים. ניתן לבחור תמונה מהגלריה.", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    loadBitmapFromUriAndProcess(uri);
                } else {
                    isCapturingOrProcessing = false;
                    documentFirstDetectedTime = 0;
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SnapStudyApp.applyThemeToActivity(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scanner);

        cameraPreview = findViewById(R.id.cameraPreview);
        documentOverlayView = findViewById(R.id.documentOverlayView);
        tvScanInstruction = findViewById(R.id.tvScanInstruction);
        btnModeAuto = findViewById(R.id.btnModeAuto);
        btnModeManual = findViewById(R.id.btnModeManual);
        scanFrame = findViewById(R.id.scanFrame);

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        analysisExecutor = Executors.newSingleThreadExecutor();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scannerRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            View btnClose = findViewById(R.id.btnCloseScanner);
            if (btnClose != null) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) btnClose.getLayoutParams();
                int margin16 = (int) (16 * getResources().getDisplayMetrics().density);
                lp.topMargin = systemBars.top + margin16;
                btnClose.setLayoutParams(lp);
            }
            View bottomControls = findViewById(R.id.bottomControls);
            if (bottomControls != null) {
                int baseBottom = (int) (28 * getResources().getDisplayMetrics().density);
                bottomControls.setPadding(
                        bottomControls.getPaddingLeft(),
                        bottomControls.getPaddingTop(),
                        bottomControls.getPaddingRight(),
                        baseBottom + systemBars.bottom
                );
            }
            return insets;
        });

        View btnClose = findViewById(R.id.btnCloseScanner);
        View btnFlash = findViewById(R.id.btnFlash);
        View btnShutter = findViewById(R.id.btnShutter);
        View btnGallery = findViewById(R.id.btnGallery);

        ViewAnimationUtils.applyTouchScaleEffect(btnClose);
        ViewAnimationUtils.applyTouchScaleEffect(btnFlash);
        ViewAnimationUtils.applyTouchScaleEffect(btnShutter);
        ViewAnimationUtils.applyTouchScaleEffect(btnGallery);
        ViewAnimationUtils.applyTouchScaleEffect(btnModeAuto);
        ViewAnimationUtils.applyTouchScaleEffect(btnModeManual);

        btnClose.setOnClickListener(v -> finish());

        // Animate scanner laser line
        View laser = findViewById(R.id.laserLine);
        if (laser != null) {
            laserAnimator = ObjectAnimator.ofFloat(laser, "translationY", -120f, 180f);
            laserAnimator.setDuration(1600);
            laserAnimator.setRepeatMode(ValueAnimator.REVERSE);
            laserAnimator.setRepeatCount(ValueAnimator.INFINITE);
            laserAnimator.start();
        }

        TextView tvFlashText = findViewById(R.id.tvFlashText);
        btnFlash.setOnClickListener(v -> {
            flashMode = (flashMode + 1) % flashLabels.length;
            tvFlashText.setText(flashLabels[flashMode]);
            updateTorchState();
        });

        btnShutter.setOnClickListener(v -> takePhotoAndProcess());

        btnGallery.setOnClickListener(v -> {
            isCapturingOrProcessing = true;
            if (documentOverlayView != null) documentOverlayView.clear();
            galleryLauncher.launch("image/*");
        });

        btnModeAuto.setOnClickListener(v -> setScanMode(true));
        btnModeManual.setOnClickListener(v -> setScanMode(false));

        setScanMode(true);

        checkPermissionAndStartCamera();
    }

    private void setScanMode(boolean auto) {
        this.isAutoMode = auto;
        documentFirstDetectedTime = 0;
        consecutiveEmptyFrames = 0;
        if (documentOverlayView != null) {
            documentOverlayView.clear();
        }

        if (scanFrame != null) {
            scanFrame.setBackgroundResource(R.drawable.bg_card_white);
            scanFrame.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#15FFFFFF")));
        }

        if (isAutoMode) {
            btnModeAuto.setBackgroundResource(R.drawable.bg_chip);
            btnModeAuto.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
            btnModeAuto.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnModeAuto.setTypeface(null, Typeface.BOLD);

            btnModeManual.setBackground(null);
            btnModeManual.setTextColor(Color.parseColor("#B3FFFFFF"));
            btnModeManual.setTypeface(null, Typeface.NORMAL);

            if (tvScanInstruction != null) {
                tvScanInstruction.setText("כוון למסמך לצילום אוטומטי");
            }
        } else {
            btnModeManual.setBackgroundResource(R.drawable.bg_chip);
            btnModeManual.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
            btnModeManual.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnModeManual.setTypeface(null, Typeface.BOLD);

            btnModeAuto.setBackground(null);
            btnModeAuto.setTextColor(Color.parseColor("#B3FFFFFF"));
            btnModeAuto.setTypeface(null, Typeface.NORMAL);

            if (tvScanInstruction != null) {
                tvScanInstruction.setText("כוון למסמך ולחץ על כפתור הצילום");
            }
        }
    }

    private void checkPermissionAndStartCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(analysisExecutor, this::analyzeImageForDocument);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);

                updateTorchState();

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Error binding camera lifecycle: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @androidx.camera.core.ExperimentalGetImage
    private void analyzeImageForDocument(@NonNull ImageProxy imageProxy) {
        if (!isAutoMode || isCapturingOrProcessing) {
            imageProxy.close();
            return;
        }

        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
        InputImage image = InputImage.fromMediaImage(mediaImage, rotationDegrees);

        int proxyWidth = imageProxy.getWidth();
        int proxyHeight = imageProxy.getHeight();
        int rotWidth = (rotationDegrees == 90 || rotationDegrees == 270) ? proxyHeight : proxyWidth;
        int rotHeight = (rotationDegrees == 90 || rotationDegrees == 270) ? proxyWidth : proxyHeight;

        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    if (!isAutoMode || isCapturingOrProcessing) {
                        return;
                    }

                    int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
                    int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
                    int totalChars = 0;

                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        Rect box = block.getBoundingBox();
                        if (box != null && box.width() > 10 && box.height() > 10) {
                            minX = Math.min(minX, box.left);
                            minY = Math.min(minY, box.top);
                            maxX = Math.max(maxX, box.right);
                            maxY = Math.max(maxY, box.bottom);
                            totalChars += block.getText().trim().length();
                        }
                    }

                    if (totalChars >= 3 && minX < maxX && minY < maxY) {
                        consecutiveEmptyFrames = 0;

                        // Expand bounding box around text to frame the document page
                        float padX = Math.max(30f, (maxX - minX) * 0.12f);
                        float padY = Math.max(40f, (maxY - minY) * 0.12f);

                        float docLeft = Math.max(8f, minX - padX);
                        float docTop = Math.max(8f, minY - padY);
                        float docRight = Math.min(rotWidth - 8f, maxX + padX);
                        float docBottom = Math.min(rotHeight - 8f, maxY + padY);

                        int viewWidth = cameraPreview.getWidth();
                        int viewHeight = cameraPreview.getHeight();

                        if (viewWidth > 0 && viewHeight > 0 && rotWidth > 0 && rotHeight > 0) {
                            float scale = Math.max((float) viewWidth / rotWidth, (float) viewHeight / rotHeight);
                            float offsetX = (viewWidth - rotWidth * scale) / 2f;
                            float offsetY = (viewHeight - rotHeight * scale) / 2f;

                            float screenLeft = Math.max(16f, docLeft * scale + offsetX);
                            float screenTop = Math.max(50f, docTop * scale + offsetY);
                            float screenRight = Math.min(viewWidth - 16f, docRight * scale + offsetX);
                            float screenBottom = Math.min(viewHeight - 120f, docBottom * scale + offsetY);

                            RectF screenRect = new RectF(screenLeft, screenTop, screenRight, screenBottom);

                            long now = System.currentTimeMillis();
                            if (documentFirstDetectedTime == 0) {
                                documentFirstDetectedTime = now;
                            }

                            long elapsed = now - documentFirstDetectedTime;

                            runOnUiThread(() -> {
                                if (!isAutoMode || isCapturingOrProcessing) return;

                                if (scanFrame != null) {
                                    scanFrame.setBackground(null);
                                }

                                documentOverlayView.setDetectedBounds(screenRect);

                                if (elapsed >= 1100) {
                                    documentOverlayView.setLabel("📸 מצלם אוטומטית...");
                                    if (tvScanInstruction != null) {
                                        tvScanInstruction.setText("מצלם מסמך...");
                                    }
                                    triggerAutoCapture();
                                } else {
                                    int progressPct = (int) ((elapsed / 1100f) * 100);
                                    documentOverlayView.setLabel("📄 מסמך זוהה • סורק (" + Math.min(99, progressPct) + "%)...");
                                    if (tvScanInstruction != null) {
                                        tvScanInstruction.setText("מסמך זוהה! החזק יציב לצילום אוטומטי");
                                    }
                                }
                            });
                        }
                    } else {
                        consecutiveEmptyFrames++;
                        if (consecutiveEmptyFrames >= 3) {
                            documentFirstDetectedTime = 0;
                            runOnUiThread(() -> {
                                if (isAutoMode && !isCapturingOrProcessing) {
                                    documentOverlayView.clear();
                                    if (scanFrame != null) {
                                        scanFrame.setBackgroundResource(R.drawable.bg_card_white);
                                        scanFrame.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#15FFFFFF")));
                                    }
                                    if (tvScanInstruction != null) {
                                        tvScanInstruction.setText("כוון למסמך לצילום אוטומטי");
                                    }
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "ML Kit text recognition error: " + e.getMessage()))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void triggerAutoCapture() {
        if (isCapturingOrProcessing) return;
        isCapturingOrProcessing = true;
        takePhotoAndProcess();
    }

    private void updateTorchState() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            camera.getCameraControl().enableTorch(flashMode == 1);
        }
    }

    private void takePhotoAndProcess() {
        if (imageCapture == null) {
            isCapturingOrProcessing = false;
            documentFirstDetectedTime = 0;
            // Camera not ready, launch gallery as fallback
            Toast.makeText(this, "המצלמה טרם מוכנה, נא לבחור תמונה מהגלריה", Toast.LENGTH_SHORT).show();
            galleryLauncher.launch("image/*");
            return;
        }

        isCapturingOrProcessing = true;
        if (documentOverlayView != null) {
            documentOverlayView.clear();
        }

        // Visual flash feedback on capture
        View shutter = findViewById(R.id.btnShutter);
        if (shutter != null) {
            shutter.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() -> {
                shutter.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
            }).start();
        }

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap bitmap = imageProxyToBitmap(image);
                image.close();
                if (bitmap != null) {
                    processImageWithAi(bitmap);
                } else {
                    isCapturingOrProcessing = false;
                    documentFirstDetectedTime = 0;
                    Toast.makeText(ScannerActivity.this, "שגיאה בפענוח תמונת המצלמה", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                isCapturingOrProcessing = false;
                documentFirstDetectedTime = 0;
                Log.e(TAG, "Image capture failed: " + exception.getMessage());
                Toast.makeText(ScannerActivity.this, "צילום התמונה נכשל, נסה שוב", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            int rotationDegrees = image.getImageInfo().getRotationDegrees();
            if (rotationDegrees != 0 && bitmap != null) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationDegrees);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error converting imageProxy to bitmap: " + e.getMessage());
            return null;
        }
    }

    private void loadBitmapFromUriAndProcess(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (bitmap != null) {
                processImageWithAi(bitmap);
            } else {
                isCapturingOrProcessing = false;
                documentFirstDetectedTime = 0;
                Toast.makeText(this, "לא ניתן היה לטעון את התמונה שנבחרה", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            isCapturingOrProcessing = false;
            documentFirstDetectedTime = 0;
            Log.e(TAG, "Failed to load image from URI: " + e.getMessage());
            Toast.makeText(this, "שגיאה בטעינת הקובץ מהגלריה", Toast.LENGTH_SHORT).show();
        }
    }

    private void processImageWithAi(Bitmap bitmap) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("✨ סריקה חכמה");
        progress.setMessage("המערכת מנתחת את המסמך ומחלצת כרטיסיות לימוד...");
        progress.setCancelable(false);
        progress.show();

        GeminiStudyService.generateFlashcardsFromBitmap(this, bitmap, new GeminiStudyService.AiStudyCallback() {
            @Override
            public void onSuccess(StudySet studySet) {
                progress.dismiss();

                // Save to repository (and Firestore)
                StudyRepository.getInstance().addStudySet(studySet);

                new AlertDialog.Builder(ScannerActivity.this)
                        .setTitle("🎉 הערכה נוצרה בהצלחה!")
                        .setMessage("חולצו " + studySet.getCardCount() + " כרטיסיות זיכרון:\n\"" +
                                studySet.getTitle() + "\" (" + studySet.getCategory() + ").\n\nהאם תרצה להתחיל לתרגל כעת?")
                        .setPositiveButton("תרגל כעת ✨", (dialog, which) -> {
                            Intent intent = new Intent(ScannerActivity.this, FlashcardsActivity.class);
                            intent.putExtra(FlashcardsActivity.EXTRA_SET_ID, studySet.getId());
                            ViewAnimationUtils.startActivityWithTransition(ScannerActivity.this, intent);
                            finish();
                        })
                        .setNegativeButton("חזרה למסך הראשי", (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }

            @Override
            public void onError(String errorMessage) {
                progress.dismiss();
                isCapturingOrProcessing = false;
                documentFirstDetectedTime = 0;
                showErrorDialog(bitmap, errorMessage);
            }
        });
    }

    private void showErrorDialog(Bitmap pendingBitmap, String errorMessage) {
        new AlertDialog.Builder(this)
                .setTitle("שגיאה בעיבוד המסמך")
                .setMessage(errorMessage + "\n\nהאם ברצונך לנסות שוב?")
                .setPositiveButton("נסה שוב 🔄", (dialog, which) -> {
                    if (pendingBitmap != null) {
                        isCapturingOrProcessing = true;
                        processImageWithAi(pendingBitmap);
                    }
                })
                .setNegativeButton("סגור", (dialog, which) -> {
                    isCapturingOrProcessing = false;
                    documentFirstDetectedTime = 0;
                })
                .setOnCancelListener(dialog -> {
                    isCapturingOrProcessing = false;
                    documentFirstDetectedTime = 0;
                })
                .show();
    }

    @Override
    public void finish() {
        super.finish();
        ViewAnimationUtils.applyActivityCloseTransition(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (laserAnimator != null) {
            laserAnimator.cancel();
        }
        if (analysisExecutor != null) {
            analysisExecutor.shutdown();
        }
        if (textRecognizer != null) {
            textRecognizer.close();
        }
    }
}
