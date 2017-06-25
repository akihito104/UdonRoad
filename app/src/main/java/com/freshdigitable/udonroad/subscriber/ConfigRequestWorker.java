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
import android.util.Log;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.PublishProcessor;
import twitter4j.IDs;
import twitter4j.Relationship;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.User;
import twitter4j.auth.AccessToken;

/**
 * ConfigRequestWorker provides to fetch twitter api and to store its data.
 *
 * Created by akihit on 2016/09/23.
 */
public class ConfigRequestWorker extends RequestWorkerBase<ConfigStore> {
  private final String TAG = ConfigRequestWorker.class.getSimpleName();
  private final AppSettingStore appSettings;

  @Inject
  public ConfigRequestWorker(@NonNull TwitterApi twitterApi,
                             @NonNull ConfigStore configStore,
                             @NonNull AppSettingStore appSettings,
                             @NonNull PublishProcessor<UserFeedbackEvent> userFeedback) {
    super(twitterApi, configStore, userFeedback);
    this.appSettings = appSettings;
  }

  public void open() {
    super.open();
    appSettings.open();
  }

  @Override
  public void close() {
    super.close();
    appSettings.close();
  }

  public void setup(Action completeAction) {
    if (!isAuthenticated()) return;
    Completable.concatArray(
        Completable.fromSingle(verifyCredentials()),
        Completable.fromSingle(fetchTwitterAPIConfig()),
        Completable.fromSingle(fetchAllIgnoringUsers()))
        .subscribe(completeAction, onErrorAction);
  }

  private boolean isAuthenticated() {
    final AccessToken token = appSettings.getCurrentUserAccessToken();
    return token != null;
  }

  private Single<User> verifyCredentials() {
    return twitterApi.verifyCredentials()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(appSettings::addAuthenticatedUser)
        .doOnError(onErrorAction);
  }

  public Single<User> getAuthenticatedUser() {
    return isAuthenticated()
        ? twitterApi.getId().observeOn(AndroidSchedulers.mainThread())
        .map(appSettings::getAuthenticatedUser)
        : Single.never();
  }

  private Single<TwitterAPIConfiguration> fetchTwitterAPIConfig() {
    if (!appSettings.isTwitterAPIConfigFetchable()) {
      Log.d(TAG, "fetchTwitterAPIConfig: not fetch");
      return Single.never();
    }
    Log.d(TAG, "fetchTwitterAPIConfig: fetching");
    return twitterApi.getTwitterAPIConfiguration()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(appSettings::setTwitterAPIConfig)
        .doOnError(onErrorAction);
  }

  private final Consumer<Throwable> onErrorAction = throwable -> Log.e(TAG, "call: ", throwable);

  private Single<? extends Set<Long>> fetchAllIgnoringUsers() {
    return Observable.concat(twitterApi.getAllMutesIDs(), twitterApi.getAllBlocksIDs())
        .map(IDs::getIDs)
        .collect(TreeSet::new, (BiConsumer<TreeSet<Long>, long[]>) (collector, ids) -> {
          for (long l : ids) {
            collector.add(l);
          }
        })
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(ids -> cache.replaceIgnoringUsers(ids))
        .doOnError(onErrorAction);
  }

  public Single<Relationship> observeFetchRelationship(long userId) {
    return twitterApi.showFriendship(userId)
        .doOnError(onErrorFeedback(R.string.msg_fetch_relationship_failed));
  }

  public Single<User> observeCreateBlock(long userId) {
    return twitterApi.createBlock(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(addIgnoringUserAction(R.string.msg_create_block_success))
        .doOnError(onErrorFeedback(R.string.msg_create_block_failed));
  }

  public Single<User> observeDestroyBlock(long userId) {
    return twitterApi.destroyBlock(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(removeIgnoringUserAction(R.string.msg_create_block_success))
        .doOnError(onErrorFeedback(R.string.msg_create_block_failed));
  }

  public void reportSpam(long userId) {
    twitterApi.reportSpam(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            addIgnoringUserAction(R.string.msg_report_spam_success),
            onErrorFeedback(R.string.msg_report_spam_failed));
  }

  public Single<User> observeCreateMute(long userId) {
    return twitterApi.createMute(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(addIgnoringUserAction(R.string.msg_create_mute_success))
        .doOnError(onErrorFeedback(R.string.msg_create_mute_failed));
  }

  public Single<User> observeDestroyMute(long userId) {
    return twitterApi.destroyMute(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(removeIgnoringUserAction(R.string.msg_destroy_mute_success))
        .doOnError(onErrorFeedback(R.string.msg_destroy_mute_failed));
  }

  public Single<Relationship> observeBlockRetweet(Relationship old) {
    return twitterApi.updateFriendship(old.getTargetUserId(),
        old.isSourceNotificationsEnabled(), false)
        .doOnSuccess(r -> userFeedback.onNext(new UserFeedbackEvent(R.string.msg_block_retweet_success)))
        .doOnError(onErrorFeedback(R.string.msg_block_retweet_failed));
  }

  public Single<Relationship> observeUnblockRetweet(Relationship old) {
    return twitterApi.updateFriendship(old.getTargetUserId(),
        old.isSourceNotificationsEnabled(), true)
        .doOnSuccess(r -> userFeedback.onNext(new UserFeedbackEvent(R.string.msg_unblock_retweet_success)))
        .doOnError(onErrorFeedback(R.string.msg_unblock_retweet_failed));
  }

  @NonNull
  private Consumer<User> addIgnoringUserAction(@StringRes int msg) {
    return user -> {
      cache.addIgnoringUser(user);
      userFeedback.onNext(new UserFeedbackEvent(msg));
    };
  }

  @NonNull
  private Consumer<User> removeIgnoringUserAction(@StringRes int msg) {
    return user -> {
      cache.removeIgnoringUser(user);
      userFeedback.onNext(new UserFeedbackEvent(msg));
    };
  }

  public void shrink() {
    cache.shrink();
  }
}
