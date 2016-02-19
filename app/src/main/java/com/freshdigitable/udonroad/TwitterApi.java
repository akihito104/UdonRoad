package com.freshdigitable.udonroad;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.User;
import twitter4j.UserStreamListener;
import twitter4j.auth.AccessToken;

/**
 * Created by akihit on 15/10/20.
 */
public class TwitterApi {
  @SuppressWarnings("unused")
  private static final String TAG = TwitterApi.class.getSimpleName();
  private static final String TOKEN = "token";
  private static final String TOKEN_SECRET = "token_secret";

  @Nullable
  public static TwitterApi setup(final Context context){
    AccessToken accessToken = loadAccessToken(context);
    if (accessToken == null) {
      return null;
    }
    String consumerKey = context.getString(R.string.consumer_key);
    String consumerSecret = context.getString(R.string.consumer_secret);

    TwitterFactory factory = new TwitterFactory();
    final Twitter twitter = factory.getInstance();
    twitter.setOAuthConsumer(consumerKey, consumerSecret);
    twitter.setOAuthAccessToken(accessToken);

    TwitterStreamFactory streamFactory = new TwitterStreamFactory();
    TwitterStream twitterStream = streamFactory.getInstance();
    twitterStream.setOAuthConsumer(consumerKey, consumerSecret);
    twitterStream.setOAuthAccessToken(accessToken);

    return new TwitterApi(twitter, twitterStream);
  }

  private final Twitter twitter;
  private final TwitterStream twitterStream;

  private TwitterApi(Twitter twitter, TwitterStream twitterStream) {
    this.twitter = twitter;
    this.twitterStream = twitterStream;
  }

  public Observable<User> verifyCredentials() {
    return Observable.create(new Observable.OnSubscribe<User>() {
      @Override
      public void call(Subscriber<? super User> subscriber) {
        try {
          User user = twitter.users().showUser(twitter.getId());
          subscriber.onNext(user);
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public void connectUserStream(UserStreamListener listener) {
    twitterStream.addListener(listener);
    twitterStream.user();
  }

  public void disconnectStreamListener() {
    Observable.create(new Observable.OnSubscribe<Void>() {
      @Override
      public void call(Subscriber<? super Void> subscriber) {
        twitterStream.shutdown();
        twitterStream.clearListeners();
      }
    }).subscribeOn(Schedulers.io())
    .subscribe();
  }

  public static Twitter getTwitterInstance(final Context context) {
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
    editor.apply();
  }

  @Nullable
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

  public static boolean hasAccessToken(Context context) {
    return loadAccessToken(context) != null;
  }

  public Observable<Status> updateStatus(final String sendingText) {
    return Observable.create(new Observable.OnSubscribe<Status>() {
      @Override
      public void call(Subscriber<? super Status> subscriber) {
        try {
          subscriber.onNext(twitter.updateStatus(sendingText));
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
        subscriber.onCompleted();
      }
    }).subscribeOn(Schedulers.io());

  }

  public Observable<Status> retweetStatus(final long tweetId) {
    return Observable.create(new Observable.OnSubscribe<Status>() {
      @Override
      public void call(Subscriber<? super Status> subscriber) {
        try {
          subscriber.onNext(twitter.retweetStatus(tweetId));
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());

  }

  public Observable<Status> createFavorite(final long tweetId) {
    return Observable.create(new Observable.OnSubscribe<Status>() {
      @Override
      public void call(Subscriber<? super Status> subscriber) {
        try {
          subscriber.onNext(twitter.createFavorite(tweetId));
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());

  }

  public Observable<List<Status>> getHomeTimeline() {
    return Observable.create(new Observable.OnSubscribe<List<Status>>() {
      @Override
      public void call(Subscriber<? super List<Status>> subscriber) {
        try {
          subscriber.onNext(twitter.getHomeTimeline());
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<List<Status>> getHomeTimeline(final Paging paging) {
    return Observable.create(new Observable.OnSubscribe<List<Status>>(){
      @Override
      public void call(Subscriber<? super List<Status>> subscriber) {
        try {
          subscriber.onNext(twitter.getHomeTimeline(paging));
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }
}
