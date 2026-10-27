package com.kaam.app;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small UI helpers — colors, dp, rounded backgrounds, text views. */
public final class Ui {

    public static final int BG = 0xFF0C0C14;
    public static final int SURFACE = 0xFF16161F;
    public static final int SURFACE2 = 0xFF1D1D29;
    public static final int TEXT = 0xFFF2F2F7;
    public static final int MUTED = 0xFF9A9AAD;
    public static final int FAINT = 0xFF6C6C80;
    public static final int PRIMARY = 0xFF6366F1;
    public static final int PRIMARY2 = 0xFFA855F7;
    public static final int SUCCESS = 0xFF34D399;
    public static final int DANGER = 0xFFF87171;
    public static final int WARNING = 0xFFFBBF24;
    public static final int BORDER = 0xFF262633;
    public static final int INPUT_BG = 0xFF12121C;

    private Ui() {
    }

    public static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
    }

    public static TextView text(Context context, CharSequence value, float sizeSp, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return view;
    }

    public static GradientDrawable rounded(int color, Context context, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    public static GradientDrawable roundedStroke(int color, Context context, float radiusDp, int strokeColor) {
        GradientDrawable drawable = rounded(color, context, radiusDp);
        drawable.setStroke(dp(context, 1), strokeColor);
        return drawable;
    }

    public static GradientDrawable gradient(int startColor, int endColor, Context context, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{startColor, endColor});
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    public static LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }
}
