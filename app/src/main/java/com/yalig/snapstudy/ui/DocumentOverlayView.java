package com.yalig.snapstudy.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * רכיב תצוגה גרפי מותאם אישית (Custom View) המצייר שכבת חיווי דינמית על גבי מסך הסורק.
 * מחלקה זו יורשת ישירות ממחלקת View ודורסת את מתודת onDraw לצורך ציור ידני על גבי ה-Canvas.
 * תפקידיה:
 * 1. ציור מסגרת חכמה סביב גבולות המסמך שזוהה על ידי מודל הראייה הממוחשבת (ML Kit).
 * 2. החלקת התנועה (Smoothing / Interpolation) של המסגרת בין פריימים רצופים כדי למנוע רעידות.
 * 3. הדגשת ארבע פינות המסגרת בעזרת קווים עבים ומעוגלים.
 * 4. ציור תגית חיווי צפה (Floating Badge) המציגה את מצב הסריקה ואחוזי היציבות.
 */
public class DocumentOverlayView extends View {

    // גבולות המסגרת הנוכחית המוצגת על המסך (כולל החלקה)
    private RectF currentRect = null;
    // גבולות המסגרת המקורית שהתקבלה מזיהוי ה-ML Kit
    private RectF targetRect = null;

    // כלי הציור הגרפיים (Paint objects) להגדרת צבעים, עוביים וסגנונות
    private Paint borderPaint;    // קו מתאר המסגרת
    private Paint fillPaint;      // מילוי שקוף-למחצה של המסמך
    private Paint cornerPaint;    // הדגשת ארבע הפינות
    private Paint badgeBgPaint;   // רקע התגית הצפה
    private Paint textPaint;      // טקסט החיווי בתוך התגית

    // טקסט החיווי הנוכחי המוצג בתגית הצפה
    private String label = "📄 מסמך זוהה • סורק...";

    // ממדי הפינות והעיגול מחושבים לפי צפיפות המסך (density)
    private float cornerLength;
    private float cornerRadius;

    /**
     * מעדכנת את טקסט החיווי וגורמת לציור מחדש של המסך.
     * @param label הטקסט החדש להצגה.
     */
    public void setLabel(String label) {
        this.label = label;
        postInvalidate();
    }

    public DocumentOverlayView(Context context) {
        super(context);
        init();
    }

    public DocumentOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DocumentOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * אתחול כלי הציור והגדרת הפרמטרים הגרפיים בהתאם לרזולוציית המסך.
     */
    private void init() {
        float density = getResources().getDisplayMetrics().density;
        cornerLength = 26f * density;
        cornerRadius = 14f * density;

        // צבע אדום-ניאון מודרני להדגשת זיהוי המסמך
        int redColor = Color.parseColor("#FF2D55");

        // 1. קו מתאר למסגרת
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(redColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f * density);

        // 2. מילוי פנימי שקוף-למחצה (8% אטימות) למסמך
        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(Color.parseColor("#15FF2D55"));
        fillPaint.setStyle(Paint.Style.FILL);

        // 3. הדגשת פינות המסגרת בקווים מעוגלים ועבים
        cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cornerPaint.setColor(redColor);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(5.5f * density);
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);

        // 4. רקע התגית הצפה
        badgeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeBgPaint.setColor(redColor);
        badgeBgPaint.setStyle(Paint.Style.FILL);

        // 5. טקסט התגית הצפה
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(12f * density);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * מעדכנת את גבולות המסמך המזוהה ומפעילה אינטרפולציה להחלקה חזותית.
     * @param rect מלבן הגבולות החדש, או null אם לא זוהה מסמך בפריים זה.
     */
    public void setDetectedBounds(@Nullable RectF rect) {
        if (rect == null) {
            targetRect = null;
            currentRect = null;
        } else {
            targetRect = new RectF(rect);
            if (currentRect == null) {
                currentRect = new RectF(rect);
            } else {
                // מנגנון החלקה (LERP / Linear Interpolation):
                // המסגרת נעה ב-45% מהמרחק ליעד בכל פריים, ליצירת תנועה רציפה וחלקה
                float factor = 0.45f;
                currentRect.left += (targetRect.left - currentRect.left) * factor;
                currentRect.top += (targetRect.top - currentRect.top) * factor;
                currentRect.right += (targetRect.right - currentRect.right) * factor;
                currentRect.bottom += (targetRect.bottom - currentRect.bottom) * factor;
            }
        }
        postInvalidateOnAnimation();
    }

