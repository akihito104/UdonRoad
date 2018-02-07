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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.freshdigitable.udonroad.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import twitter4j.IDs;
import twitter4j.PagableResponseList;
import twitter4j.Paging;
import twitter4j.Query;
import twitter4j.Relationship;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.TwitterException;
import twitter4j.UploadedMedia;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

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

  public Single<RequestToken> fetchOAuthRequestToken() {
    return observeThrowableFetch(() -> twitter.getOAuthRequestToken("oob"));
  }

  public Single<AccessToken> fetchOAuthAccessToken(RequestToken requestToken, String verifier) {
    return observeThrowableFetch(() -> twitter.getOAuthAccessToken(requestToken, verifier));
  }

  public Single<Long> getId() {
    return observeThrowableFetch(twitter::getId);
  }

  public Single<User> verifyCredentials() {
    return observeThrowableFetch(twitter::verifyCredentials);
  }

  public Single<TwitterAPIConfiguration> getTwitterAPIConfiguration() {
    return observeThrowableFetch(twitter::getAPIConfiguration);
  }

  public Single<Status> showStatus(long statusId) {
    return observeThrowableFetch(() -> twitter.showStatus(statusId));
  }

  public Single<Status> updateStatus(final String sendingText) {
    return observeThrowableFetch(() -> twitter.updateStatus(sendingText));
  }

  public Single<Status> updateStatus(final StatusUpdate statusUpdate) {
    return observeThrowableFetch(() -> twitter.updateStatus(statusUpdate));
  }

  public Single<Status> updateStatus(Context context, StatusUpdate statusUpdate, List<Uri> media) {
    return Observable.fromIterable(media)
        .map(uri -> uploadMedia(context, uri))
        .collectInto(new ArrayList<UploadedMedia>(media.size()), ArrayList::add)
        .map(uploadedMedia -> {
          final long[] ids = new long[uploadedMedia.size()];
          for (int i = 0; i < ids.length; i++) {
            ids[i] = uploadedMedia.get(i).getMediaId();
          }
          statusUpdate.setMediaIds(ids);
          return twitter.updateStatus(statusUpdate);
        })
        .subscribeOn(Schedulers.io());
  }

  private UploadedMedia uploadMedia(Context context, Uri uri) throws TwitterException, FileNotFoundException {
    if (uri.getScheme().startsWith("file")) {
      return twitter.uploadMedia(new File(uri.getPath()));
    } else if (uri.getScheme().startsWith("content")) {
      final ContentResolver contentResolver = context.getContentResolver();
      final String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
      try (final Cursor cursor = contentResolver.query(uri, projection, null, null, null)) {
        final String fileName = (cursor != null && cursor.moveToFirst()) ? cursor.getString(0) : "";
        return twitter.uploadMedia(fileName, contentResolver.openInputStream(uri));
      }
    }
    throw new IllegalStateException();
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
    return Observable.<IDs>create(subscriber -> {
      try {
        IDs blocksIDs = twitter.getBlocksIDs(-1);
        while (blocksIDs != null) {
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
    return Observable.<IDs>create(subscriber -> {
      try {
        IDs mutesIDs = twitter.getMutesIDs(-1);
        while (mutesIDs != null) {
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
    return Observable.<Status>create(subscriber -> {
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

  public Single<List<Status>> fetchSearch(Query query) {
    return observeThrowableFetch(() -> twitter.search(query)).map(QueryResultList::new);
  }

  public Single<PagableResponseList<UserList>> fetchUserListsOwnerships(
      long ownerId, int count, long cursor) {
    return observeThrowableFetch(() ->
        twitter.getUserListsOwnerships(ownerId, count, cursor));
  }

  public Single<List<Status>> fetchUserListsStatuses(long listId, Paging paging) {
    return observeThrowableFetch(() -> twitter.getUserListStatuses(listId, paging));
  }

  public Single<PagableResponseList<UserList>> getUserListMemberships(
      long ownerUserId, int count, long cursor) {
    return observeThrowableFetch(() ->
        twitter.getUserListMemberships(ownerUserId, count, cursor));
  }

  private static <T> Single<T> observeThrowableFetch(final Callable<T> fetch) {
    return Single.<T>create(subscriber -> {
      try {
        final T ret = fetch.call();
        subscriber.onSuccess(ret);
      } catch (Exception e) {
        subscriber.onError(e);
      }
    }).subscribeOn(Schedulers.io());
  }
}
