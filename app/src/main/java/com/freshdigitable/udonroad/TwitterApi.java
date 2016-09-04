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

import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import twitter4j.Paging;
import twitter4j.Relationship;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.TwitterException;
import twitter4j.User;
import twitter4j.auth.AccessToken;

/**
 * Wrapper for Twitter API
 *
 * Created by akihit on 15/10/20.
 */
public class TwitterApi {
  @SuppressWarnings("unused")
  private static final String TAG = TwitterApi.class.getSimpleName();
  private static final String TOKEN = "token";
  private static final String TOKEN_SECRET = "token_secret";

  private final Twitter twitter;
  private final SharedPreferences sharedPreference;

  @Inject
  public TwitterApi(Twitter twitter, SharedPreferences sprefs) {
    this.twitter = twitter;
    this.sharedPreference = sprefs;
  }

  public boolean loadAccessToken() {
    final AccessToken accessToken = loadAccessToken(sharedPreference);
    if (accessToken == null) {
      return false;
    }
    twitter.setOAuthAccessToken(accessToken);
    return true;
  }

  public Observable<User> verifyCredentials() {
    return Observable.create(new Observable.OnSubscribe<User>() {
      @Override
      public void call(Subscriber<? super User> subscriber) {
        try {
          User user = twitter.users().showUser(twitter.getId());
          subscriber.onNext(user);
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<TwitterAPIConfiguration> getTwitterAPIConfiguration() {
    return Observable.create(new Observable.OnSubscribe<TwitterAPIConfiguration>() {
      @Override
      public void call(Subscriber<? super TwitterAPIConfiguration> subscriber) {
        try {
          subscriber.onNext(twitter.getAPIConfiguration());
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public static void storeAccessToken(SharedPreferences sharedPreferences, AccessToken token) {
    final SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString(TOKEN, token.getToken());
    editor.putString(TOKEN_SECRET, token.getTokenSecret());
    editor.apply();
  }

  @Nullable
  public static AccessToken loadAccessToken(SharedPreferences sharedPreference) {
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

  public Observable<Status> updateStatus(final String sendingText) {
    return Observable.create(new Observable.OnSubscribe<Status>() {
      @Override
      public void call(Subscriber<? super Status> subscriber) {
        try {
          subscriber.onNext(twitter.updateStatus(sendingText));
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<Status> updateStatus(final StatusUpdate statusUpdate) {
    return Observable.create(new Observable.OnSubscribe<Status>() {
      @Override
      public void call(Subscriber<? super Status> subscriber) {
        try {
          subscriber.onNext(twitter.updateStatus(statusUpdate));
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<Status> retweetStatus(final long tweetId) {
    return Observable.create(new Observable.OnSubscribe<Status>() {
      @Override
      public void call(Subscriber<? super Status> subscriber) {
        try {
          subscriber.onNext(twitter.retweetStatus(tweetId));
          subscriber.onCompleted();
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

  public Observable<Status> destroyStatus(final long id) {
    return Observable.create(new Observable.OnSubscribe<Status>() {
      @Override
      public void call(Subscriber<? super Status> subscriber) {
        try {
          final Status status = twitter.destroyStatus(id);
          subscriber.onNext(status);
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<Status> destroyFavorite(final long id) {
    return Observable.create(new Observable.OnSubscribe<Status>() {
      @Override
      public void call(Subscriber<? super Status> subscriber) {
        try {
          final Status status = twitter.destroyFavorite(id);
          subscriber.onNext(status);
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<List<Status>> getUserTimeline(final User user) {
    final long userId = user.getId();
    return getUserTimeline(userId);
  }

  public Observable<List<Status>> getUserTimeline(final long userId) {
    return Observable.create(new Observable.OnSubscribe<List<Status>>() {
      @Override
      public void call(Subscriber<? super List<Status>> subscriber) {
        try {
          subscriber.onNext(twitter.getUserTimeline(userId));
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<List<Status>> getUserTimeline(final long userId, final Paging paging) {
    return Observable.create(new Observable.OnSubscribe<List<Status>>() {
      @Override
      public void call(Subscriber<? super List<Status>> subscriber) {
        try {
          subscriber.onNext(twitter.getUserTimeline(userId, paging));
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<List<Status>> getFavorites(final long userId) {
    return Observable.create(new Observable.OnSubscribe<List<Status>>() {
      @Override
      public void call(Subscriber<? super List<Status>> subscriber) {
        try {
          subscriber.onNext(twitter.getFavorites(userId));
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<List<Status>> getFavorites(final long userId, final Paging paging) {
    return Observable.create(new Observable.OnSubscribe<List<Status>>() {
      @Override
      public void call(Subscriber<? super List<Status>> subscriber) {
        try {
          subscriber.onNext(twitter.getFavorites(userId, paging));
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<User> createFriendship(final long userId) {
    return Observable.create(new Observable.OnSubscribe<User>() {
      @Override
      public void call(Subscriber<? super User> subscriber) {
        try {
          final User friendship = twitter.createFriendship(userId);
          subscriber.onNext(friendship);
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<User> destroyFriendship(final long userId) {
    return Observable.create(new Observable.OnSubscribe<User>() {
      @Override
      public void call(Subscriber<? super User> subscriber) {
        try {
          final User user = twitter.destroyFriendship(userId);
          subscriber.onNext(user);
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<Relationship> updateFriendship(final long userId,
                                                   final boolean enableDeviceNotification,
                                                   final boolean enableRetweet) {
    return Observable.create(new Observable.OnSubscribe<Relationship>() {
      @Override
      public void call(Subscriber<? super Relationship> subscriber) {
        try {
          final Relationship user = twitter.updateFriendship(userId, enableDeviceNotification, enableRetweet);
          subscriber.onNext(user);
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<User> createBlock(final long userId) {
    return Observable.create(new Observable.OnSubscribe<User>() {
      @Override
      public void call(Subscriber<? super User> subscriber) {
        try {
          final User blocked = twitter.createBlock(userId);
          subscriber.onNext(blocked);
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<User> destroyBlock(final long userId) {
    return Observable.create(new Observable.OnSubscribe<User>() {
      @Override
      public void call(Subscriber<? super User> subscriber) {
        try {
          final User blocked = twitter.destroyBlock(userId);
          subscriber.onNext(blocked);
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<User> reportSpam(final long userId) {
    return Observable.create(new Observable.OnSubscribe<User>() {
      @Override
      public void call(Subscriber<? super User> subscriber) {
        try {
          final User reported = twitter.reportSpam(userId);
          subscriber.onNext(reported);
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<User> createMute(final long userId) {
    return Observable.create(new Observable.OnSubscribe<User>() {
      @Override
      public void call(Subscriber<? super User> subscriber) {
        try {
          final User muted = twitter.createMute(userId);
          subscriber.onNext(muted);
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<User> destroyMute(final long userId) {
    return Observable.create(new Observable.OnSubscribe<User>() {
      @Override
      public void call(Subscriber<? super User> subscriber) {
        try {
          final User muted = twitter.destroyMute(userId);
          subscriber.onNext(muted);
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Twitter getTwitter() {
    return twitter;
  }
}