    /**
     * מאפסת את המסגרת ומנקה את המסך מציורים.
     */
    public void clear() {
        targetRect = null;
        currentRect = null;
        postInvalidate();
    }

    /**
     * פונקציית הציור הראשית על גבי ה-Canvas של ה-View.
     * מופעלת אוטומטית על ידי מערכת ה-UI של Android בכל פעם שנדרש רענון גרפי.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // אם אין מסמך מזוהה, אין צורך לצייר
        if (currentRect == null) return;

        // שלב 1: ציור מילוי שקוף ומסגרת מעוגלת סביב המסמך
        canvas.drawRoundRect(currentRect, cornerRadius, cornerRadius, fillPaint);
        canvas.drawRoundRect(currentRect, cornerRadius, cornerRadius, borderPaint);

        // שלב 2: חישוב וציור ארבע הפינות המודגשות (Corner Brackets)
        float l = currentRect.left;
        float t = currentRect.top;
        float r = currentRect.right;
        float b = currentRect.bottom;
        float cl = Math.min(cornerLength, Math.min((r - l) / 3f, (b - t) / 3f));

        // פינה עליונה-שמאלית
        canvas.drawLine(l, t + cl, l, t, cornerPaint);
        canvas.drawLine(l, t, l + cl, t, cornerPaint);

        // פינה עליונה-ימנית
        canvas.drawLine(r - cl, t, r, t, cornerPaint);
        canvas.drawLine(r, t, r, t + cl, cornerPaint);

        // פינה תחתונה-שמאלית
        canvas.drawLine(l, b - cl, l, b, cornerPaint);
        canvas.drawLine(l, b, l + cl, b, cornerPaint);

        // פינה תחתונה-ימנית
        canvas.drawLine(r - cl, b, r, b, cornerPaint);
        canvas.drawLine(r, b, r, b - cl, cornerPaint);

        // שלב 3: ציור תגית חיווי מרחפת (Floating Badge) מעל המסגרת
        String currentLabelText = this.label != null ? this.label : "📄 מסמך זוהה • סורק...";
        float textWidth = textPaint.measureText(currentLabelText);
        float density = getResources().getDisplayMetrics().density;
        float badgePaddingH = 12f * density;
        float badgeHeight = 26f * density;
        float badgeWidth = textWidth + badgePaddingH * 2f;
        float badgeCenterX = (l + r) / 2f;
        float badgeTop = t - badgeHeight - (8f * density);

        // אם המסגרת קרובה מדי לקצה העליון של המסך, נציג את התגית בחלק הפנימי העליון
        if (badgeTop < 80f * density) {
            badgeTop = t + 12f * density;
        }

        // מלבן הרקע של התגית בצורת קפסולה מעוגלת
        RectF badgeRect = new RectF(
                badgeCenterX - badgeWidth / 2f,
                badgeTop,
                badgeCenterX + badgeWidth / 2f,
                badgeTop + badgeHeight
        );
        canvas.drawRoundRect(badgeRect, badgeHeight / 2f, badgeHeight / 2f, badgeBgPaint);

        // חישוב גובה קו הבסיס (Baseline) למרכוז אנכי מושלם של הטקסט
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textBaseline = badgeRect.centerY() - (fm.top + fm.bottom) / 2f;
        canvas.drawText(currentLabelText, badgeCenterX, textBaseline, textPaint);
    }
}

