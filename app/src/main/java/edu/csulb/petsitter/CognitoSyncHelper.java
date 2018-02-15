package edu.csulb.petsitter;

import android.content.Context;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.regions.Regions;

import java.io.Serializable;

/**
 * Created by Daniel on 2/14/2018.
 */

public class CognitoSyncHelper implements Serializable{
    private static volatile CognitoSyncHelper helperInstance;


    private CognitoSyncHelper() {
        //Prevent the use of the Reflection API destroying singleton integrity
        if(helperInstance != null) {
            throw new RuntimeException("Only use getInstance() to create this object");
        }
    }

}
