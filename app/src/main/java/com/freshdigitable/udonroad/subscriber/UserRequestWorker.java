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

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.datastore.BaseOperation;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.PublishProcessor;
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
                           @NonNull PublishProcessor<UserFeedbackEvent> feedback) {
    super(twitterApi, userStore, feedback);
  }

  public Single<User> observeCreateFriendship(long userId) {
    return twitterApi.createFriendship(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(createUpsertAction(R.string.msg_create_friendship_success))
        .doOnError(onErrorFeedback(R.string.msg_create_friendship_failed));
  }

  public Single<User> observeDestroyFriendship(long userId) {
    return twitterApi.destroyFriendship(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(createUpsertAction(R.string.msg_destroy_friendship_success))
        .doOnError(onErrorFeedback(R.string.msg_destroy_friendship_failed));
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
  private Consumer<User> createUpsertAction(@StringRes int msg) {
    return user -> {
      cache.upsert(user);
      userFeedback.onNext(new UserFeedbackEvent(msg));
    };
  }

  @NonNull
  private Consumer<List<User>> createUpsertListAction() {
    return users -> cache.upsert(users);
  }
}
