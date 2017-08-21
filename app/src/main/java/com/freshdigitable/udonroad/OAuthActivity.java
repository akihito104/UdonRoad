/*
 * Copyright (c) 2016. Matsuda, Akihit (akihito104)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freshdigitable.udonroad;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 * OAuthActivity is for OAuth authentication with Twitter.
 *
 * Created by akihit on 15/10/22.
 */
public class OAuthActivity extends AppCompatActivity {
  private static final String TAG = OAuthActivity.class.getName();

  @Inject
  TwitterApi twitterApi;
  @Inject
  AppSettingStore appSettings;
  private View oauthButton;
  private EditText pin;
  private Button sendPin;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
    InjectionUtil.getComponent(this).inject(this);
    oauthButton = findViewById(R.id.button_oauth);
    pin = findViewById(R.id.oauth_pin);
    sendPin = findViewById(R.id.oauth_send_pin);
  }

  @Override
  protected void onStart() {
    super.onStart();
    oauthButton.setOnClickListener(v -> startAuthorization());
    sendPin.setOnClickListener(v -> startAuthentication(pin.getText().toString()));
  }

  @Override
  protected void onStop() {
    super.onStop();
    oauthButton.setOnClickListener(null);
    sendPin.setOnClickListener(null);
  }

  private RequestToken requestToken;

  private void startAuthorization() {
    twitterApi.fetchOAuthRequestToken()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            this::startAuthAction,
            e -> {
              Log.e(TAG, "Authorization error: ", e);
              Toast.makeText(this, "authorization is failed...", Toast.LENGTH_LONG).show();
            });
  }

  private void startAuthAction(RequestToken token) {
    requestToken = token;
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(token.getAuthorizationURL()));
    startActivity(intent);
  }

  private void startAuthentication(final String verifier) {
    twitterApi.fetchOAuthAccessToken(requestToken, verifier)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            this::checkOAuth,
            e -> {
              Log.e(TAG, "authentication error: ", e);
              Toast.makeText(this, "authentication is failed...", Toast.LENGTH_LONG).show();
            });
  }

  private void checkOAuth(AccessToken accessToken) {
    appSettings.open();
    appSettings.storeAccessToken(accessToken);
    appSettings.setCurrentUserId(accessToken.getUserId());
    appSettings.close();
    Toast.makeText(this, "authentication is success!", Toast.LENGTH_LONG).show();
    Intent intent = new Intent(this, getRedirect());
    startActivity(intent);
    finish();
  }

  private static final String EXTRAS_REDIRECT = "redirect";

  public static Intent createIntent(Activity redirect) {
    if (redirect instanceof OAuthActivity) {
      throw new IllegalArgumentException();
    }
    return createIntent(redirect.getApplicationContext(), redirect.getClass());
  }

  public static Intent createIntent(Context context, Class<? extends Activity> clz) {
    final Intent intent = new Intent(context, OAuthActivity.class);
    intent.putExtra(EXTRAS_REDIRECT, clz);
    return intent;
  }

  private Class<?> getRedirect() {
    return (Class<?>) getIntent().getExtras().getSerializable(EXTRAS_REDIRECT);
  }

  public static void start(Activity redirect) {
    final Intent intent = createIntent(redirect);
    redirect.startActivity(intent);
  }
}

