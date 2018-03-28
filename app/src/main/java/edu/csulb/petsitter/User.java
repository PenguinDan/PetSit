package edu.csulb.petsitter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.tokens.CognitoAccessToken;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.tokens.CognitoRefreshToken;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import static edu.csulb.petsitter.AuthHelper.COGNITO_PROVIDER;
import static edu.csulb.petsitter.AuthHelper.FACEBOOK_PROVIDER;
import static edu.csulb.petsitter.AuthHelper.GOOGLE_PROVIDER;

/**
 * Created by Daniel on 3/16/2018.
 */

//A thread, reflection, and serializable safe Singleton class
public class User implements Serializable {

    //Forces userInstance to be thread safe, all the write will happen on this instance
    //before being read from
    private static volatile User userInstace;
    //User Variables
    private static String signInProvider;
    private static String email;
    private static String name;
    private static String googleAccessToken;
    private static String facebookAccessToken;
    private static CognitoAccessToken cognitoAccessToken;
    private static CognitoRefreshToken cognitoRefreshToken;
    //Constants
    private final static String TAG = User.class.getSimpleName();
    //Public Constants
    public final static String USER_PREFERENCES = "user";
    public final static String USER_SIGN_IN_PROVIDER = "provider";


    /**
     * Does not allow the developer to create a User object through the constructor
     */
    private User() {
        //Prevent instantiation from the reflection API
        if (userInstace != null) {
            throw new RuntimeException("Must use getInstance() to generate this object");
        }
    }

    /**
     * Automatically looks into the shared preferences and builds a User object
     * based on the current sign in provider
     *
     * @return
     */
    public static User getInstance(Context context) {
        //Create a new user object if there is no current User object
        if (userInstace == null) {
            synchronized (User.class) {
                if (userInstace == null) {
                    userInstace = new User();
                    retrieveUserInformation(context);
                }
            }
        }
        return userInstace;
    }

    /**
     * The user's name retrieved from Sign In Providers
     *
     * @return The user's name
     */
    public String getName() {
        return name;
    }

    /**
     * The user's email retrieved from Sign In Providers
     *
     * @return The user's email
     */
    public String getEmail() {
        return email;
    }

    /**
     * The user's sign in provider
     *
     * @return The sign in provider that the user is currently signed in with
     */
    public String getSignInProvider() {
        return signInProvider;
    }

    private static void retrieveUserInformation(final Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(USER_PREFERENCES, Context.MODE_PRIVATE);
        String signInProvider = sharedPreferences.getString(USER_SIGN_IN_PROVIDER, null);
        if (signInProvider == null) {
            throw new RuntimeException("Sign in provider is null, user is not signed in");
        }
        switch (signInProvider) {
            case GOOGLE_PROVIDER: {
                Log.d(TAG, "retrieveUserInformation: Retrieving user's google information");
                retrieveGoogleInformation(context);
            }
            break;
            case FACEBOOK_PROVIDER: {
                Log.d(TAG, "retrieveUserInformation: Retrieving user's facebook information");
                retrieveFacebookInformation();
            }
            break;
            case COGNITO_PROVIDER: {
                Log.d(TAG, "retrieveUserInformation: Retrieveing user's cognito information");
                retrieveCognitoInformation(context);
            }
            break;
        }

    }

    /**
     * Retrieve's the user's google information
     *
     * @param context The activity where the User object is created
     */
    private static void retrieveGoogleInformation(Context context) {
        GoogleSignInAccount googleAccount = GoogleSignIn.getLastSignedInAccount(context);
        email = googleAccount.getEmail();
        name = googleAccount.getDisplayName();
        signInProvider = GOOGLE_PROVIDER;
        Log.d(TAG, "retrieveGoogleInformation: Finished retrieving Google information");
        getGoogleAccessToken(context);
    }

