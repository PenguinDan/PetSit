package edu.csulb.petsitter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by Daniel on 1/13/2018.
 */

public class SpashScreenActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.SplashTheme);
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(SplashScreenActivity.this , UserAuthenticationContainer.class);
        intent.setAction(UserAuthenticationContainer.LOGIN_USER_ACTION);
        startActivity(intent);
    }

    @Override
    protected void onStop(){
        super.onStop();
        finish();
    }
}
