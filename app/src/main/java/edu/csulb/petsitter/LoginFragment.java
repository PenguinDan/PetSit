package edu.csulb.petsitter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.FaceDetector;
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
//import com.amazonaws.client.AwsSyncClientParams;
//import com.amazonaws.client.builder.AwsClientBuilder;
//import com.amazonaws.client.builder.AwsSyncClientBuilder;
//import com.amazonaws.http.SdkHttpMetadata;
import com.amazonaws.mobile.auth.core.IdentityHandler;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.IdentityProvider;
import com.amazonaws.mobile.auth.core.SignInResultHandler;
import com.amazonaws.mobile.auth.core.signin.SignInManager;
import com.amazonaws.mobile.auth.core.signin.SignInProviderResultHandler;
import com.amazonaws.mobile.auth.facebook.FacebookSignInProvider;
import com.amazonaws.mobile.auth.google.GoogleSignInProvider;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
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
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentity;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClient;
//import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClientBuilder;
import com.amazonaws.services.cognitoidentity.model.GetCredentialsForIdentityRequest;
import com.amazonaws.services.cognitoidentity.model.GetCredentialsForIdentityResult;
import com.amazonaws.services.cognitoidentity.model.GetIdRequest;
import com.amazonaws.services.cognitoidentity.model.GetIdResult;
//import com.amazonaws.services.cognitoidentity.model.GetOpenIdTokenForDeveloperIdentityRequest;
//import com.amazonaws.services.cognitoidentity.model.GetOpenIdTokenForDeveloperIdentityResult;
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
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    private GoogleApiClient googleApiClient;
    private CognitoCachingCredentialsProvider credentialsProvider;
    private CognitoUserPool cognitoUserPool;
    private SignInManager signInManager;
    private FacebookSignInProvider facebookSignInProvider;
    private GoogleSignInProvider googleSignInProvider;
    //Variables
    private OnButtonClicked onButtonClickedListener;
    private boolean currentlySigningIn;
    private AWSConfiguration awsConfiguration;
    private AlertDialog loginDialog;
    //Constants
    private final static String TAG = "LoginFragment";
    private final static int RC_GOOGLE_SIGN_IN = 16723;
    private final static int HTTP_SUCCESS = 200;

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

                    String idToken = cognitoUserSession.getIdToken().getJWTToken();
                    Map<String, String> logins = new HashMap<>();
                    logins.put(credentialsProvider.getIdentityPoolId(), idToken);
                    credentialsProvider.setLogins(logins);

                    onIdentityResult();
                    Log.d(TAG, "onPostExecute : Login Successful");
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

            dismissLoginDialog();

            if (!loginSuccess) {
                Log.d(TAG, "onPostExecute: Login Failure");
                AlertDialog errorDialog = createLoginErrorDialog("Username and password combination not found.");
                errorDialog.show();
            } else {
                Log.d(TAG, "onPostExecute: Login Success");
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            loginSuccess = false;

            loginDialog = createLoginDialog();
            loginDialog.show();
        }
    }

    private class SignInResultHandlerImpl implements SignInProviderResultHandler {
        /**
         * Sign-in was successful
         *
         * @param provider sign-in identity provider
         */
        @Override
        public void onSuccess(final IdentityProvider provider) {
            Log.i(TAG, "SignInResultHandlerImpl->onSuccess");


            //Sign in manager is no longer needed because the user is signed in
            SignInManager.dispose();
            final SignInResultHandler signInResultHandler = signInManager.getResultHandler();

            //Call back the results handler
            signInResultHandler.onSuccess(getActivity(), provider);
            IdentityManager.getDefaultIdentityManager().getUserID(new IdentityHandler() {
                @Override
                public void onIdentityId(final String identityId) {
                    Log.d(TAG, "SignInResultHandlerImpl-> onSuccess-> onIdentityId: " + identityId);

                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            //Update credentials provider
                            Map<String, String> logins = new HashMap<>();
                            logins.put(provider.getCognitoLoginKey(), provider.getToken());
                            credentialsProvider.setLogins(logins);
                            //Start credentials for identity request
                            AmazonCognitoIdentityClient amazonCognitoIdentityClient =
                                    new AmazonCognitoIdentityClient(credentialsProvider);
                            GetCredentialsForIdentityRequest credentialsForIdentityRequest = new GetCredentialsForIdentityRequest();
                            credentialsForIdentityRequest
                                    .withIdentityId(identityId)
                                    .withLogins(credentialsProvider.getLogins());
                            GetCredentialsForIdentityResult credentialsForIdentityResult =
                                    amazonCognitoIdentityClient.getCredentialsForIdentity(credentialsForIdentityRequest);
                            onIdentityResult();
                        }
                    });
                }

                @Override
                public void handleError(Exception exception) {
                    Log.w(TAG, "SignInResultHandlerImpl-> onSuccess-> handleError", exception);
                }
            });

        }

        /**
         * Sign-in failed.
         *
         * @param provider sign-in identity provider
         * @param ex       exception that occurred
         */
        @Override
        public void onError(IdentityProvider provider, Exception ex) {
            Log.i(TAG, "SignInResultHandlerImpl->onError");

            signInManager.getResultHandler()
                    .onIntermediateProviderError(getActivity(), provider, ex);
        }

        /**
         * Sign-in was cancelled by the user.
         *
         * @param provider sign-in identity provider
         */
        @Override
        public void onCancel(IdentityProvider provider) {
            Log.i(TAG, "SignInResultHandlerImpl->onCancel");
            if (loginDialog.isShowing()) {
                loginDialog.dismiss();
            }
            signInManager.getResultHandler().onIntermediateProviderCancel(getActivity(), provider);
        }
    }

    public void onIdentityResult() {
        //Dismiss the login dialog fragment
        if (loginDialog.isShowing()) {
            loginDialog.dismiss();
        }

        //Move to the activity and finish sign in so the activity gets popped off the stack
        Intent intent = new Intent(getActivity(), MainActivityContainer.class);
        startActivity(intent);
        getActivity().finish();
    }

    public void dismissLoginDialog() {
        if(loginDialog.isShowing()) {
            loginDialog.dismiss();
        }
    }

    /**
     *
     * @param context
     */
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
        awsConfiguration = new AWSConfiguration(getActivity().getApplicationContext());
        //Setup IdentityManager
        SignInResultHandler signInResultHandler = new SignInResultHandler() {
            @Override
            public void onSuccess(Activity callingActivity, IdentityProvider provider) {
                Log.d(TAG, "SignInResultHandler->onSuccess");
            }

            @Override
            public void onIntermediateProviderCancel(Activity callingActivity, IdentityProvider provider) {
                Log.d(TAG, "SignInResultHandler->onIntermediateProviderCancel");
            }

            @Override
            public void onIntermediateProviderError(Activity callingActivity, IdentityProvider provider, Exception ex) {
                Log.d(TAG, "SignInResultHandler->onIntermediateProviderError");
            }

            @Override
            public boolean onCancel(Activity callingActivity) {
                Log.d(TAG, "SignInResultHandler->onCancel");
                return false;
            }
        };
        IdentityManager identityManager = IdentityManager.getDefaultIdentityManager();
        //Initializes SignInManager instance and sets SignInManager.setResultHandler()
        identityManager.login(getActivity(), signInResultHandler);


        //Setup SignInManager and sets IdentityManager.setProviderResultsHandler()
        signInManager = SignInManager.getInstance();
        signInManager.setProviderResultsHandler(getActivity(), new SignInResultHandlerImpl());

        //Initialize Views
        SignInButton googleSignInButton = (SignInButton) getActivity().findViewById(R.id.google_login_button);
        emailInputEditText = (EditText) getActivity().findViewById(R.id.email_edit_text);
        passwordInputEditText = (EditText) getActivity().findViewById(R.id.password_edit_text);
        Button signInButton = (Button) getActivity().findViewById(R.id.sign_in_button);
        TextView createAccountTextView = (TextView) getActivity().findViewById(R.id.create_account_text);
        TextView forgotPasswordTextView = (TextView) getActivity().findViewById(R.id.forgot_password_text);
        ImageButton facebookLoginButton = (ImageButton) getActivity().findViewById(R.id.facebook_login_button);

        //Initialize Facebook Sign in
        facebookSignInProvider = new FacebookSignInProvider();
        facebookSignInProvider.initializeSignInButton(
                getActivity(),
                facebookLoginButton,
                identityManager.getResultsAdapter()
        );

        //Initialize Google Sign in
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN);
        GoogleSignInOptions gso = builder.requestEmail().requestProfile().build();
        googleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        googleApiClient.connect();
        googleSignInProvider = new GoogleSignInProvider();
        googleSignInProvider.initialize(getActivity().getApplicationContext(), awsConfiguration);
        googleSignInProvider.initializeSignInButton(
                getActivity(),
                googleSignInButton,
                identityManager.getResultsAdapter()
        );

        //Reset the OnClickListener for the federated login buttons to implement custom flow
        facebookLoginButton.setOnClickListener(this);
        googleSignInButton.setOnClickListener(this);

        //Initialize Listeners
        signInButton.setOnClickListener(this);
        createAccountTextView.setOnClickListener(this);
        forgotPasswordTextView.setOnClickListener(this);
        facebookLoginButton.setOnClickListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        if (currentlySigningIn) {
            //Creates the Login Dialog and displays it to the user
            loginDialog = createLoginDialog();
            loginDialog.show();

            //The user has finished *their* sign in process
            currentlySigningIn = false;

            if (requestCode == RC_GOOGLE_SIGN_IN) {
                googleSignInProvider.handleActivityResult(requestCode, resultCode, data);
            } else if (FacebookSdk.isFacebookRequestCode(requestCode)) {
                facebookSignInProvider.handleActivityResult(requestCode, resultCode, data);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
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
        switch (view.getId()) {
            case R.id.sign_in_button: {
                Log.d(TAG, "Normal Sign In Chosen");
                currentlySigningIn = true;
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
            case R.id.facebook_login_button: {
                Log.d(TAG, "onClick-> Facebook Login Button");
                currentlySigningIn = true;
                LoginManager.getInstance().logInWithReadPermissions(getActivity(),
                        Arrays.asList("public_profile", "email"));
            }
            break;
            case R.id.google_login_button: {
                Log.d(TAG, "onClick -> Google Login Button");
                currentlySigningIn = true;
                Intent intent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
                getActivity().startActivityForResult(intent, RC_GOOGLE_SIGN_IN);
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

    private AlertDialog createLoginErrorDialog(String errorMessage) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle("Error");
        alertDialogBuilder.setMessage(errorMessage);
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

