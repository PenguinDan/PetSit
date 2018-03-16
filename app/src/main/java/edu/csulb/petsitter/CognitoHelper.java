package edu.csulb.petsitter;

import android.content.Context;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.regions.Regions;

/**
 * Created by Daniel on 3/16/2018.
 */

public class CognitoHelper {
    //Cognito Constants
    public final static String COGNITO_INFO = "cognito info";
    public final static String COGNITO_EMAIL = "cognito email";
    public final static String COGNITO_USER_NAME = "cognito user name";

    /**
     * User should not be able to generate this class
     */
    private CognitoHelper() {
        throw new RuntimeException("CognitoHelper should not be generated");
    }

    public static CognitoUserPool getCognitoUserPool(Context context) {
        CognitoUserPool cognitoUserPool = new CognitoUserPool(
                context,
                context.getResources().getString(R.string.cognito_pool_id),
                context.getResources().getString(R.string.application_client_id),
                context.getResources().getString(R.string.application_client_secret),
                Regions.US_WEST_2
        );
        
        return cognitoUserPool;
    }
}
