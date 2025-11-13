package com.example.ddquest;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class InitialsAvatarDrawable extends Drawable {

    private final String initials;
    private final int bgColor;
    private final Paint bgPaint;
    private final Paint textPaint;

    public InitialsAvatarDrawable(String initials, int bgColor) {
        this.initials = (initials != null && initials.length() > 2)
                ? initials.substring(0, 2).toUpperCase()
                : (initials != null ? initials.toUpperCase() : "??");

        this.bgColor = bgColor;

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(bgColor);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.width() == 0 || bounds.height() == 0) return;

        float centerX = bounds.centerX();
        float centerY = bounds.centerY();
        float radius = Math.min(bounds.width(), bounds.height()) / 2f;

        canvas.drawCircle(centerX, centerY, radius, bgPaint);

        float textSize = radius * 0.8f;
        textPaint.setTextSize(textSize);

        Rect textBounds = new Rect();
        textPaint.getTextBounds(initials, 0, initials.length(), textBounds);
        float y = centerY - textBounds.exactCenterY();

        canvas.drawText(initials, centerX, y, textPaint);
    }

    @Override public void setAlpha(int alpha) { textPaint.setAlpha(alpha); bgPaint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(@Nullable ColorFilter colorFilter) { textPaint.setColorFilter(colorFilter); bgPaint.setColorFilter(colorFilter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}