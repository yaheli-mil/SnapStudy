package com.yalig.snapstudy.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * View שציירתי בעצמי (יורש מ-View):
 * מציג את התמונה שצולמה, ומצייר מסגרת אדומה סביב האזור שבו ML Kit זיהה טקסט.
 */
public class DocumentOverlayView extends View {

    private Bitmap photo;        // התמונה שצולמה או נבחרה מהגלריה
    private Rect textBounds;     // האזור שבו זוהה טקסט (בקואורדינטות של התמונה)
    private Paint framePaint;

    public DocumentOverlayView(Context context) {
        super(context);
        init();
    }

    public DocumentOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        framePaint = new Paint();
        framePaint.setColor(Color.RED);
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(8);
    }

    public void setPhoto(Bitmap photo) {
        this.photo = photo;
        this.textBounds = null;
        invalidate();   // מבקש לצייר את ה-View מחדש
    }

    public void setTextBounds(Rect textBounds) {
        this.textBounds = textBounds;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (photo == null) {
            return;
        }

        // מחשבים כמה להקטין את התמונה כדי שתיכנס כולה ל-View, וממרכזים אותה
        float scale = Math.min((float) getWidth() / photo.getWidth(),
                (float) getHeight() / photo.getHeight());
        float left = (getWidth() - photo.getWidth() * scale) / 2;
        float top = (getHeight() - photo.getHeight() * scale) / 2;

        RectF photoArea = new RectF(left, top,
                left + photo.getWidth() * scale, top + photo.getHeight() * scale);
        canvas.drawBitmap(photo, null, photoArea, null);

        // המסגרת סביב הטקסט, באותו יחס הקטנה של התמונה
        if (textBounds != null) {
            canvas.drawRect(
                    left + textBounds.left * scale,
                    top + textBounds.top * scale,
                    left + textBounds.right * scale,
                    top + textBounds.bottom * scale,
                    framePaint);
        }
    }
}
