package com.yalig.snapstudy.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class DocumentOverlayView extends View {

    private RectF currentRect = null;
    private RectF targetRect = null;

    private Paint borderPaint;
    private Paint fillPaint;
    private Paint cornerPaint;
    private Paint badgeBgPaint;
    private Paint textPaint;

    private String label = "📄 מסמך זוהה • סורק...";

    private float cornerLength;
    private float cornerRadius;

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

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        cornerLength = 26f * density;
        cornerRadius = 14f * density;

        int redColor = Color.parseColor("#FF2D55");

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(redColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f * density);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(Color.parseColor("#15FF2D55"));
        fillPaint.setStyle(Paint.Style.FILL);

        cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cornerPaint.setColor(redColor);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(5.5f * density);
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);

        badgeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeBgPaint.setColor(redColor);
        badgeBgPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(12f * density);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setDetectedBounds(@Nullable RectF rect) {
        if (rect == null) {
            targetRect = null;
            currentRect = null;
        } else {
            targetRect = new RectF(rect);
            if (currentRect == null) {
                currentRect = new RectF(rect);
            } else {
                float factor = 0.45f;
                currentRect.left += (targetRect.left - currentRect.left) * factor;
                currentRect.top += (targetRect.top - currentRect.top) * factor;
                currentRect.right += (targetRect.right - currentRect.right) * factor;
                currentRect.bottom += (targetRect.bottom - currentRect.bottom) * factor;
            }
        }
        postInvalidateOnAnimation();
    }

    public void clear() {
        targetRect = null;
        currentRect = null;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (currentRect == null) return;

        // 1. Red fill & border
        canvas.drawRoundRect(currentRect, cornerRadius, cornerRadius, fillPaint);
        canvas.drawRoundRect(currentRect, cornerRadius, cornerRadius, borderPaint);

        // 2. Red corner indicators
        float l = currentRect.left;
        float t = currentRect.top;
        float r = currentRect.right;
        float b = currentRect.bottom;
        float cl = Math.min(cornerLength, Math.min((r - l) / 3f, (b - t) / 3f));

        // Top-left
        canvas.drawLine(l, t + cl, l, t, cornerPaint);
        canvas.drawLine(l, t, l + cl, t, cornerPaint);

        // Top-right
        canvas.drawLine(r - cl, t, r, t, cornerPaint);
        canvas.drawLine(r, t, r, t + cl, cornerPaint);

        // Bottom-left
        canvas.drawLine(l, b - cl, l, b, cornerPaint);
        canvas.drawLine(l, b, l + cl, b, cornerPaint);

        // Bottom-right
        canvas.drawLine(r - cl, b, r, b, cornerPaint);
        canvas.drawLine(r, b, r, b - cl, cornerPaint);

        // 3. Floating indicator badge
        String label = "📄 מסמך זוהה • סורק...";
        float textWidth = textPaint.measureText(label);
        float density = getResources().getDisplayMetrics().density;
        float badgePaddingH = 12f * density;
        float badgeHeight = 26f * density;
        float badgeWidth = textWidth + badgePaddingH * 2f;
        float badgeCenterX = (l + r) / 2f;
        float badgeTop = t - badgeHeight - (8f * density);
        if (badgeTop < 80f * density) {
            badgeTop = t + 12f * density;
        }

        RectF badgeRect = new RectF(
                badgeCenterX - badgeWidth / 2f,
                badgeTop,
                badgeCenterX + badgeWidth / 2f,
                badgeTop + badgeHeight
        );
        canvas.drawRoundRect(badgeRect, badgeHeight / 2f, badgeHeight / 2f, badgeBgPaint);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textBaseline = badgeRect.centerY() - (fm.top + fm.bottom) / 2f;
        canvas.drawText(label, badgeCenterX, textBaseline, textPaint);
    }
}
