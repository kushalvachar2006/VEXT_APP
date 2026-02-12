package com.example.chat_application;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class TermsAndCondition extends AppCompatActivity {
    TextView t_c,t_c_text,privacytext;
    Button t_c_exit,agreetocontinue;
    CardView t_c_layout;
    View shadow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.terms_and_condition);
        t_c=findViewById(R.id.t_and_c);
        t_c_layout=findViewById(R.id.t_c_layout);
        t_c_exit = findViewById(R.id.exitbtn);
        agreetocontinue = findViewById(R.id.agree_continue);
        t_c_text=findViewById(R.id.t_c_text);
        privacytext=findViewById(R.id.privacy_text);
        shadow = findViewById(R.id.shadowOverlay);

        t_c_exit.setBackground(null);
        String tc=getString(R.string.t_c_conditions_text);
        String privacy=getString(R.string.privacy_condition_text);

        t_c_text.setText(Html.fromHtml(tc,Html.FROM_HTML_MODE_COMPACT));
        privacytext.setText(Html.fromHtml(privacy,Html.FROM_HTML_MODE_COMPACT));

        t_c.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                t_c_layout.setVisibility(View.VISIBLE);
                shadow.setVisibility(View.VISIBLE);
                Animation slidein = AnimationUtils.loadAnimation(TermsAndCondition.this,R.anim.slide_in_bottom);
                t_c_layout.startAnimation(slidein);
            }
        });

        t_c_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation slideOut = AnimationUtils.loadAnimation(TermsAndCondition.this, R.anim.slide_out_bottom);
                t_c_layout.startAnimation(slideOut);
                slideOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        t_c_layout.setVisibility(View.INVISIBLE);
                        shadow.setVisibility(View.INVISIBLE);
                    }
                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
            }
        });

        agreetocontinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TermsAndCondition.this,Login_Page.class);
                startActivity(intent);
                finish();
            }
        });
    }
}