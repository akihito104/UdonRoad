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

import com.freshdigitable.udonroad.Utils;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.schedulers.Schedulers;
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

  public Single<Long> getId() {
    return observeThrowableFetch(twitter::getId);
  }

  public Single<User> verifyCredentials() {
    return observeThrowableFetch(() -> twitter.showUser(twitter.getId()));
  }

  public Single<TwitterAPIConfiguration> getTwitterAPIConfiguration() {
    return observeThrowableFetch(twitter::getAPIConfiguration);
  }

  public Single<Status> updateStatus(final String sendingText) {
    return observeThrowableFetch(() -> twitter.updateStatus(sendingText));
  }

  public Single<Status> updateStatus(final StatusUpdate statusUpdate) {
    return observeThrowableFetch(() -> twitter.updateStatus(statusUpdate));
  }

  public Single<Status> retweetStatus(final long tweetId) {
    return observeThrowableFetch(() -> twitter.retweetStatus(tweetId));
  }

  public Single<Status> createFavorite(final long tweetId) {
    return observeThrowableFetch(() -> twitter.createFavorite(tweetId));
  }

  public Single<List<Status>> getHomeTimeline() {
    return observeThrowableFetch(twitter::getHomeTimeline);
  }

  public Single<List<Status>> getHomeTimeline(final Paging paging) {
    return observeThrowableFetch(() -> twitter.getHomeTimeline(paging));
  }

  public Single<Status> destroyStatus(final long id) {
    return observeThrowableFetch(() -> twitter.destroyStatus(id));
  }

  public Single<Status> destroyFavorite(final long id) {
    return observeThrowableFetch(() -> twitter.destroyFavorite(id));
  }

  public Single<List<Status>> getUserTimeline(final long userId) {
    return observeThrowableFetch(() -> twitter.getUserTimeline(userId));
  }

  public Single<List<Status>> getUserTimeline(final long userId, final Paging paging) {
    return observeThrowableFetch(() -> twitter.getUserTimeline(userId, paging));
  }

  public Single<List<Status>> getFavorites(final long userId) {
    return observeThrowableFetch(() -> twitter.getFavorites(userId));
  }

  public Single<List<Status>> getFavorites(final long userId, final Paging paging) {
    return observeThrowableFetch(() -> twitter.getFavorites(userId, paging));
  }

  public Single<User> createFriendship(final long userId) {
    return observeThrowableFetch(() -> twitter.createFriendship(userId));
  }

  public Single<User> destroyFriendship(final long userId) {
    return observeThrowableFetch(() -> twitter.destroyFriendship(userId));
  }

  public Single<Relationship> updateFriendship(final long userId,
                                               final boolean enableDeviceNotification,
                                               final boolean enableRetweet) {
    return observeThrowableFetch(() ->
        twitter.updateFriendship(userId, enableDeviceNotification, enableRetweet));
  }

  public Single<User> createBlock(final long userId) {
    return observeThrowableFetch(() -> twitter.createBlock(userId));
  }

  public Single<User> destroyBlock(final long userId) {
    return observeThrowableFetch(() -> twitter.destroyBlock(userId));
  }

  public Single<User> reportSpam(final long userId) {
    return observeThrowableFetch(() -> twitter.reportSpam(userId));
  }

  public Single<User> createMute(final long userId) {
    return observeThrowableFetch(() -> twitter.createMute(userId));
  }

  public Single<User> destroyMute(final long userId) {
    return observeThrowableFetch(() -> twitter.destroyMute(userId));
  }

  public Single<PagableResponseList<User>> getFollowersList(final long userId, final long cursor) {
    return observeThrowableFetch(() ->
        twitter.getFollowersList(userId, cursor, 20, true, false));
  }

  public Single<PagableResponseList<User>> getFriendsList(final long userId, final long cursor) {
    return observeThrowableFetch(() ->
        twitter.getFriendsList(userId, cursor, 20, true, false));
  }

  public Observable<IDs> getAllBlocksIDs() {
    return Observable.create((ObservableOnSubscribe<IDs>) subscriber -> {
      try {
        IDs blocksIDs = twitter.getBlocksIDs(-1);
        while (blocksIDs != null && blocksIDs.hasNext()) {
          subscriber.onNext(blocksIDs);
          if (!blocksIDs.hasNext()) {
            break;
          }
          blocksIDs = twitter.getBlocksIDs(blocksIDs.getNextCursor());
        }
        subscriber.onComplete();
      } catch (TwitterException e) {
        subscriber.onError(e);
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<IDs> getAllMutesIDs() {
    return Observable.create((ObservableOnSubscribe<IDs>) subscriber -> {
      try {
        IDs mutesIDs = twitter.getMutesIDs(-1);
        while (mutesIDs != null && mutesIDs.hasNext()) {
          subscriber.onNext(mutesIDs);
          if (!mutesIDs.hasNext()) {
            break;
          }
          mutesIDs = twitter.getMutesIDs(mutesIDs.getNextCursor());
        }
        subscriber.onComplete();
      } catch (TwitterException e) {
        subscriber.onError(e);
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<Status> fetchConversations(long statusId) {
    return Observable.create((ObservableOnSubscribe<Status>) subscriber -> {
      try {
        Status status = twitter.showStatus(statusId);
        while (status != null) {
          subscriber.onNext(status);
          final long inReplyToStatusId = Utils.getBindingStatus(status).getInReplyToStatusId();
          if (inReplyToStatusId > 0) {
            status = twitter.showStatus(inReplyToStatusId);
          } else {
            status = null;
          }
        }
        subscriber.onComplete();
      } catch (TwitterException e) {
        subscriber.onError(e);
      }
    }).subscribeOn(Schedulers.io());
  }

  public Single<Relationship> showFriendship(final long targetId) {
    return observeThrowableFetch(() -> {
      final long sourceId = twitter.getId();
      return twitter.showFriendship(sourceId, targetId);
    });
  }

  interface ThrowableFetch<T> {
    T call() throws Exception;
  }

  private static <T> Single<T> observeThrowableFetch(final ThrowableFetch<T> fetch) {
    return Single.create((SingleOnSubscribe<T>) subscriber -> {
      try {
        final T ret = fetch.call();
        subscriber.onSuccess(ret);
      } catch (Exception e) {
        subscriber.onError(e);
      }
    }).subscribeOn(Schedulers.io());
  }
}
