package com.yalig.snapstudy.ai;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Base64;

import com.yalig.snapstudy.models.Flashcard;
import com.yalig.snapstudy.models.StudySet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * שולחת תמונה של דף ל-Gemini ומקבלת בחזרה ערכת כרטיסיות (שאלות ותשובות).
 * הבקשה נשלחת ב-Thread נפרד, כי אסור לגשת לאינטרנט מה-Thread הראשי של המסך.
 */
public class GeminiStudyService {

    // ממשק שבעזרתו מחזירים את התוצאה למסך
    public interface AiStudyCallback {
        void onSuccess(StudySet studySet);
        void onError(String message);
    }

    private static final String PROMPT =
            "התבונן בתמונה (דף סיכום, ספר או מחברת) וצור ממנה כרטיסיות לימוד בעברית.\n" +
            "החזר רק JSON במבנה הזה, בלי שום טקסט נוסף:\n" +
            "{\"title\": \"כותרת קצרה לנושא\", \"category\": \"מקצוע הלימוד\", " +
            "\"flashcards\": [{\"question\": \"שאלה\", \"answer\": \"תשובה\", \"topic\": \"תת נושא\"}]}\n" +
            "צור בין 3 ל-10 כרטיסיות לפי כמות החומר בתמונה.";

    public static void generateFlashcards(Activity activity, Bitmap bitmap, AiStudyCallback callback) {
        new Thread(() -> {
            try {
                // 1. הקטנת התמונה והמרה לטקסט Base64
                String imageBase64 = bitmapToBase64(scaleBitmap(bitmap, 1280));

                // 2. בניית הבקשה ושליחה ל-Gemini
                String requestBody = buildRequest(imageBase64).toString();
                String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                        + GeminiConfig.MODEL + ":generateContent?key=" + GeminiConfig.API_KEY;
                String response = sendPost(url, requestBody);

                // 3. פענוח התשובה לערכת לימוד
                StudySet set = parseResponse(response);
                activity.runOnUiThread(() -> callback.onSuccess(set));
            } catch (Exception e) {
                activity.runOnUiThread(() -> callback.onError(
                        "לא הצלחנו ליצור כרטיסיות מהתמונה. בדוק את החיבור לאינטרנט ונסה שוב."));
            }
        }).start();
    }

    // בניית ה-JSON של הבקשה: טקסט ההנחיה + התמונה
    private static JSONObject buildRequest(String imageBase64) throws Exception {
        JSONObject textPart = new JSONObject();
        textPart.put("text", PROMPT);

        JSONObject inlineData = new JSONObject();
        inlineData.put("mime_type", "image/jpeg");
        inlineData.put("data", imageBase64);
        JSONObject imagePart = new JSONObject();
        imagePart.put("inline_data", inlineData);

        JSONArray parts = new JSONArray();
        parts.put(textPart);
        parts.put(imagePart);

        JSONObject content = new JSONObject();
        content.put("parts", parts);
        JSONArray contents = new JSONArray();
        contents.put(content);

        // מבקשים מהמודל להחזיר JSON בלבד
        JSONObject config = new JSONObject();
        config.put("response_mime_type", "application/json");

        JSONObject request = new JSONObject();
        request.put("contents", contents);
        request.put("generationConfig", config);
        return request;
    }

    // שליחת בקשת POST וקריאת התשובה מהשרת
    private static String sendPost(String urlString, String body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setDoOutput(true);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(90000);

        OutputStream out = connection.getOutputStream();
        out.write(body.getBytes(StandardCharsets.UTF_8));
        out.close();

        if (connection.getResponseCode() != 200) {
            throw new Exception("Server error: " + connection.getResponseCode());
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        reader.close();
        connection.disconnect();
        return result.toString();
    }

    // התשובה של Gemini נמצאת ב: candidates[0].content.parts[0].text
    private static StudySet parseResponse(String response) throws Exception {
        String text = new JSONObject(response)
                .getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts").getJSONObject(0)
                .getString("text");

        JSONObject data = new JSONObject(text);
        String title = data.optString("title", "ערכת לימוד חדשה");
        String category = data.optString("category", "כללי");
        JSONArray cards = data.getJSONArray("flashcards");

        String setId = "set_" + System.currentTimeMillis();
        StudySet set = new StudySet(setId, title, category, "נוצר מסריקה", 0);
        for (int i = 0; i < cards.length(); i++) {
            JSONObject card = cards.getJSONObject(i);
            set.addFlashcard(new Flashcard(
                    setId + "_" + i,
                    card.getString("question"),
                    card.getString("answer"),
                    card.optString("topic", category)));
        }
        if (set.getFlashcards().isEmpty()) {
            throw new Exception("No flashcards in response");
        }
        return set;
    }

    // הקטנת התמונה כך שהצלע הארוכה תהיה לכל היותר maxSize פיקסלים
    private static Bitmap scaleBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }
        float scale = (float) maxSize / Math.max(width, height);
        return Bitmap.createScaledBitmap(bitmap, Math.round(width * scale), Math.round(height * scale), true);
    }

    // המרת התמונה ל-JPEG ואז לטקסט Base64 (כך שולחים תמונה בתוך JSON)
    private static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);
    }
}
