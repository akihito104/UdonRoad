/*
 * Copyright (c) 2016. Akihito Matsuda (akihito104)
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

import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import com.freshdigitable.udonroad.datastore.TimelineStore;

import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import twitter4j.Paging;
import twitter4j.Status;

/**
 * Created by akihit on 2016/08/01.
 */
public class TwitterApiUtil {
  public static final String TAG = TwitterApiUtil.class.getSimpleName();
  private final TwitterApi twitterApi;
  private final TimelineStore timelineStore;
  private final UserFeedback userFeedback;

  public TwitterApiUtil(@NonNull TwitterApi twitterApi,
                        @NonNull TimelineStore timelineStore,
                        @NonNull UserFeedback userFeedback) {
    this.twitterApi = twitterApi;
    this.timelineStore = timelineStore;
    this.userFeedback = userFeedback;
  }

  public void fetchHomeTimeline() {
    twitterApi.getHomeTimeline()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            userFeedback.onErrorDefault("tweet was not downloaded..."));
  }

  public void fetchHomeTimeline(Paging paging) {
    twitterApi.getHomeTimeline(paging)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            userFeedback.onErrorDefault("tweet was not downloaded..."));
  }

  public void fetchHomeTimeline(long userId) {
    twitterApi.getUserTimeline(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            userFeedback.onErrorDefault("tweet was not downloaded..."));
  }

  public void fetchHomeTimeline(long userId, Paging paging) {
    twitterApi.getUserTimeline(userId, paging)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            userFeedback.onErrorDefault("tweet was not downloaded..."));
  }

  public void fetchFavorites(long userId) {
    twitterApi.getFavorites(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            userFeedback.onErrorDefault("favorites was not downloaded..."));
  }

  public void fetchFavorites(long userId, Paging paging) {
    twitterApi.getFavorites(userId, paging)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            userFeedback.onErrorDefault("favorites was not downloaded..."));
  }

  public void createFavorite(long statusId) {
    twitterApi.createFavorite(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createUpsertAction(),
            userFeedback.onErrorDefault("failed fav..."),
            userFeedback.onCompleteDefault("success to fav"));
  }

  public void retweetStatus(long statusId) {
    twitterApi.retweetStatus(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createUpsertAction(),
            userFeedback.onErrorDefault("failed RT..."),
            userFeedback.onCompleteDefault("success to RT"));
  }

  public TimelineStore getTimelineStore() {
    return timelineStore;
  }

  public Observable<Integer> subscribeInsertEvent() {
    return timelineStore.subscribeInsertEvent();
  }

  public Observable<Integer> subscribeUpdateEvent() {
    return timelineStore.subscribeUpdateEvent();
  }

  public Observable<Integer> subscribeDeleteEvent() {
    return timelineStore.subscribeDeleteEvent();
  }

  @NonNull
  private Action1<List<Status>> createListUpsertAction() {
    return new Action1<List<Status>>() {
      @Override
      public void call(List<Status> statuses) {
        timelineStore.upsert(statuses);
      }
    };
  }

  @NonNull
  private Action1<Status> createUpsertAction() {
    return new Action1<Status>() {
      @Override
      public void call(Status statuses) {
        timelineStore.upsert(statuses);
      }
    };
  }

  public interface UserFeedback {
    Action1<Throwable> onErrorDefault(String msg);

    Action0 onCompleteDefault(String msg);
  }

  public static class SimpleUserFeedback implements UserFeedback {
    @Override
    public Action1<Throwable> onErrorDefault(final String msg) {
      return new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
          Log.e(TAG, "message: " + msg, throwable);
        }
      };
    }

    @Override
    public Action0 onCompleteDefault(final String msg) {
      return new Action0() {
        @Override
        public void call() {
          Log.d(TAG, "call: " + msg);
        }
      };
    }
  }

  public static class SnackbarFeedback implements UserFeedback {
    private final View root;

    public SnackbarFeedback(View root) {
      this.root = root;
    }

    @Override
    public Action1<Throwable> onErrorDefault(final String msg) {
      return new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
          SnackbarUtil.show(root, msg);
          Log.e(TAG, "msg: " + msg, throwable);
        }
      };
    }

    @Override
    public Action0 onCompleteDefault(String msg) {
      return SnackbarUtil.action(root, msg);
    }
  }
}