    /**
     * Retrieve's the user's facebook information
     */
    private static void retrieveFacebookInformation() {
        AccessToken facebookAccessToken = AccessToken.getCurrentAccessToken();
        Log.d(TAG, "retrieveFacebookInformation: userID = " + facebookAccessToken.getUserId());
        GraphRequest request = GraphRequest.newMeRequest(
                facebookAccessToken,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        try {
                            name = object.getString("name");
                            email = object.getString("email");
                            signInProvider = FACEBOOK_PROVIDER;
                            Log.d(TAG, "retrieveFacebookInformation: onCompleted: Finished retrieving Facebook information");
                            //Retrieve the facebook access tokens
                            getFacebookAccessToken();
                        } catch (JSONException exception) {
                            Log.d(TAG, "retrieveFacebookInformation: failure, response code: " + response.getError());
                            exception.printStackTrace();
                        } catch (Exception exception) {
                            Log.d(TAG, "retrieveFacebookInformation: failure, response code: " + response.getError());
                        }
                    }
                }
        );
        Bundle neededInformation = new Bundle();
        neededInformation.putString("fields", "name, email");
        request.setParameters(neededInformation);
        request.executeAsync();
    }

    /**
     * Retrieve's the user's cognito user pool information
     *
     * @param context The activity where the User object is created
     */
    private static void retrieveCognitoInformation(final Context context) {
        //Retrieve user's information from shared preferences
        SharedPreferences sharedPreferences = context.getSharedPreferences(AuthHelper.COGNITO_INFO, Context.MODE_PRIVATE);
        name = sharedPreferences.getString(AuthHelper.COGNITO_USER_NAME, null);
        email = sharedPreferences.getString(AuthHelper.COGNITO_EMAIL, null);
        if (name == null || email == null) {
            throw new RuntimeException("Attempt to retrieve empty Cognito user details");
        }
        signInProvider = COGNITO_PROVIDER;
        Log.d(TAG, "retrieveCognitoInformation: Finished retrieving Cognito information");
        getCognitoAccessToken(context);
    }


    /**
     * Retrieve Google access token
     *
     * @return Google's access token
     */
    private static void getGoogleAccessToken(final Context context) {
        //Initialize Google Account object
        final GoogleSignInAccount googleAccount = GoogleSignIn.getLastSignedInAccount(context);

        //Google Token Scope
        final String scope = "audience:server:client_id:" + context.getString(R.string.google_web_client_id);

        //Run on separate thread to prevent deadlock
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                try {
                    Log.d(TAG, "Attempting to retrieve token");
                    String accessToken = GoogleAuthUtil.getToken(context, googleAccount.getAccount(), scope);

                    if (accessToken != null) {
                        Log.d(TAG, "getGoogleAccessToken: Google token is OK. Token Hashcode = " + accessToken.hashCode());
                        googleAccessToken = accessToken;
                    } else {
                        Log.w(TAG, "getGoogleAccessToken: Google token is NULL");
                    }
                } catch (Exception exception) {
                    Log.w(TAG, "getGoogleAccessToken: Error retrieving access token");
                    exception.printStackTrace();
                }
            }
        };
        new Thread(runnable).start();
    }

    private static void getCognitoAccessToken(final Context context) {
        CognitoUserPool cognitoUserPool = AuthHelper.getCognitoUserPool(context);
        //Retrieves the current user signed in
        CognitoUser cognitoUser = cognitoUserPool.getCurrentUser();
        cognitoUser.getSessionInBackground(new AuthenticationHandler() {
            @Override
            public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
                Log.d(TAG, "Successfully retrieved user session object");
                //Store the user access and refresh tokens
                cognitoAccessToken = userSession.getAccessToken();
                cognitoRefreshToken = userSession.getRefreshToken();
            }

            @Override
            public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
                //Not Applicable
            }

            @Override
            public void getMFACode(MultiFactorAuthenticationContinuation continuation) {
                //Not Applicable
            }

            @Override
            public void authenticationChallenge(ChallengeContinuation continuation) {
                //Not Applicable
            }

            @Override
            public void onFailure(Exception exception) {
                Log.w(TAG, "getCognitoAccessToken->onFailure: Failed to get user's access token");
                exception.printStackTrace();
            }
        });
    }

    private static void getFacebookAccessToken() {
        final AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken != null && !accessToken.isExpired()) {
            Log.d(TAG, "Facebook Access Token is OK. Token hashcode = " + accessToken.hashCode());
            facebookAccessToken = accessToken.getToken();
        } else {
            Log.d(TAG, "Facebook Access Token is null or expired");
        }
    }

    /**
     * Make singleton from serialize and deserialize operation
     *
     * @param context The activity calling this
     * @return The user instance object
     */
    protected User readResolve(Context context) {
        return getInstance(context);
    }

}
