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
import android.util.Log;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.util.TreeSet;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.subjects.PublishSubject;
import twitter4j.IDs;
import twitter4j.Relationship;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.User;

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
                             @NonNull PublishSubject<UserFeedbackEvent> userFeedback) {
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

  public void setup(Action0 completeAction) {
    Observable.concat(
        verifyCredentials(),
        fetchTwitterAPIConfig(),
        fetchAllIgnoringUsers())
        .subscribe(
            serializable -> { /* nop */ },
            onErrorAction,
            completeAction);
  }

  private Observable<User> verifyCredentials() {
    return twitterApi.verifyCredentials()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(appSettings::addAuthenticatedUser)
        .doOnError(onErrorAction);
  }

  public Observable<User> getAuthenticatedUser() {
    return twitterApi.getId()
        .observeOn(AndroidSchedulers.mainThread())
        .map(appSettings::getAuthenticatedUser);
  }

  private Observable<TwitterAPIConfiguration> fetchTwitterAPIConfig() {
    if (!appSettings.isTwitterAPIConfigFetchable()) {
      Log.d(TAG, "fetchTwitterAPIConfig: not fetch");
      return Observable.empty();
    }
    Log.d(TAG, "fetchTwitterAPIConfig: fetching");
    return twitterApi.getTwitterAPIConfiguration()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(appSettings::setTwitterAPIConfig)
        .doOnError(onErrorAction);
  }

  private final Action1<Throwable> onErrorAction = throwable -> Log.e(TAG, "call: ", throwable);

  private Observable<TreeSet<Long>> fetchAllIgnoringUsers() {
    return Observable.concat(twitterApi.getAllMutesIDs(), twitterApi.getAllBlocksIDs())
        .map(IDs::getIDs)
        .collect(TreeSet::new,
            (Action2<TreeSet<Long>, long[]>) (collector, ids) -> {
              for (long l : ids) {
                collector.add(l);
              }
            })
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(ids -> cache.replaceIgnoringUsers(ids))
        .doOnError(onErrorAction);
  }

  public Observable<Relationship> observeFetchRelationship(long userId) {
    return twitterApi.showFriendship(userId)
        .doOnError(onErrorFeedback(R.string.msg_fetch_relationship_failed));
  }

  public Observable<User> observeCreateBlock(long userId) {
    return twitterApi.createBlock(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(addIgnoringUserAction())
        .doOnError(onErrorFeedback(R.string.msg_create_block_failed))
        .doOnCompleted(onCompleteFeedback(R.string.msg_create_block_success));
  }

  public Observable<User> observeDestroyBlock(long userId) {
    return twitterApi.destroyBlock(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(removeIgnoringUserAction())
        .doOnError(onErrorFeedback(R.string.msg_create_block_failed))
        .doOnCompleted(onCompleteFeedback(R.string.msg_create_block_success));
  }

  public void reportSpam(long userId) {
    twitterApi.reportSpam(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            addIgnoringUserAction(),
            onErrorFeedback(R.string.msg_report_spam_failed),
            onCompleteFeedback(R.string.msg_report_spam_success));
  }

  public Observable<User> observeCreateMute(long userId) {
    return twitterApi.createMute(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(addIgnoringUserAction())
        .doOnError(onErrorFeedback(R.string.msg_create_mute_failed))
        .doOnCompleted(onCompleteFeedback(R.string.msg_create_mute_success));
  }

  public Observable<User> observeDestroyMute(long userId) {
    return twitterApi.destroyMute(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(removeIgnoringUserAction())
        .doOnError(onErrorFeedback(R.string.msg_destroy_mute_failed))
        .doOnCompleted(onCompleteFeedback(R.string.msg_destroy_mute_success));
  }

  public Observable<Relationship> observeBlockRetweet(Relationship old) {
    return twitterApi.updateFriendship(old.getTargetUserId(),
        old.isSourceNotificationsEnabled(), false)
        .doOnError(onErrorFeedback(R.string.msg_block_retweet_failed))
        .doOnCompleted(onCompleteFeedback(R.string.msg_block_retweet_success));
  }

  public Observable<Relationship> observeUnblockRetweet(Relationship old) {
    return twitterApi.updateFriendship(old.getTargetUserId(),
        old.isSourceNotificationsEnabled(), true)
        .doOnError(onErrorFeedback(R.string.msg_unblock_retweet_failed))
        .doOnCompleted(onCompleteFeedback(R.string.msg_unblock_retweet_success));
  }

  @NonNull
  private Action1<User> addIgnoringUserAction() {
    return user -> cache.addIgnoringUser(user);
  }

  @NonNull
  private Action1<User> removeIgnoringUserAction() {
    return user -> cache.removeIgnoringUser(user);
  }
}
