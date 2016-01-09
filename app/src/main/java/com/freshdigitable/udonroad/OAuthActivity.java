package com.freshdigitable.udonroad;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.StringRes;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 * Created by akihit on 15/10/22.
 */
@EActivity(R.layout.activity_login)
public class OAuthActivity extends AppCompatActivity {
  private static final String TAG = OAuthActivity.class.getName();

  @StringRes(R.string.callback_url)
  String callbackUrl;

  @ViewById(R.id.button_oauth)
  Button oauthButton;

  private Twitter twitter;

  @AfterViews
  protected void afterViews() {
    twitter = TwitterApi.getTwitterInstance(this);
  }

  @Click(R.id.button_oauth)
  protected void onClicked() {
    startAuthorization();
  }

  private RequestToken requestToken;

  @Background
  protected void startAuthorization() {
    try {
      requestToken = twitter.getOAuthRequestToken(callbackUrl);
    } catch (TwitterException e) {
      Log.e(TAG, "Authorization error: ", e);
      return;
    }
    String authUrl = requestToken.getAuthorizationURL();
    startAuthAction(authUrl);
  }

  @UiThread
  protected void startAuthAction(String url) {
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

  @Background
  protected void startAuthentication(String verifier) {
    AccessToken accessToken = null;
    try {
      accessToken = twitter.getOAuthAccessToken(requestToken, verifier);
    } catch (TwitterException e) {
      Log.e(TAG, "authentication error: ", e);
    }
    checkOAuth(accessToken);
  }

  @UiThread
  protected void checkOAuth(AccessToken accessToken) {
    if (accessToken == null) {
      Toast.makeText(this, "authentication is failed...", Toast.LENGTH_LONG).show();
    }
    TwitterApi.storeAccessToken(this, accessToken);
    Toast.makeText(this, "authentication is success!", Toast.LENGTH_LONG).show();
    Intent intent = new Intent(this, MainActivity_.class);
    startActivity(intent);
    finish();
  }
}

