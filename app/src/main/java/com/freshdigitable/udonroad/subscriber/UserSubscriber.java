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
import android.support.annotation.StringRes;
import android.view.View;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.datastore.BaseOperation;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import twitter4j.User;

/**
 * UserSubscriber creates twitter request for user resources and subscribes its response
 * with user feedback.
 *
 * Created by akihit on 2016/09/03.
 */
public class UserSubscriber<T extends BaseOperation<User>> {
  private final TwitterApi twitterApi;
  private final T userStore;
  private final UserFeedbackSubscriber feedback;

  public UserSubscriber(@NonNull TwitterApi twitterApi,
                        @NonNull T userStore,
                        @NonNull UserFeedbackSubscriber feedback) {
    this.twitterApi = twitterApi;
    this.userStore = userStore;
    this.feedback = feedback;
  }

  public void registerRootView(View view) {
    feedback.registerRootView(view);
  }

  public void unregisterRootView() {
    feedback.unregisterRootView();
  }

  public void close() {
    feedback.reset();
  }

  public void createFriendship(final long userId) {
    twitterApi.createFriendship(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createUpsertAction(),
            onErrorAction(R.string.msg_create_friendship_failed),
            onCompleteAction(R.string.msg_create_friendship_success));
  }

  public void destroyFriendship(long userId) {
    twitterApi.destroyFriendship(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createUpsertAction(),
            onErrorAction(R.string.msg_destroy_friendship_failed),
            onCompleteAction(R.string.msg_destroy_friendship_success));
  }

  public void fetchFollowers(final long userId, final long cursor) {
    twitterApi.getFollowersList(userId, cursor)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(createUpsertListAction(),
            onErrorAction(R.string.msg_follower_list_failed));
  }

  public void fetchFriends(long userId, long cursor) {
    twitterApi.getFriendsList(userId, cursor)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(createUpsertListAction(),
            onErrorAction(R.string.msg_friends_list_failed));
  }

  @NonNull
  private Action1<User> createUpsertAction() {
    return new Action1<User>() {
      @Override
      public void call(User user) {
        userStore.upsert(user);
      }
    };
  }

  @NonNull
  private Action1<List<User>> createUpsertListAction() {
    return new Action1<List<User>>() {
      @Override
      public void call(List<User> users) {
        userStore.upsert(users);
      }
    };
  }

  @NonNull
  private Action1<Throwable> onErrorAction(@StringRes final int msg) {
    return new Action1<Throwable>() {
      @Override
      public void call(Throwable throwable) {
        feedback.offerEvent(msg);
      }
    };
  }

  @NonNull
  private Action0 onCompleteAction(@StringRes final int msg) {
    return new Action0() {
      @Override
      public void call() {
        feedback.offerEvent(msg);
      }
    };
  }
}
