package edu.csulb.petsitter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

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
    //Other variables
    //Constants
    private final static String TAG = User.class.getSimpleName();
    //Public Constants
    public final static String GOOGLE_PROVIDER  = "google";
    public final static String FACEBOOK_PROVIDER = "facebook";
    public final static String COGNITO_PROVIDER = "cognito";
    public final static String USER_PREFERENCES = "user";
    public final static String USER_SIGN_IN_PROVIDER = "provider";


    /**
     * Does not allow the developer to create a User object through the constructor
     */
    private User() {
        //Prevent instantiation from the reflection API
        if(userInstace != null) {
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
        if(userInstace == null) {
            synchronized (User.class) {
                if(userInstace == null) {
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
//
//    public String getUserAccessToken(Context context) {
//        //Retrieve user access token depending on the provider
//        switch(signInProvider){
//            case GOOGLE_PROVIDER:{
//                return getGoogleAccessToken(context);
//            }
//            case FACEBOOK_PROVIDER: {
//
//            }
//            case COGNITO_PROVIDER: {
//
//            }
//        }
//        return null;
//    }

    private static void retrieveUserInformation(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(USER_PREFERENCES, Context.MODE_PRIVATE);
        String signInProvider = sharedPreferences.getString(USER_SIGN_IN_PROVIDER, null);
        if(signInProvider == null) {
            throw new RuntimeException("Sign in provider is null, user is not signed in");
        }
        switch(signInProvider) {
            case GOOGLE_PROVIDER:{
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
    }

    /**
     * Retrieve's the user's facebook information
     */
    private static void retrieveFacebookInformation() {
        AccessToken facebookAccessToken = AccessToken.getCurrentAccessToken();
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
                        }catch(JSONException exception) {
                            Log.d(TAG, "retrieveFacebookInformation: failure");
                            exception.printStackTrace();
                        }
                    }
                }
        );
        Bundle neededInformation = new Bundle();
        neededInformation.putString("email", "name");
        request.setParameters(neededInformation);
        request.executeAsync();
    }

    /**
     * Retrieve's the user's cognito user pool information
     *
     * @param context The activity where the User object is created
     */
    private static void retrieveCognitoInformation(Context context) {
        //Retrieve user's information from shared preferences
        SharedPreferences sharedPreferences = context.getSharedPreferences(CognitoHelper.COGNITO_INFO, Context.MODE_PRIVATE);
        name = sharedPreferences.getString(CognitoHelper.COGNITO_USER_NAME, null);
        email = sharedPreferences.getString(CognitoHelper.COGNITO_EMAIL, null);
        if(name == null || email == null) {
            throw new RuntimeException("Attempt to retrieve empty Cognito user details");
        }
        signInProvider = COGNITO_PROVIDER;
        Log.d(TAG, "retrieveCognitoInformation: Finished retrieving Cognito information");
    }

//    /**
//     * Retrieve Google access token
//     *
//     * @return Google's access token
//     */
//    private String getGoogleAccessToken(Context context) {
//        String accessToken;
//        //Initialize Google Account object
//        //Look at https://github.com/aws/aws-sdk-android/blob/master/aws-android-sdk-auth-google/src/main/java/com/amazonaws/mobile/auth/google/GoogleSignInProvider.java
//        GoogleSignInAccount googleAccount = GoogleSignIn.getLastSignedInAccount(context);
//
////        accessToken = GoogleAuthUtil.getToken(context, googleAccount.getAccount(), )
////        return accessToken;
//    }
//
//    private String getCognitoAccessToken() {
//        String accessToken;
//
//        return accessToken;
//    }
//
//    private String getFacebookAccessToken() {
//        String accessToken;
//
//        return accessToken;
//    }

    /**
     * Make singleton from serialize and deserialize operation
     *
     * @param context The activity calling this
     *
     * @return The user instance object
     */
    protected User readResolve(Context context) {
        return getInstance(context);
    }
}