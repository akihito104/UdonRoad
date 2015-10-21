package com.freshdigitable.udonroad;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.shapes.Shape;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

/**
 * Created by akihit on 15/10/20.
 */
public class AccessUtil {

  public static final String TOKEN = "token";
  public static final String TOKEN_SECRET = "token_secret";

  public static Twitter getTwitterInstance(Context context) {
    String consumerKey = context.getString(R.string.consumer_key);
    String consumerSecret = context.getString(R.string.consumer_secret);
    TwitterFactory factory = new TwitterFactory();
    Twitter twitter = factory.getInstance();
    twitter.setOAuthConsumer(consumerKey, consumerSecret);

    AccessToken accessToken = loadAccessToken(context);
    if (accessToken != null) {
      twitter.setOAuthAccessToken(accessToken);
    }

    return twitter;
  }

  public static void storeAccessToken(Context context, AccessToken token) {
    SharedPreferences.Editor editor = getAppSharedPrefs(context).edit();
    editor.putString(TOKEN, token.getToken());
    editor.putString(TOKEN_SECRET, token.getTokenSecret());
    editor.commit();
  }

  private static AccessToken loadAccessToken(Context context) {
    SharedPreferences sharedPreference = getAppSharedPrefs(context);
    String token = sharedPreference.getString(TOKEN, null);
    if (token == null) {
      return null;
    }
    String tokenSecret = sharedPreference.getString(TOKEN_SECRET, null);
    if (tokenSecret == null) {
      return null;
    }

    return new AccessToken(token, tokenSecret);
  }

  private static SharedPreferences getAppSharedPrefs(Context context) {
    return context.getSharedPreferences("udonroad_prefs", Context.MODE_PRIVATE);
  }

}
