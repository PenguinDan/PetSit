package edu.csulb.petsitter;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.regions.Regions;
import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Created by Daniel on 1/14/2018.
 */

public class UserAuthenticationContainer extends Activity
        implements LoginFragment.OnButtonClicked{
    //Variables
    private CognitoUserPool cognitoUserPool;
    //Constants
    private final static String TAG = "UserAuthContainer";
    public final static String USER_EMAIL = "UE";
    public final static String USER_PASSWORD = "UP";
    public final static String LOGIN_USER_ACTION = "LUA";
    public final static String CREATE_ACCOUNT_ACTION = "CAA";
    private final LoginFragment loginFragment = new LoginFragment();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_authentication_container);

        //Retrieve instances and make checks
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        AWSMobileClient.getInstance().initialize(this).execute();

        //Create User Pool Object
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        cognitoUserPool = new CognitoUserPool(this, getString(R.string.aws_cognito_user_pool_id)
                , getString(R.string.aws_cognito_client_id), getString(R.string.aws_cognito_client_secret),
                clientConfiguration, Regions.US_EAST_1);

        //Check why this activity was started
        createAndStartFragment(getIntent().getAction());

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        if(loginFragment.isHandlingSignIn()) {
            loginFragment.onActivityResult(requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void createAndStartFragment(String action) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        switch (action) {
            case LOGIN_USER_ACTION: {
                loginFragment.setCognitoUserPool(cognitoUserPool);
                fragmentTransaction.add(R.id.user_authentication_container, loginFragment);
            }
            break;
        }
        fragmentTransaction.commit();
    }

    @Override
    public void buttonClicked(String reason) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        switch(reason) {
            case "create_account":{
                CreateAccountFragment createAccountFragment = new CreateAccountFragment();
                createAccountFragment.setCognitoUserPool(cognitoUserPool);
                createAccountFragment.setCreateAccountClickedListener(new CreateAccountFragment.OnCreateAccountClicked() {
                    @Override
                    public void onClick(String email, String password) {
                        loginFragment.setUserEmailEditText(email);
                        loginFragment.setUserPasswordEditText(password);
                    }
                });
                fragmentTransaction.replace(R.id.user_authentication_container, createAccountFragment);
                fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                fragmentTransaction.addToBackStack(null);
            }
            break;
        }
        fragmentTransaction.commit();
    }
}
