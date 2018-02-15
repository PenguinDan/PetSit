package edu.csulb.petsitter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.StartupAuthResult;
import com.amazonaws.mobile.auth.core.StartupAuthResultHandler;
import com.amazonaws.mobile.auth.facebook.FacebookSignInProvider;
import com.amazonaws.mobile.auth.google.GoogleSignInProvider;
import com.amazonaws.mobile.config.AWSConfiguration;

/**
 * Created by Danie on 1/28/2018.
 */

public class SplashScreenActivity extends Activity {
    //Constants
    private final static String TAG  = "SplashScreenActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.SplashTheme);
        super.onCreate(savedInstanceState);

        AWSConfiguration awsConfiguration = new AWSConfiguration(getApplicationContext());

        //Check if the Identity Manager has been created
        if(IdentityManager.getDefaultIdentityManager() == null) {
            IdentityManager identityManager = new IdentityManager(getApplicationContext(),
                    awsConfiguration);
            IdentityManager.setDefaultIdentityManager(identityManager);
        }

        //Configure Facebook Sign In
        FacebookSignInProvider.setPermissions("public_profile", "email");
        IdentityManager.getDefaultIdentityManager().addSignInProvider(FacebookSignInProvider.class);
        //Configure Google Sign In
        GoogleSignInProvider.setPermissions("profile", "email", "openid");
        IdentityManager.getDefaultIdentityManager().addSignInProvider(GoogleSignInProvider.class);

        //Resume the Identity Manager session if any
        IdentityManager.getDefaultIdentityManager().resumeSession(this, new StartupAuthResultHandler() {
            @Override
            public void onComplete(final StartupAuthResult authResults) {
                if(authResults.isUserSignedIn()) {
                    Log.d(TAG, "onComplete : User is signed in with " +
                    authResults.getIdentityManager().getCurrentIdentityProvider().getDisplayName());
                } else {
                    Log.d(TAG, "onComplete : User is not signed in.");
                }
            }
        });

        Intent intent = new Intent(this, MainActivityContainer.class);
        intent.setAction(UserAuthenticationContainer.LOGIN_USER_ACTION);
        startActivity(intent);
    }

    @Override
    protected void onStop(){
        super.onStop();
        finish();
    }
}
