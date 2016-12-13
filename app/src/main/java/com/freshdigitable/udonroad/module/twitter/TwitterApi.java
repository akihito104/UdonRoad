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

package com.freshdigitable.udonroad.module.twitter;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import twitter4j.IDs;
import twitter4j.PagableResponseList;
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
  private final Twitter twitter;

  @Inject
  public TwitterApi(Twitter twitter) {
    this.twitter = twitter;
  }

  public void setOAuthAccessToken(AccessToken accessToken) {
    twitter.setOAuthAccessToken(accessToken);
  }

  public Observable<Long> getId() {
    return observeThrowableFetch(new ThrowableFetch<Long>() {
      @Override
      public Long call() throws Exception {
        return twitter.getId();
      }
    });
  }

  public Observable<User> verifyCredentials() {
    return observeThrowableFetch(new ThrowableFetch<User>() {
      @Override
      public User call() throws Exception {
        return twitter.showUser(twitter.getId());
      }
    });
  }

  public Observable<TwitterAPIConfiguration> getTwitterAPIConfiguration() {
    return observeThrowableFetch(new ThrowableFetch<TwitterAPIConfiguration>() {
      @Override
      public TwitterAPIConfiguration call() throws Exception {
        return twitter.getAPIConfiguration();
      }
    });
  }

  public Observable<Status> updateStatus(final String sendingText) {
    return observeThrowableFetch(new ThrowableFetch<Status>() {
      @Override
      public Status call() throws Exception {
        return twitter.updateStatus(sendingText);
      }
    });
  }

  public Observable<Status> updateStatus(final StatusUpdate statusUpdate) {
    return observeThrowableFetch(new ThrowableFetch<Status>() {
      @Override
      public Status call() throws Exception {
        return twitter.updateStatus(statusUpdate);
      }
    });
  }

  public Observable<Status> retweetStatus(final long tweetId) {
    return observeThrowableFetch(new ThrowableFetch<Status>() {
      @Override
      public Status call() throws Exception {
        return twitter.retweetStatus(tweetId);
      }
    });
  }

  public Observable<Status> createFavorite(final long tweetId) {
    return observeThrowableFetch(new ThrowableFetch<Status>() {
      @Override
      public Status call() throws Exception {
        return twitter.createFavorite(tweetId);
      }
    });
  }

  public Observable<List<Status>> getHomeTimeline() {
    return observeThrowableFetch(new ThrowableFetch<List<Status>>() {
      @Override
      public List<Status> call() throws Exception {
        return twitter.getHomeTimeline();
      }
    });
  }

  public Observable<List<Status>> getHomeTimeline(final Paging paging) {
    return observeThrowableFetch(new ThrowableFetch<List<Status>>(){
      @Override
      public List<Status> call() throws Exception {
        return twitter.getHomeTimeline(paging);
      }
    });
  }

  public Observable<Status> destroyStatus(final long id) {
    return observeThrowableFetch(new ThrowableFetch<Status>() {
      @Override
      public Status call() throws Exception {
        return twitter.destroyStatus(id);
      }
    });
  }

  public Observable<Status> destroyFavorite(final long id) {
    return observeThrowableFetch(new ThrowableFetch<Status>() {
      @Override
      public Status call() throws Exception {
        return twitter.destroyFavorite(id);
      }
    });
  }

  public Observable<List<Status>> getUserTimeline(final long userId) {
    return observeThrowableFetch(new ThrowableFetch<List<Status>>() {
      @Override
      public List<Status> call() throws Exception {
        return twitter.getUserTimeline(userId);
      }
    });
  }

  public Observable<List<Status>> getUserTimeline(final long userId, final Paging paging) {
    return observeThrowableFetch(new ThrowableFetch<List<Status>>() {
      @Override
      public List<Status> call() throws Exception {
        return twitter.getUserTimeline(userId, paging);
      }
    });
  }

  public Observable<List<Status>> getFavorites(final long userId) {
    return observeThrowableFetch(new ThrowableFetch<List<Status>>() {
      @Override
      public List<Status> call() throws Exception {
        return twitter.getFavorites(userId);
      }
    });
  }

  public Observable<List<Status>> getFavorites(final long userId, final Paging paging) {
    return observeThrowableFetch(new ThrowableFetch<List<Status>>() {
      @Override
      public List<Status> call() throws Exception {
        return twitter.getFavorites(userId, paging);
      }
    });
  }

  public Observable<User> createFriendship(final long userId) {
    return observeThrowableFetch(new ThrowableFetch<User>() {
      @Override
      public User call() throws Exception {
        return twitter.createFriendship(userId);
      }
    });
  }

  public Observable<User> destroyFriendship(final long userId) {
    return observeThrowableFetch(new ThrowableFetch<User>() {
      @Override
      public User call() throws Exception {
        return twitter.destroyFriendship(userId);
      }
    });
  }

  public Observable<Relationship> updateFriendship(final long userId,
                                                   final boolean enableDeviceNotification,
                                                   final boolean enableRetweet) {
    return observeThrowableFetch(new ThrowableFetch<Relationship>() {
      @Override
      public Relationship call() throws Exception {
        return twitter.updateFriendship(userId, enableDeviceNotification, enableRetweet);
      }
    });
  }

  public Observable<User> createBlock(final long userId) {
    return observeThrowableFetch(new ThrowableFetch<User>() {
      @Override
      public User call() throws Exception {
        return twitter.createBlock(userId);
      }
    });
  }

  public Observable<User> destroyBlock(final long userId) {
    return observeThrowableFetch(new ThrowableFetch<User>() {
      @Override
      public User call() throws Exception {
        return twitter.destroyBlock(userId);
      }
    });
  }

  public Observable<User> reportSpam(final long userId) {
    return observeThrowableFetch(new ThrowableFetch<User>() {
      @Override
      public User call() throws Exception {
        return twitter.reportSpam(userId);
      }
    });
  }

  public Observable<User> createMute(final long userId) {
    return observeThrowableFetch(new ThrowableFetch<User>() {
      @Override
      public User call() throws Exception {
        return twitter.createMute(userId);
      }
    });
  }

  public Observable<User> destroyMute(final long userId) {
    return observeThrowableFetch(new ThrowableFetch<User>() {
      @Override
      public User call() throws Exception {
        return twitter.destroyMute(userId);
      }
    });
  }

  public Observable<PagableResponseList<User>> getFollowersList(final long userId, final long cursor) {
    return observeThrowableFetch(new ThrowableFetch<PagableResponseList<User>>() {
      @Override
      public PagableResponseList<User> call() throws Exception {
        return twitter.getFollowersList(userId, cursor, 20, true, false);
      }
    });
  }

  public Observable<PagableResponseList<User>> getFriendsList(final long userId, final long cursor) {
    return observeThrowableFetch(new ThrowableFetch<PagableResponseList<User>>() {
      @Override
      public PagableResponseList<User> call() throws Exception {
        return twitter.getFriendsList(userId, cursor, 20, true, false);
      }
    });
  }

  public Observable<IDs> getAllBlocksIDs() {
    return Observable.create(new Observable.OnSubscribe<IDs>() {
      @Override
      public void call(Subscriber<? super IDs> subscriber) {
        try {
          IDs blocksIDs = null;
          while (blocksIDs == null || blocksIDs.hasNext()) {
            final long cursor = blocksIDs == null
                ? -1
                : blocksIDs.getNextCursor();
            blocksIDs = twitter.getBlocksIDs(cursor);
            subscriber.onNext(blocksIDs);
          }
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<IDs> getAllMutesIDs() {
    return Observable.create(new Observable.OnSubscribe<IDs>() {
      @Override
      public void call(Subscriber<? super IDs> subscriber) {
        try {
          IDs mutesIDs = null;
          while (mutesIDs == null || mutesIDs.hasNext()) {
            final long cursor = mutesIDs == null
                ? -1
                : mutesIDs.getNextCursor();
            mutesIDs = twitter.getMutesIDs(cursor);
            subscriber.onNext(mutesIDs);
          }
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<Relationship> showFriendship(final long targetId) {
    return observeThrowableFetch(new ThrowableFetch<Relationship>() {
      @Override
      public Relationship call() throws Exception {
        final long sourceId = twitter.getId();
        return twitter.showFriendship(sourceId, targetId);
      }
    });
  }

  interface ThrowableFetch<T> {
    T call() throws Exception;
  }

  private static <T> Observable<T> observeThrowableFetch(final ThrowableFetch<T> fetch) {
    return Observable.create(new Observable.OnSubscribe<T>() {
      @Override
      public void call(Subscriber<? super T> subscriber) {
        try {
          final T ret = fetch.call();
          subscriber.onNext(ret);
          subscriber.onCompleted();
        } catch (Exception e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }
}
