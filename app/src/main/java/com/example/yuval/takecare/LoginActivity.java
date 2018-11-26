package com.example.yuval.takecare;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.drawable.BitmapDrawable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.transition.Explode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.transition.TransitionManager;
import android.support.transition.Fade;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        findViewById(R.id.faded_image).setVisibility(View.GONE);

    }

    public void transition(View view) {
        ImageView faded_background_image = findViewById(R.id.faded_image);
        final ImageView background_image = findViewById(R.id.imageView3);
        faded_background_image.setAlpha(0f);
        faded_background_image.setVisibility(View.VISIBLE);
        faded_background_image.animate().alpha(0.666f).setDuration(100).setListener(null);
        background_image.animate().alpha(0f).setDuration(100).setListener(null);

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(this, R.layout.activity_login2);
        TransitionManager.beginDelayedTransition((ConstraintLayout) findViewById(R.id.login_screen_layout));
        constraintSet.applyTo((ConstraintLayout) findViewById(R.id.login_screen_layout));

    }
}