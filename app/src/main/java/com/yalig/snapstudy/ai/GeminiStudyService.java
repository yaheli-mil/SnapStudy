package com.yalig.snapstudy.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.yalig.snapstudy.models.Flashcard;
import com.yalig.snapstudy.models.StudySet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * שירות עיבוד ובינה מלאכותית מול מנוע Google Gemini (גרסת gemini-2.5-flash).
 * מחלקה זו אחראית על:
 * 1. קבלת תמונת מסמך (Bitmap) ממצלמת האפליקציה או מהגלריה.
 * 2. כיווץ והמרת התמונה לקידוד Base64 JPEG כדי לייעל את תעבורת הרשת.
 * 3. הרכבת בקשת JSON מובנית הכוללת הנחיה פדגוגית (Prompt) ואת המידע הוויזואלי.
 * 4. ביצוע קריאת HTTP POST אסינכרונית בעזרת ספריית OkHttpClient.
 * 5. פענוח תשובת ה-JSON המוחזרת מהמודל והמרתה לאובייקט StudySet המכיל כרטיסיות לימוד.
 */
public class GeminiStudyService {

    // תגית לרישום לוגים במערכת לצורכי ניפוי שגיאות (Debug)
    private static final String TAG = "GeminiStudyService";

    // שם המודל המהיר והרב-מודאלי שבו אנו משתמשים לחילוץ טקסט מתמונות
    private static final String GEMINI_MODEL = "gemini-2.5-flash";

    // כתובת הבסיס לקריאת ה-REST API של שירותי Google AI
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/" + GEMINI_MODEL + ":generateContent?key=";

    /**
     * ממשק Callback להחזרת תוצאות הפענוח למסך הקורא (ScannerActivity)
     */
    public interface AiStudyCallback {
        // מופעל בהצלחה בעת סיום פענוח הערכה
        void onSuccess(StudySet studySet);
        // מופעל במקרה של שגיאת רשת, שגיאת מפתח או פענוח
        void onError(String errorMessage);
    }

