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

import android.support.annotation.NonNull;

import com.freshdigitable.udonroad.datastore.UserCapable;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import twitter4j.User;

/**
 * UserSubscriber creates twitter request for user resources and subscribes its response
 * with user feedback.
 *
 * Created by akihit on 2016/09/03.
 */
public class UserSubscriber<T extends UserCapable> {
  private final TwitterApi twitterApi;
  private final T userStore;
  private final FeedbackSubscriber feedback;

  public UserSubscriber(@NonNull TwitterApi twitterApi,
                        @NonNull T userStore,
                        @NonNull FeedbackSubscriber feedback) {
    this.twitterApi = twitterApi;
    this.userStore = userStore;
    this.feedback = feedback;
  }

  public void createFriendship(final long userId) {
    twitterApi.createFriendship(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createUpsertAction(),
            feedback.onErrorDefault(R.string.msg_create_friendship_failed),
            feedback.onCompleteDefault(R.string.msg_create_friendship_success));
  }

  public void destroyFriendship(long userId) {
    twitterApi.destroyFriendship(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createUpsertAction(),
            feedback.onErrorDefault(R.string.msg_destroy_friendship_failed),
            feedback.onCompleteDefault(R.string.msg_destroy_friendship_success));
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

  public void createBlock(final long userId) {
    twitterApi.createBlock(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createUpsertAction(),
            feedback.onErrorDefault(R.string.msg_create_block_failed),
            feedback.onCompleteDefault(R.string.msg_create_block_success));
  }

  public void destroyBlock(final long userId) {
    twitterApi.destroyBlock(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createUpsertAction(),
            feedback.onErrorDefault(R.string.msg_create_block_failed),
            feedback.onCompleteDefault(R.string.msg_create_block_success));
  }

  public void reportSpam(long userId) {
    twitterApi.reportSpam(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createUpsertAction(),
            feedback.onErrorDefault(R.string.msg_report_spam_failed),
            feedback.onCompleteDefault(R.string.msg_report_spam_success));
  }

  public void createMute(long userId) {
    twitterApi.createMute(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createUpsertAction(),
            feedback.onErrorDefault(R.string.msg_create_mute_failed),
            feedback.onCompleteDefault(R.string.msg_create_mute_success));
  }
}