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

package com.freshdigitable.udonroad.subscriber;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.datastore.BaseOperation;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.util.Date;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import twitter4j.ExtendedMediaEntity;
import twitter4j.GeoLocation;
import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Paging;
import twitter4j.Place;
import twitter4j.RateLimitStatus;
import twitter4j.Scopes;
import twitter4j.Status;
import twitter4j.SymbolEntity;
import twitter4j.TwitterException;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;

/**
 * StatusRequestWorker creates twitter request for status resources and subscribes its response
 * with user feedback.

 * Created by akihit on 2016/08/01.
 */
public class StatusRequestWorker<T extends BaseOperation<Status>>
    extends RequestWorkerBase {
  public static final String TAG = StatusRequestWorker.class.getSimpleName();
  private final T statusStore;

  public StatusRequestWorker(@NonNull TwitterApi twitterApi,
                             @NonNull T statusStore,
                             @NonNull UserFeedbackSubscriber userFeedback) {
    super(twitterApi, userFeedback);
    this.statusStore = statusStore;
  }

  public void fetchHomeTimeline() {
    twitterApi.getHomeTimeline()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            onErrorFeedback(R.string.msg_tweet_not_download));
  }

  public void fetchHomeTimeline(Paging paging) {
    twitterApi.getHomeTimeline(paging)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            onErrorFeedback(R.string.msg_tweet_not_download));
  }

  public void fetchHomeTimeline(long userId) {
    twitterApi.getUserTimeline(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            onErrorFeedback(R.string.msg_tweet_not_download));
  }

  public void fetchHomeTimeline(long userId, @Nullable Paging paging) {
    if (paging == null) {
      fetchHomeTimeline(userId);
      return;
    }
    twitterApi.getUserTimeline(userId, paging)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            onErrorFeedback(R.string.msg_tweet_not_download));
  }

  public void fetchFavorites(long userId) {
    twitterApi.getFavorites(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            onErrorFeedback(R.string.msg_tweet_not_download));
  }

  public void fetchFavorites(long userId, @Nullable Paging paging) {
    if (paging == null) {
      fetchFavorites(userId);
      return;
    }
    twitterApi.getFavorites(userId, paging)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            onErrorFeedback(R.string.msg_tweet_not_download));
  }

  public Observable<Status> observeCreateFavorite(final long statusId) {
    return twitterApi.createFavorite(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(createUpsertAction())
        .doOnError(new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            final int msg = findMessageByTwitterExeption(throwable);
            if (msg == R.string.msg_already_fav) {
              updateStatus();
            }
            userFeedback.offerEvent(msg > 0 ? msg : R.string.msg_fav_create_failed);
          }

          private void updateStatus() {
            final Status status = statusStore.find(statusId);
            if (status == null) {
              return;
            }
            final Status bindingStatus = status.isRetweet() ? status.getRetweetedStatus() : status;
            final MyStatus myStatus = create(bindingStatus);
            myStatus.setFavorited(true);
            myStatus.setRetweeted(bindingStatus.isRetweeted());
            statusStore.forceUpsert(myStatus);
          }
        })
        .doOnCompleted(onCompleteFeedback(R.string.msg_fav_create_success));
  }

  public void createFavorite(long statusId) {
    observeCreateFavorite(statusId)
        .subscribe(StatusRequestWorker.<Status>nopSubscriber());
  }

  public Observable<Status> observeRetweetStatus(final long statusId) {
    return twitterApi.retweetStatus(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(createUpsertAction())
        .doOnError(new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            final int msg = findMessageByTwitterExeption(throwable);
            if (msg == R.string.msg_already_rt) {
              updateStatus();
            }
            userFeedback.offerEvent(msg > 0 ? msg : R.string.msg_rt_create_failed);
          }

          private void updateStatus() {
            final Status status = statusStore.find(statusId);
            if (status == null) {
              return;
            }
            final Status bindingStatus = status.isRetweet() ? status.getRetweetedStatus() : status;
            final MyStatus myStatus = create(bindingStatus);
            myStatus.setRetweeted(true);
            myStatus.setFavorited(bindingStatus.isFavorited());
            statusStore.forceUpsert(myStatus);
          }
        })
        .doOnCompleted(onCompleteFeedback(R.string.msg_rt_create_success));
  }

  public void retweetStatus(long statusId) {
    observeRetweetStatus(statusId)
        .subscribe(StatusRequestWorker.<Status>nopSubscriber());
  }

  public void destroyFavorite(long statusId) {
    twitterApi.destroyFavorite(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new Action1<Status>() {
              @Override
              public void call(Status status) {
                statusStore.forceUpsert(status);
              }
            },
            onErrorFeedback(R.string.msg_fav_delete_failed),
            onCompleteFeedback(R.string.msg_fav_delete_success));
  }

  public void destroyRetweet(long statusId) {
    twitterApi.destroyStatus(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new Action1<Status>() {
              @Override
              public void call(Status status) {
                statusStore.forceUpsert(status);
              }
            },
            onErrorFeedback(R.string.msg_rt_delete_failed),
            onCompleteFeedback(R.string.msg_rt_delete_success));
  }

  public T getStatusStore() {
    return statusStore;
  }

  @NonNull
  private Action1<List<Status>> createListUpsertAction() {
    return new Action1<List<Status>>() {
      @Override
      public void call(List<Status> statuses) {
        statusStore.upsert(statuses);
      }
    };
  }

  @NonNull
  private Action1<Status> createUpsertAction() {
    return new Action1<Status>() {
      @Override
      public void call(Status statuses) {
        statusStore.upsert(statuses);
      }
    };
  }

  public static <T> Subscriber<T> nopSubscriber() {
    return new Subscriber<T>() {
      @Override
      public void onCompleted() {
      }

      @Override
      public void onError(Throwable e) {
      }

      @Override
      public void onNext(T o) {
      }
    };
  }

  @StringRes
  private static int findMessageByTwitterExeption(@NonNull Throwable throwable) {
    if (!(throwable instanceof TwitterException)) {
      return 0;
    }
    final TwitterException te = (TwitterException) throwable;
    final int statusCode = te.getStatusCode();
    final int errorCode = te.getErrorCode();
    if (statusCode == 403 && errorCode == 139) {
      return R.string.msg_already_fav;
    } else if (statusCode == 403 && errorCode == 327) {
      return R.string.msg_already_rt;
    }
    Log.d(TAG, "not registered exception: ", throwable);
    return 0;
  }

  private static abstract class MyStatus implements Status {
    boolean favorited;
    boolean retweeted;

    void setFavorited(boolean favorited) {
      this.favorited = favorited;
    }

    void setRetweeted(boolean retweeted) {
      this.retweeted = retweeted;
    }
  }

  private MyStatus create(final Status status) {
    return new MyStatus() {
      @Override
      public Date getCreatedAt() {
        return status.getCreatedAt();
      }

      @Override
      public long getId() {
        return status.getId();
      }

      @Override
      public String getText() {
        return status.getText();
      }

      @Override
      public String getSource() {
        return status.getSource();
      }

      @Override
      public boolean isTruncated() {
        return status.isTruncated();
      }

      @Override
      public long getInReplyToStatusId() {
        return status.getInReplyToStatusId();
      }

      @Override
      public long getInReplyToUserId() {
        return status.getInReplyToUserId();
      }

      @Override
      public String getInReplyToScreenName() {
        return status.getInReplyToScreenName();
      }

      @Override
      public GeoLocation getGeoLocation() {
        return status.getGeoLocation();
      }

      @Override
      public Place getPlace() {
        return status.getPlace();
      }

      @Override
      public boolean isFavorited() {
        return super.favorited;
      }

      @Override
      public boolean isRetweeted() {
        return super.retweeted;
      }

      @Override
      public int getFavoriteCount() {
        return status.getFavoriteCount();
      }

      @Override
      public User getUser() {
        return status.getUser();
      }

      @Override
      public boolean isRetweet() {
        return status.isRetweet();
      }

      @Override
      public Status getRetweetedStatus() {
        return status.getRetweetedStatus();
      }

      @Override
      public long[] getContributors() {
        return status.getContributors();
      }

      @Override
      public int getRetweetCount() {
        return status.getRetweetCount();
      }

      @Override
      public boolean isRetweetedByMe() {
        return status.isRetweetedByMe();
      }

      @Override
      public long getCurrentUserRetweetId() {
        return status.getCurrentUserRetweetId();
      }

      @Override
      public boolean isPossiblySensitive() {
        return status.isPossiblySensitive();
      }

      @Override
      public String getLang() {
        return status.getLang();
      }

      @Override
      public Scopes getScopes() {
        return status.getScopes();
      }

      @Override
      public String[] getWithheldInCountries() {
        return status.getWithheldInCountries();
      }

      @Override
      public long getQuotedStatusId() {
        return status.getQuotedStatusId();
      }

      @Override
      public Status getQuotedStatus() {
        return status.getQuotedStatus();
      }

      @Override
      public int compareTo(@NonNull Status other) {
        return status.compareTo(other);
      }

      @Override
      public UserMentionEntity[] getUserMentionEntities() {
        return status.getUserMentionEntities();
      }

      @Override
      public URLEntity[] getURLEntities() {
        return status.getURLEntities();
      }

      @Override
      public HashtagEntity[] getHashtagEntities() {
        return status.getHashtagEntities();
      }

      @Override
      public MediaEntity[] getMediaEntities() {
        return status.getMediaEntities();
      }

      @Override
      public ExtendedMediaEntity[] getExtendedMediaEntities() {
        return status.getExtendedMediaEntities();
      }

      @Override
      public SymbolEntity[] getSymbolEntities() {
        return status.getSymbolEntities();
      }

      @Override
      public RateLimitStatus getRateLimitStatus() {
        return status.getRateLimitStatus();
      }

      @Override
      public int getAccessLevel() {
        return status.getAccessLevel();
      }
    };
  }
}