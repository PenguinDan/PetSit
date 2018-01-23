package edu.csulb.petsitter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by Daniel on 1/13/2018.
 */

public class DynamicLinkHandlerActivity extends Activity {
    private final String TAG = "DynamicLinkHandler";
    private final String EMAIL_VERIFICATION = "email_verification";
    private final String PASSWORD_RESET = "password_reset";
    private final String USER_SIGN_IN = "user_sign_in";
    private final String USER_SIGN_OUT = "user_sign_out";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        //Get the reason why this activity was started
        switch (getIntent().getData().toString()) {
            case EMAIL_VERIFICATION: {
                Log.d(TAG, "Email Verification");
            }
            break;
            case PASSWORD_RESET: {
                Log.d(TAG, "Password Reset");
            }
            break;
            case USER_SIGN_IN: {
                Log.d(TAG, "User Sign In");
            }
            break;
            case USER_SIGN_OUT: {
                Log.d(TAG, "User Sign Out");
            }
            break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        setVisible(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
