package com.freshdigitable.udonroad;

import android.content.Context;
import android.content.SharedPreferences;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

/**
 * Created by akihit on 15/10/20.
 */
public class AccessUtil {
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

  private static AccessToken loadAccessToken(Context context) {
    SharedPreferences sharedPreference = context.getSharedPreferences("udonroad_prefs", Context.MODE_PRIVATE);
    String token = sharedPreference.getString("token", null);
    if (token == null) {
      return null;
    }
    String tokenSecret = sharedPreference.getString("token_secret", null);
    if (tokenSecret == null) {
      return null;
    }

    return new AccessToken(token, tokenSecret);
  }

}
