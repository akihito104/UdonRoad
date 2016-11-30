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

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.datastore.BaseOperation;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.PublishSubject;
import twitter4j.User;

/**
 * UserRequestWorker creates twitter request for user resources and subscribes its response
 * with user feedback.
 * <p>
 * Created by akihit on 2016/09/03.
 */
public class UserRequestWorker<T extends BaseOperation<User>>
    extends RequestWorkerBase<T> {

  @Inject
  public UserRequestWorker(@NonNull TwitterApi twitterApi,
                           @NonNull T userStore,
                           @NonNull PublishSubject<Integer> feedback) {
    super(twitterApi, userStore, feedback);
  }

  public void createFriendship(final long userId) {
    observeCreateFriendship(userId).subscribe(RequestWorkerBase.<User>nopSubscriber());
  }

  public Observable<User> observeCreateFriendship(long userId) {
    return twitterApi.createFriendship(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(createUpsertAction())
        .doOnError(onErrorFeedback(R.string.msg_create_friendship_failed))
        .doOnCompleted(onCompleteFeedback(R.string.msg_create_friendship_success));
  }

  public void destroyFriendship(long userId) {
    observeDestroyFriendship(userId).subscribe(RequestWorkerBase.<User>nopSubscriber());
  }

  public Observable<User> observeDestroyFriendship(long userId) {
    return twitterApi.destroyFriendship(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(createUpsertAction())
        .doOnError(onErrorFeedback(R.string.msg_destroy_friendship_failed))
        .doOnCompleted(onCompleteFeedback(R.string.msg_destroy_friendship_success));
  }

  public void fetchFollowers(final long userId, final long cursor) {
    twitterApi.getFollowersList(userId, cursor)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(createUpsertListAction(),
            onErrorFeedback(R.string.msg_follower_list_failed));
  }

  public void fetchFriends(long userId, long cursor) {
    twitterApi.getFriendsList(userId, cursor)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(createUpsertListAction(),
            onErrorFeedback(R.string.msg_friends_list_failed));
  }

  @NonNull
  private Action1<User> createUpsertAction() {
    return new Action1<User>() {
      @Override
      public void call(User user) {
        cache.upsert(user);
      }
    };
  }

  @NonNull
  private Action1<List<User>> createUpsertListAction() {
    return new Action1<List<User>>() {
      @Override
      public void call(List<User> users) {
        cache.upsert(users);
      }
    };
  }
}