    // לקוח HTTP מוגדר עם זמני המתנה (Timeouts) מתאימים להעלאת תמונות ועיבוד ענן
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    // Handler להחזרת תוצאות ה-Callback ישירות אל ה-UI Thread הראשי של אנדרואיד
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * הפונקציה המרכזית: מקבלת Bitmap של מסמך ושולחת אותו לניתוח בענן.
     * @param context הקשר המערכת לקריאת הגדרות ומפתח ה-API.
     * @param bitmap תמונת המסמך או המחברת שנלכדה במצלמה.
     * @param callback ממשק המאזין לקבלת התוצאה.
     */
    public static void generateFlashcardsFromBitmap(Context context, Bitmap bitmap, AiStudyCallback callback) {
        // שלב 1: שליפת מפתח ה-API מהקונפיגורציה
        String apiKey = GeminiConfig.getApiKey(context);

        if (apiKey == null || apiKey.trim().isEmpty()) {
            mainHandler.post(() -> callback.onError("NO_KEY"));
            return;
        }

        // שלב 2: הקטנת ממדי התמונה (עד 1280 פיקסלים) כדי לחסוך ברוחב פס תוך שמירה על קריאות מלאה של הטקסט
        Bitmap scaledBitmap = scaleBitmapIfNeeded(bitmap, 1280);

        // שלב 3: המרת התמונה למחרוזת Base64 בפורמט JPEG
        String base64Jpeg = bitmapToBase64Jpeg(scaledBitmap);

        try {
            // שלב 4: בניית גוף הבקשה בפורמט JSON לפי המפרט הרשמי של Gemini API
            JSONObject requestJson = new JSONObject();

            JSONArray contents = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray parts = new JSONArray();

            // חלק 1 בבקשה: הנחיה פדגוגית מפורטת למודל (Prompt) המגדירה את הפורמט הנדרש
            JSONObject textPart = new JSONObject();
            textPart.put("text",
                    "אתה מורה פרטי מומחה ופדגוג מקצועי. התבונן היטב בתמונה המצורפת (דף סיכום, ספר, מחברת, מצגת או צילום לוח).\n" +
                    "קרא את כל הטקסט, ההגדרות, המושגים והנוסחאות שבתמונה.\n" +
                    "צור ערכת כרטיסיות זיכרון (Flashcards) איכותיות בעברית בהירה ומדויקת על בסיס התוכן המדויק של התמונה.\n" +
                    "החזר אך ורק מבנה JSON תקין לפי הסכמה הבאה, ללא שום טקסט נוסף לפני או אחרי:\n" +
                    "{\n" +
                    "  \"title\": \"כותרת קצרה וקולעת לנושא הלימוד שבתמונה\",\n" +
                    "  \"category\": \"מקצוע הלימוד (לדוגמה: היסטוריה, אזרחות, פיזיקה, ביולוגיה, ספרות, מתמטיקה, אנגלית, מדעי המחשב)\",\n" +
                    "  \"description\": \"תיאור קצר של תוכן הערכה שנלמד מהתמונה\",\n" +
                    "  \"flashcards\": [\n" +
                    "    {\n" +
                    "      \"question\": \"שאלה ישירה וממוקדת\",\n" +
                    "      \"answer\": \"תשובה מלאה, מדויקת ומלמדת\",\n" +
                    "      \"topic\": \"תת נושא\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n" +
                    "חלץ בין 3 ל-10 כרטיסיות ממוקדות בהתאם לכמות החומר שבתמונה.");
            parts.put(textPart);

            // חלק 2 בבקשה: נתוני התמונה המקודדים (Inline Data)
            JSONObject imagePart = new JSONObject();
            JSONObject inlineData = new JSONObject();
            inlineData.put("mime_type", "image/jpeg");
            inlineData.put("data", base64Jpeg);
            imagePart.put("inline_data", inlineData);
            parts.put(imagePart);

            contentObj.put("parts", parts);
            contents.put(contentObj);
            requestJson.put("contents", contents);

            // הגדרות יצירה (Generation Config) - אכיפת פלט בפורמט JSON בלבד וטמפרטורה נמוכה לתשובות מדויקות
            JSONObject generationConfig = new JSONObject();
            generationConfig.put("response_mime_type", "application/json");
            generationConfig.put("temperature", 0.2);
            requestJson.put("generationConfig", generationConfig);

            // בניית כתובת ה-URL המלאה עם המפתח
            String url = BASE_URL + apiKey;
            RequestBody body = RequestBody.create(
                    requestJson.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            // שלב 5: ביצוע הקריאה האסינכרונית ברקע
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // טיפול בשגיאת רשת (למשל אינטרנט מנותק)
                    Log.e(TAG, "Network call failed: " + e.getMessage());
                    mainHandler.post(() -> callback.onError("שגיאת תקשורת עם המערכת. יש לוודא חיבור לאינטרנט."));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    // בדיקת קוד התגובה של השרת
                    if (!response.isSuccessful()) {
                        String errBody = response.body() != null ? response.body().string() : "";
                        Log.e(TAG, "API error (" + response.code() + "): " + errBody);
                        String userMsg;
                        if (response.code() == 429) {
                            userMsg = "עומס זמני על המערכת. נסה שוב בעוד מספר רגעים.";
                        } else {
                            userMsg = "אירעה שגיאה בעיבוד המסמך במערכת (" + response.code() + "). נסה שוב.";
                        }
                        mainHandler.post(() -> callback.onError(userMsg));
                        return;
                    }

                    try {
                        // שלב 6: חילוץ התשובה הטקסטואלית מגוף ה-JSON המוחזר
                        String respString = response.body().string();
                        JSONObject respJson = new JSONObject(respString);
                        JSONArray candidates = respJson.optJSONArray("candidates");
                        if (candidates == null || candidates.length() == 0) {
                            mainHandler.post(() -> callback.onError("המערכת לא הצליחה לקרוא מידע מתמונה זו. נסה לצלם ברור יותר."));
                            return;
                        }

                        JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
                        JSONArray respParts = content != null ? content.optJSONArray("parts") : null;
                        if (respParts == null || respParts.length() == 0) {
                            mainHandler.post(() -> callback.onError("המערכת לא מצאה כרטיסיות לימוד בתמונה."));
                            return;
                        }

                        String jsonText = respParts.getJSONObject(0).optString("text", "");
                        
                        // שלב 7: פענוח מחרוזת ה-JSON לערכת לימוד מוכנה
                        StudySet studySet = parseStudySetFromJson(jsonText);
                        mainHandler.post(() -> callback.onSuccess(studySet));

                    } catch (Exception ex) {
                        Log.e(TAG, "Failed to parse response: " + ex.getMessage(), ex);
                        mainHandler.post(() -> callback.onError("שגיאה בפענוח נתוני המערכת. נסה שוב."));
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error preparing request: " + e.getMessage(), e);
            mainHandler.post(() -> callback.onError("שגיאה בהכנת הבקשה: " + e.getMessage()));
        }
    }

    /**
     * מפענחת את מחרוזת ה-JSON שהתקבלה מהמודל ובונה ממנה אובייקט מסוג StudySet.
     * @param jsonString טקסט התשובה הגולמי.
     * @return אובייקט StudySet מאוכלס בכרטיסיות.
     */
    private static StudySet parseStudySetFromJson(String jsonString) throws Exception {
        // ניקוי תגיות Markdown (במידה והמודל עטף את הפלט ב-```json)
        String clean = jsonString.trim();
        if (clean.startsWith("```json")) {
            clean = clean.substring(7);
        }
        if (clean.startsWith("```")) {
            clean = clean.substring(3);
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        clean = clean.trim();

        JSONObject obj = new JSONObject(clean);
        String title = obj.optString("title", "ערכת לימוד חדשה");
        String category = obj.optString("category", "כללי");
        String description = obj.optString("description", "נוצר מסריקה חכמה במערכת");
        if (description.length() > 50) {
            description = description.substring(0, 47) + "...";
        }

        JSONArray flashcardsArr = obj.optJSONArray("flashcards");
        int count = flashcardsArr != null ? flashcardsArr.length() : 0;
        
        // יצירת מזהה ייחודי עבור ערכת הלימוד החדשה
        String setId = "set_" + System.currentTimeMillis();

        StudySet set = new StudySet(setId, title, category, description + " • " + count + " כרטיסיות", 0);

        // מעבר על מערך הכרטיסיות והוספתן לערכה
        if (flashcardsArr != null) {
            for (int i = 0; i < flashcardsArr.length(); i++) {
                JSONObject fcObj = flashcardsArr.getJSONObject(i);
                String q = fcObj.optString("question", "").trim();
                String a = fcObj.optString("answer", "").trim();
                String topic = fcObj.optString("topic", category).trim();
                if (!q.isEmpty() && !a.isEmpty()) {
                    set.addFlashcard(new Flashcard("fc_" + setId + "_" + (i + 1), q, a, topic));
                }
            }
        }

        // גיבוי למקרה שהערכה נוצרה ללא כרטיסיות
        if (set.getFlashcards().isEmpty()) {
            set.addFlashcard(new Flashcard("fc_default_1", "מהו הרעיון המרכזי שנלמד במסמך זה?", "המסמך מציג מושגי יסוד ועיקרי החומר כפי שנסרקו.", category));
        }

        return set;
    }

    /**
     * פונקציית עזר לשינוי גודל תמונה (Scaling) תוך שמירה מדויקת על יחס הגובה-רוחב (Aspect Ratio).
     * מטרתה למנוע שליחת קבצים ענקיים לרשת ולמנוע בעיות זיכרון (OutOfMemoryError).
     * @param original התמונה המקורית.
     * @param maxDimension הגודל המקסימלי בפיקסלים לצלע הארוכה ביותר.
     * @return תמונה מוקטנת וחדה.
     */
    private static Bitmap scaleBitmapIfNeeded(Bitmap original, int maxDimension) {
        int width = original.getWidth();
        int height = original.getHeight();

        if (width <= maxDimension && height <= maxDimension) {
            return original;
        }

        float ratio = (float) width / (float) height;
        int newWidth, newHeight;
        if (width > height) {
            newWidth = maxDimension;
            newHeight = Math.round(maxDimension / ratio);
        } else {
            newHeight = maxDimension;
            newWidth = Math.round(maxDimension * ratio);
        }

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }

    /**
     * ממירה Bitmap של תמונה למחרוזת בינארית בקידוד Base64 עם דחיסת JPEG.
     * @param bitmap התמונה לדחיסה והמרה.
     * @return מחרוזת Base64 המוכנה להטמעה ב-JSON.
     */
    private static String bitmapToBase64Jpeg(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }
}

