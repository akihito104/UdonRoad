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

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 * Created by akihit on 15/10/22.
 */
public class OAuthActivity extends AppCompatActivity {
  private static final String TAG = OAuthActivity.class.getName();
  private String callbackUrl;

  @Inject
  Twitter twitter;
  @Inject
  SharedPreferences prefs;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
    InjectionUtil.getComponent(this).inject(this);

    callbackUrl = getString(R.string.callback_url);
    findViewById(R.id.button_oauth).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startAuthorization();
      }
    });
  }

  private RequestToken requestToken;

  private void startAuthorization() {
    Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        try {
          requestToken = twitter.getOAuthRequestToken(callbackUrl);
          String authUrl = requestToken.getAuthorizationURL();
          subscriber.onNext(authUrl);
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<String>() {
          @Override
          public void onCompleted() {
          }

          @Override
          public void onError(Throwable e) {
            Log.e(TAG, "Authorization error: ", e);
          }

          @Override
          public void onNext(String authUrl) {
            startAuthAction(authUrl);
          }
        });
  }

  private void startAuthAction(String url) {
    if (url == null) {
      Toast.makeText(this, "authorization is failed...", Toast.LENGTH_LONG).show();
    }
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    startActivity(intent);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    if (intent == null) {
      return;
    }
    Uri uri = intent.getData();
    if (uri == null) {
      return;
    }
    if (!uri.toString().startsWith(callbackUrl)) {
      return;
    }

    startAuthentication(uri.getQueryParameter("oauth_verifier"));
  }

  private void startAuthentication(final String verifier) {
    Observable.create(new Observable.OnSubscribe<AccessToken>() {
      @Override
      public void call(Subscriber<? super AccessToken> subscriber) {
        try {
          AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);
          subscriber.onNext(accessToken);
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<AccessToken>() {
          @Override
          public void onCompleted() {
          }

          @Override
          public void onError(Throwable e) {
            Log.e(TAG, "authentication error: ", e);
          }

          @Override
          public void onNext(AccessToken accessToken) {
            checkOAuth(accessToken);
          }
        });
  }

  private void checkOAuth(AccessToken accessToken) {
    if (accessToken == null) {
      Toast.makeText(this, "authentication is failed...", Toast.LENGTH_LONG).show();
    }
    TwitterApi.storeAccessToken(prefs, accessToken);
    Toast.makeText(this, "authentication is success!", Toast.LENGTH_LONG).show();
    Intent intent = new Intent(this, MainActivity.class);
    startActivity(intent);
    finish();
  }
}

