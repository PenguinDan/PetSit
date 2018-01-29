package edu.csulb.petsitter;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.exceptions.CognitoInternalErrorException;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.tokens.CognitoAccessToken;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.tokens.CognitoIdToken;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.tokens.CognitoRefreshToken;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.util.CognitoSecretHash;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.util.CognitoServiceConstants;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProvider;
import com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProviderClient;
import com.amazonaws.services.cognitoidentityprovider.model.AuthFlowType;
import com.amazonaws.services.cognitoidentityprovider.model.AuthenticationResultType;
import com.amazonaws.services.cognitoidentityprovider.model.InitiateAuthRequest;
import com.amazonaws.services.cognitoidentityprovider.model.InitiateAuthResult;
import com.amazonaws.services.cognitoidentityprovider.model.NotAuthorizedException;
import com.amazonaws.services.cognitoidentityprovider.model.RespondToAuthChallengeRequest;
import com.amazonaws.services.cognitoidentityprovider.model.RespondToAuthChallengeResult;
import com.amazonaws.util.Base64;
import com.amazonaws.util.StringUtils;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.SignInButton;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class LoginFragment extends Fragment
        implements View.OnClickListener {

    //Views
    private EditText emailInputEditText;
    private EditText passwordInputEditText;
    //Log in
    private GoogleSignInClient googleSignInClient;
    private CognitoUserPool cognitoUserPool;
    //Variables
    private OnButtonClicked onButtonClickedListener;
    private CognitoCachingCredentialsProvider credentialsProvider;
    private boolean currentlySigningIn;
    //Constants
    private final String TAG = "LoginFragment";
    //Interfaces
    public interface OnButtonClicked {
        void buttonClicked(String reason);
    }

    //Classes
    private class CognitoHelper extends AsyncTask<String, Void, Void> {
        private AuthHelper authHelper = new AuthHelper(cognitoUserPool.getUserPoolId());
        private final int SRP_RADIX = 16;
        private String usernameInternal;
        private String secretHash;
        private AlertDialog alertDialog;
        private boolean loginSuccess;

        @Override
        protected Void doInBackground(String... strings) {
            String userEmail = strings[0];
            String userPassword = strings[1];

            Log.d(TAG, "CognitoHelper: doInBackground");
            Log.d(TAG, "CognitoHelper: email: " + userEmail);
            Log.d(TAG, "CognitoHelper: password" + userPassword);

            //Creates a authentication request to start authentication with user SRP verification
            final InitiateAuthRequest initiateAuthRequest = new InitiateAuthRequest();
            initiateAuthRequest.setAuthFlow(AuthFlowType.USER_SRP_AUTH);
            initiateAuthRequest.setClientId(cognitoUserPool.getClientId());
            initiateAuthRequest.addAuthParametersEntry(CognitoServiceConstants.AUTH_PARAM_SECRET_HASH,
                    CognitoSecretHash.getSecretHash(
                            userEmail,
                            getString(R.string.aws_cognito_client_id),
                            getString(R.string.aws_cognito_client_secret)
                    ));
            initiateAuthRequest.addAuthParametersEntry(CognitoServiceConstants.AUTH_PARAM_SRP_A,
                    authHelper.getA().toString(SRP_RADIX));
            initiateAuthRequest.addAuthParametersEntry(CognitoServiceConstants.AUTH_PARAM_USERNAME,
                    userEmail);

            //Build Client
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            AmazonCognitoIdentityProvider cipClient = new AmazonCognitoIdentityProviderClient(
                    new AnonymousAWSCredentials(),
                    clientConfiguration
            );
            cipClient.setRegion(Region.getRegion(Regions.US_EAST_1));

            //Start the user authentication with user password verification
            final InitiateAuthResult initiateAuthResult = cipClient.initiateAuth(initiateAuthRequest);
            if (CognitoServiceConstants.CHLG_TYPE_USER_PASSWORD_VERIFIER.equals(
                    initiateAuthResult.getChallengeName())) {
                final String userIdForSRP = initiateAuthResult.getChallengeParameters()
                        .get(CognitoServiceConstants.CHLG_PARAM_USER_ID_FOR_SRP);
                usernameInternal = initiateAuthResult.getChallengeParameters()
                        .get(CognitoServiceConstants.CHLG_PARAM_USERNAME);
                secretHash = CognitoSecretHash.getSecretHash(
                        usernameInternal,
                        getString(R.string.aws_cognito_client_id),
                        getString(R.string.aws_cognito_client_secret)
                );
                final BigInteger srpB = new BigInteger(initiateAuthResult.getChallengeParameters()
                        .get(CognitoServiceConstants.CHLG_PARAM_SRP_B), 16);
                if (srpB.mod(authHelper.N).equals(BigInteger.ZERO)) {
                    throw new CognitoInternalErrorException("SRP error, B cannot be zero");
                }
                final BigInteger salt = new BigInteger(initiateAuthResult.getChallengeParameters()
                        .get(CognitoServiceConstants.CHLG_PARAM_SALT), 16);
                final byte[] key = authHelper.getPasswordAuthenticationKey(userIdForSRP,
                        userPassword, srpB, salt);

                final Date timestamp = new Date();
                byte[] hmac;
                String dateString;
                try {
                    final Mac mac = Mac.getInstance("HmacSHA256");
                    final SecretKey keySpec = new SecretKeySpec(key, "HmacSHA256");
                    mac.init(keySpec);
                    mac.update(cognitoUserPool.getUserPoolId().split("_", 2)[1].getBytes(StringUtils.UTF8));
                    mac.update(userIdForSRP.getBytes(StringUtils.UTF8));
                    final byte[] secretBlock = Base64.decode(initiateAuthResult.getChallengeParameters()
                            .get(CognitoServiceConstants.CHLG_PARAM_SECRET_BLOCK));
                    mac.update(secretBlock);
                    final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                            "EEE MMM d HH:mm:ss z yyyy", Locale.US
                    );
                    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    dateString = simpleDateFormat.format(timestamp);
                    final byte[] dateBytes = dateString.getBytes(StringUtils.UTF8);

                    hmac = mac.doFinal(dateBytes);
                } catch (final Exception exception) {
                    throw new CognitoInternalErrorException("SRP error", exception);
                }

                try {
                    final Map<String, String> srpAuthResponses = new HashMap<>();
                    srpAuthResponses.put(CognitoServiceConstants.CHLG_RESP_PASSWORD_CLAIM_SECRET_BLOCK,
                            initiateAuthResult.getChallengeParameters().get(CognitoServiceConstants.CHLG_PARAM_SECRET_BLOCK));
                    srpAuthResponses.put(CognitoServiceConstants.CHLG_RESP_PASSWORD_CLAIM_SIGNATURE,
                            new String(Base64.encode(hmac), StringUtils.UTF8));
                    srpAuthResponses.put(CognitoServiceConstants.CHLG_RESP_TIMESTAMP, dateString);
                    srpAuthResponses.put(CognitoServiceConstants.CHLG_RESP_USERNAME, usernameInternal);
                    srpAuthResponses.put(CognitoServiceConstants.CHLG_RESP_SECRET_HASH, secretHash);

                    final RespondToAuthChallengeRequest authChallengeRequest = new RespondToAuthChallengeRequest();
                    authChallengeRequest.setChallengeName(initiateAuthResult.getChallengeName());
                    authChallengeRequest.setClientId(getString(R.string.aws_cognito_client_id));
                    authChallengeRequest.setSession(initiateAuthResult.getSession());
                    authChallengeRequest.setChallengeResponses(srpAuthResponses);
                    final RespondToAuthChallengeResult challenge = cipClient.respondToAuthChallenge(authChallengeRequest);

                    AuthenticationResultType authenticationResultType = challenge.getAuthenticationResult();
                    CognitoIdToken cognitoIdToken = new CognitoIdToken(authenticationResultType.getIdToken());
                    CognitoAccessToken cognitoAccessToken = new CognitoAccessToken(authenticationResultType.getAccessToken());
                    CognitoRefreshToken cognitoRefreshToken = new CognitoRefreshToken(authenticationResultType.getRefreshToken());
                    CognitoUserSession cognitoUserSession = new CognitoUserSession(cognitoIdToken, cognitoAccessToken, cognitoRefreshToken);
                    Log.d(TAG, "onPostExecute : Login Successful");
                    //Retrieve id token from CognitoUserSession
                    String idToken = cognitoUserSession.getIdToken().getJWTToken();
                    //Create a credentials provider, or use the existing provider
                    Map<String, String> logins = new HashMap<>();
                    loginSuccess = true;
                } catch (NotAuthorizedException notAuthorizedException) {
                    Log.w(TAG, "Wrong password or username");
                }
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onCancelled(Void aVoid) {
            super.onCancelled(aVoid);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d(TAG, "onPostExecute");
            alertDialog.dismiss();

            if(!loginSuccess) {
                Log.d(TAG, "onPostExecute: Login Failure");
                AlertDialog errorDialog = createLoginErrorDialog();
                errorDialog.show();
            } else {
                Log.d(TAG, "onPostExecute: Login Success");

            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            loginSuccess = false;

            alertDialog = createLoginDialog();
            alertDialog.show();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            onButtonClickedListener = (OnButtonClicked) context;
        } catch (ClassCastException exception) {
            throw new ClassCastException(context.toString() + " must implement OnButtonClicked");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //Initialize Variables
        currentlySigningIn = false;
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getActivity(),
                getString(R.string.aws_identity_pool_id),
                Regions.US_EAST_1
        );


        //Initialize Views
        SignInButton googleSignInButton = (SignInButton) getActivity().findViewById(R.id.sign_in_button_google);
        emailInputEditText = (EditText) getActivity().findViewById(R.id.email_edit_text);
        passwordInputEditText = (EditText) getActivity().findViewById(R.id.password_edit_text);
        Button signInButton = (Button) getActivity().findViewById(R.id.sign_in_button);
        TextView createAccountTextView = (TextView) getActivity().findViewById(R.id.create_account_text);
        TextView forgotPasswordTextView = (TextView) getActivity().findViewById(R.id.forgot_password_text);
        ImageButton facebookLoginButton = (ImageButton) getActivity().findViewById(R.id.facebook_login_button);

        //Configure Facebook Sign In

        //Initialize Listeners
        signInButton.setOnClickListener(this);
        createAccountTextView.setOnClickListener(this);
        forgotPasswordTextView.setOnClickListener(this);
    }

    //User is attempting to sign in using their email and password
    private void normalSignIn() {
        final String userEmail = emailInputEditText.getText().toString();
        final String userPassword = passwordInputEditText.getText().toString();

        new CognitoHelper().execute(userEmail, userPassword);
    }

    public void setCognitoUserPool(CognitoUserPool cognitoUserPool) {
        this.cognitoUserPool = cognitoUserPool;
    }

    public void setUserEmailEditText(String userEmail) {
        emailInputEditText.setText(userEmail);
    }

    public void setUserPasswordEditText(String userPassword) {
        passwordInputEditText.setText(userPassword);
    }

    @Override
    public void onClick(View view) {
        currentlySigningIn = true;
        switch (view.getId()) {
            case R.id.sign_in_button: {
                Log.d(TAG, "Normal Sign In Chosen");
                //Check if the EditText boxes empty
                boolean error = false;
                String errorMessage = "";
                if (emailInputEditText.getText().toString().isEmpty()) {
                    error = true;
                    errorMessage = "Email field cannot be empty.";
                } else if (passwordInputEditText.getText().toString().isEmpty()) {
                    error = true;
                    errorMessage = "Password field cannot be empty";
                }
                if (error) {
                    //Initialize Alert Dialog
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                    alertDialogBuilder.setTitle("Error Message");
                    alertDialogBuilder.setCancelable(true);
                    alertDialogBuilder.setMessage(errorMessage);
                    alertDialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
                    alertDialogBuilder.show();
                } else {
                    Log.d(TAG, "Sign in, Else Case");
                    normalSignIn();
                }
            }
            break;
            case R.id.create_account_text: {
                onButtonClickedListener.buttonClicked("create_account");
            }
            break;
        }
    }

    private AlertDialog createLoginDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle("PetSit SignIn");
        //Animate this later on
        alertDialogBuilder.setMessage("Logging User In...");
        alertDialogBuilder.setCancelable(false);
        return alertDialogBuilder.create();
    }

    private AlertDialog createLoginErrorDialog() {
       AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
       alertDialogBuilder.setTitle("Error");
        alertDialogBuilder.setMessage("Email and Password combination not found");
        alertDialogBuilder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        return alertDialogBuilder.create();
    }

    public boolean isHandlingSignIn() {
        return currentlySigningIn;
    }
}

