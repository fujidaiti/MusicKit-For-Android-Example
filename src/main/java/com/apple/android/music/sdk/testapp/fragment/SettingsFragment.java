package com.apple.android.music.sdk.testapp.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.apple.android.music.sdk.testapp.R;
import com.apple.android.music.sdk.testapp.util.AppPreferences;
import com.apple.android.sdk.authentication.AuthenticationFactory;
import com.apple.android.sdk.authentication.AuthenticationManager;
import com.apple.android.sdk.authentication.TokenError;
import com.apple.android.sdk.authentication.TokenResult;

import java.util.HashMap;


/**
 * Copyright (C) 2017 Apple, Inc. All rights reserved.
 */
public final class SettingsFragment extends BaseFragment {

    public static final String TAG = "SettingsFragment";
    private AuthenticationManager authenticationManager;
    private TextView usernameTextView;
    private Button loginButton;
    private TextView userTokenTxtView;
    private TextView userTokenLabel;

    // Activity Result API launcher
    private ActivityResultLauncher<Intent> authLauncher;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register the activity result launcher
        authLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.e(TAG, "onActivityResult: resultCode = " + result.getResultCode() + ", data = " + result.getData());
                handleAuthenticationResult(result.getData());
            }
        );
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        authenticationManager = AuthenticationFactory.createAuthenticationManager(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_settings, container, false);
        usernameTextView = v.findViewById(R.id.settings_logged_in_as);
        loginButton = v.findViewById(R.id.settings_login_button);
//        loginButton.setOnClickListener(this);

        Button requestAccessBtn = (Button) v.findViewById(R.id.requestAccessBtn);
        requestAccessBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Use createIntentBuilder api to create the Intentbuilder which the 3rd party app can use to customize a few things
                HashMap params = new HashMap<String, String>();
                params.put("ct", "mytestCampaignToken");
                params.put("at", "mytestAffiliateToken");
                if (authenticationManager == null) {
                    authenticationManager = AuthenticationFactory.createAuthenticationManager(getActivity());
                }
                Intent intent = authenticationManager.createIntentBuilder(getString(R.string.developer_token))
                        .setHideStartScreen(false)
                        .setStartScreenMessage("To play the full song, connect CustomMusicPlayer to Apple Music. (Can be replaced by third party app's custom text message)")
                        //set this if you want to set custom params
                        .setCustomParams(params)
                        // set this if you want to have contextual upsell
                        .setContextId("1100742453")
                        // invoke build to generate the intent, make sure to launch with authLauncher if you care about the music-user-token being returned.
                        .build();

                // Log intent details for debugging
                logIntentDetails(intent);

                authLauncher.launch(intent);
            }
        });
        userTokenTxtView = v.findViewById(R.id.userTokenValue);
        v.findViewById(R.id.login_layout).setVisibility(View.INVISIBLE);
        userTokenLabel = v.findViewById(R.id.usertoken_label);
        String userToken = AppPreferences.with(getActivity()).getAppleMusicUserToken();
        if (userToken != null && !userToken.isEmpty()) {
            userTokenTxtView.setText(userToken);
            userTokenLabel.setVisibility(View.VISIBLE);
        }
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(getString(R.string.title_settings));
    }

    /**
     * Handle authentication result from the Activity Result API
     */
    private void handleAuthenticationResult(Intent data) {
        String textViewText = "";
        TokenResult tokenResult = authenticationManager.handleTokenResult(data);
        if (!tokenResult.isError()) {
            String musicUserToken = tokenResult.getMusicUserToken();
            textViewText = musicUserToken;
            AppPreferences.with(getActivity()).setAppleMusicUserToken(musicUserToken);
        } else {
            TokenError error = tokenResult.getError();
            textViewText = "Error getting token: " + error;
        }
        userTokenTxtView.setText(textViewText);
        userTokenLabel.setVisibility(View.VISIBLE);
    }

    /**
     * Log detailed information about an Intent for debugging
     */
    private void logIntentDetails(Intent intent) {
        if (intent == null) {
            Log.d(TAG, "Intent is null");
            return;
        }

        Log.d(TAG, "========== Intent Details ==========");

        // Basic intent information
        Log.d(TAG, "Action: " + intent.getAction());
        Log.d(TAG, "Data URI: " + intent.getData());
        Log.d(TAG, "Type: " + intent.getType());
        Log.d(TAG, "Component: " + intent.getComponent());
        Log.d(TAG, "Package: " + intent.getPackage());
        Log.d(TAG, "Scheme: " + intent.getScheme());

        // Categories
        if (intent.getCategories() != null) {
            Log.d(TAG, "Categories: " + intent.getCategories());
        }

        // Flags
        Log.d(TAG, "Flags: 0x" + Integer.toHexString(intent.getFlags()));

        // Extras (Bundle)
        android.os.Bundle extras = intent.getExtras();
        if (extras != null) {
            Log.d(TAG, "--- Extras ---");
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                Log.d(TAG, "  " + key + " = " + value + " (type: " + (value != null ? value.getClass().getSimpleName() : "null") + ")");
            }
        } else {
            Log.d(TAG, "No extras");
        }

        Log.d(TAG, "====================================");
    }
}
