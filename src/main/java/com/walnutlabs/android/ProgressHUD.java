package com.walnutlabs.android;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.graphics.drawable.shapes.ArcShape;
import android.graphics.drawable.shapes.OvalShape;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import tv.inair.airmote.R;

public class ProgressHUD extends Dialog {

    private static final int RADIAL_PROGRESS_SIZE = 80;
    private static final float RADIAL_PROGRESS_STARTING_ANGLE = 90.0f;
    private static final int RADIAL_PROGRESS_ANIMATION_DURATION = 100;  //milliseconds
    private static final float CIRCLE_IN_DEGREES = 360.0f;
    private static final float DIM_WINDOW_PERCENTAGE = 0.33f;
    protected float mProgressAngle;

    public ProgressHUD(Context context) {
        super(context);
    }

    public ProgressHUD(Context context, int theme) {
        super(context, theme);
    }

    public static ProgressHUD show(Context context, String message, String submessage, Style style, boolean cancelable,
                                   OnCancelListener cancelListener) {
        ProgressHUD dialog = new ProgressHUD(context, R.style.ProgressHUD);
        dialog.setTitle("");
        dialog.mProgressAngle = 0.0f;
        dialog.setContentView(R.layout.progress_hud);
        dialog.setupMessage(message);
        dialog.setupSmallerMessage(submessage);
        dialog.setupSpinner(style);

        dialog.setCancelable(cancelable);
        dialog.setOnCancelListener(cancelListener);
        dialog.getWindow().getAttributes().gravity = Gravity.CENTER;
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().setDimAmount(DIM_WINDOW_PERCENTAGE);
        dialog.show();
        return dialog;
    }

    private void setupText(String message, int resId) {
        TextView txt = (TextView) findViewById(resId);
        if (txt != null) {
            if (message == null || message.length() == 0) {
                txt.setVisibility(View.GONE);
            } else {
                txt.setVisibility(View.VISIBLE);
                txt.setText(message);
                txt.invalidate();
            }
        }
    }

    protected void setupMessage(String message) {
        setupText(message, R.id.message);
    }

    protected void setupSmallerMessage(String message) {
        setupText(message, R.id.smaller_message);
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dp, getContext().getResources().getDisplayMetrics());
    }

    protected void setupSpinner(Style style) {
        ImageView imageView = (ImageView) findViewById(R.id.spinnerImageView);
        if (style == Style.Checkmark) {
            imageView.setImageResource(R.drawable.error);
            imageView.setBackgroundDrawable(null);
        } else if (style == Style.Indeterminate) {
            imageView.setImageDrawable(null);
            imageView.setBackgroundResource(R.drawable.spinner);
        } else if (style == Style.RadialProgress) {
            ShapeDrawable shapeDrawable;
            if (mProgressAngle >= CIRCLE_IN_DEGREES) {
                shapeDrawable = new ShapeDrawable(new OvalShape());
            } else {
                shapeDrawable = new ShapeDrawable(new ArcShape(RADIAL_PROGRESS_STARTING_ANGLE, mProgressAngle));
            }

            int size = dp2px(RADIAL_PROGRESS_SIZE);

            shapeDrawable.getPaint().setColor(Color.WHITE);
            shapeDrawable.getPaint().setStyle(Paint.Style.FILL);
            shapeDrawable.getPaint().setAntiAlias(true);
            shapeDrawable.setBounds(0, 0, size, size);
            shapeDrawable.setIntrinsicWidth(size);
            shapeDrawable.setIntrinsicHeight(size);

            Drawable startingDrawable;
            if (imageView.getDrawable() instanceof TransitionDrawable) {
                TransitionDrawable transitionDrawable = (TransitionDrawable) imageView.getDrawable();
                startingDrawable = transitionDrawable.getCurrent();
            } else {
                startingDrawable = imageView.getDrawable();
            }
            if (startingDrawable != null) {
                Drawable backgrounds[] = new Drawable[2];
                backgrounds[0] = startingDrawable;
                backgrounds[1] = shapeDrawable;
                TransitionDrawable crossfader = new TransitionDrawable(backgrounds);
                imageView.setImageDrawable(crossfader);
                crossfader.startTransition(RADIAL_PROGRESS_ANIMATION_DURATION);
            } else {
                imageView.setImageDrawable(shapeDrawable);
            }
            imageView.setBackgroundDrawable(null);
        }

        imageView.invalidate();
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    private void setAnimation(boolean running) {
        ImageView imageView = (ImageView) findViewById(R.id.spinnerImageView);
        AnimationDrawable spinner = (AnimationDrawable) imageView.getBackground();
        if (spinner != null) {
            if (running) {
                spinner.start();
            } else {
                spinner.stop();
            }
        }
    }

    private void startAnimation() {
        setAnimation(true);
    }

    private void stopAnimation() {
        setAnimation(false);
    }

    public void setMessage(String message) {
        setupText(message, R.id.message);
    }

    public void setStyle(Style style) {
        setupSpinner(style);
        if (style == Style.Indeterminate) {
            startAnimation();
        }
    }

    public void setProgress(float percentage) {
        if (percentage < 0.0f) {
            mProgressAngle = 0.0f;
        } else if (percentage > CIRCLE_IN_DEGREES) {
            mProgressAngle = CIRCLE_IN_DEGREES;
        } else {
            mProgressAngle = (CIRCLE_IN_DEGREES * percentage);
        }
        setupSpinner(Style.RadialProgress);
    }

    public enum Style {
        Indeterminate,
        Checkmark,
        RadialProgress
    }
}
