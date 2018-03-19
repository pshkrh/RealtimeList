package com.pshkrh.realtimelist;

import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;

import es.dmoral.toasty.Toasty;

public class SplashActivity extends AppCompatActivity {

    public static final String ANONYMOUS = "Anonymous";
    public static final int RC_SIGN_IN = 1;
    private String mUsername;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mFirebaseAuth = FirebaseAuth.getInstance();

        Handler handler = new Handler();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mAuthStateListener = new FirebaseAuth.AuthStateListener() {
                    @Override
                    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            //Signed in
                            onSignedInInit(user.getDisplayName());
                            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                            intent.putExtra("Username", mUsername);
                            startActivity(intent);
                            finish();
                        } else {
                            //Signed out
                            onSignedOutCleanup();
                            mUsername = ANONYMOUS;
                            startActivityForResult(
                                    AuthUI.getInstance()
                                            .createSignInIntentBuilder()
                                            .setIsSmartLockEnabled(false)
                                            .setLogo(R.mipmap.ic_launcher)
                                            .setAvailableProviders(Arrays.asList(
                                                    new AuthUI.IdpConfig.EmailBuilder().build(),
                                                    new AuthUI.IdpConfig.GoogleBuilder().build()))
                                            .build(),
                                    RC_SIGN_IN);
                        }
                    }
                };
                mFirebaseAuth.addAuthStateListener(mAuthStateListener);
            }
        },1500);
    }

    private void onSignedInInit(String displayName) {
        mUsername = displayName;
    }

    private void onSignedOutCleanup(){
        mUsername = ANONYMOUS;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toasty.success(SplashActivity.this, "Signed in Successfully!", Toast.LENGTH_SHORT, true).show();
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                intent.putExtra("Username", mUsername);
                startActivity(intent);
                finish();
            } else if (resultCode == RESULT_CANCELED) {
                Toasty.warning(SplashActivity.this, "Sign in Cancelled", Toast.LENGTH_SHORT, true).show();
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
