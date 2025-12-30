package com.example.ddquest;

import android.animation.Animator;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;

public class SplashActivity extends AppCompatActivity {

    private LottieAnimationView animationView;
    private boolean isNavigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        animationView = findViewById(R.id.animation_view);
        if (animationView == null) {
            // Fallback if view not found – navigate immediately
            goToNext();
            return;
        }

        // Optional scale adjustment if needed (animation is now fixed-size; adjust 1.0f for no additional scaling)
        float scale = 1.0f;
        animationView.setScaleX(scale);
        animationView.setScaleY(scale);

        // Improve sharpness (reduces any blur)
        animationView.setRenderMode(com.airbnb.lottie.RenderMode.HARDWARE);

        // Animation auto-plays via XML; add listener for end navigation
        animationView.addAnimatorListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator animation) {}
            @Override public void onAnimationCancel(Animator animation) { goToNext(); }
            @Override public void onAnimationRepeat(Animator animation) {}
            @Override public void onAnimationEnd(Animator animation) { goToNext(); }
        });
    }

    private void goToNext() {
        if (!isNavigated && !isFinishing() && animationView != null) {
            isNavigated = true;
            Intent intent = new Intent(SplashActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (animationView != null) {
            animationView.pauseAnimation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (animationView != null && !animationView.isAnimating()) {
            animationView.resumeAnimation();
        }
    }
}