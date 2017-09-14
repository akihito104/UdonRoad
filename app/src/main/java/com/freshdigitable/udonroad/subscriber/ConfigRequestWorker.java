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
import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.util.TreeSet;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BiConsumer;
import io.reactivex.processors.PublishProcessor;
import twitter4j.IDs;
import twitter4j.Relationship;

/**
 * ConfigRequestWorker provides to fetch twitter api and to store its data.
 *
 * Created by akihit on 2016/09/23.
 */
public class ConfigRequestWorker implements RequestWorker {
  private final String TAG = ConfigRequestWorker.class.getSimpleName();
  private final TwitterApi twitterApi;
  private final ConfigStore cache;
  private final PublishProcessor<UserFeedbackEvent> userFeedback;

  @Inject
  public ConfigRequestWorker(@NonNull TwitterApi twitterApi,
                             @NonNull ConfigStore configStore,
                             @NonNull PublishProcessor<UserFeedbackEvent> userFeedback) {
    this.twitterApi = twitterApi;
    this.cache = configStore;
    this.userFeedback = userFeedback;
  }

  @Override
  public OnIffabItemSelectedListener getOnIffabItemSelectedListener(long selectedId) {
    return item -> {};
  }

  public Completable setup() {
    return Completable.create(this::storeAllIgnoringUsers);
  }

  private void storeAllIgnoringUsers(CompletableEmitter emitter) {
    Observable.concat(twitterApi.getAllMutesIDs(), twitterApi.getAllBlocksIDs())
        .map(IDs::getIDs)
        .collect(TreeSet::new, (BiConsumer<TreeSet<Long>, long[]>) (collector, ids) -> {
          for (long l : ids) {
            collector.add(l);
          }
        })
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(u -> {
          cache.open();
          cache.replaceIgnoringUsers(u);
          cache.close();
          emitter.onComplete();
        }, emitter::onError);
  }

  public void fetchRelationship(long userId) {
    fetchToStore(twitterApi.showFriendship(userId),
        ConfigStore::upsertRelationship,
        0, R.string.msg_fetch_relationship_failed);
  }

  public void fetchCreateFriendship(long userId) {
    fetchToStore(twitterApi.createFriendship(userId),
        (cache, u) -> cache.updateFriendship(userId, true),
        R.string.msg_create_friendship_success, R.string.msg_create_friendship_failed);
  }

  public void fetchDestroyFriendship(long userId) {
    fetchToStore(twitterApi.destroyFriendship(userId),
        (cache, u) -> cache.updateFriendship(userId, false),
        R.string.msg_destroy_friendship_success, R.string.msg_destroy_friendship_failed);
  }

  public void fetchCreateBlock(long userId) {
    fetchToStore(twitterApi.createBlock(userId),
        (cache, u) -> {
          cache.updateBlocking(userId, true);
          cache.addIgnoringUser(u);
        },
        R.string.msg_create_block_success, R.string.msg_create_block_failed);

  }

  public void fetchDestroyBlock(long userId) {
    fetchToStore(twitterApi.destroyBlock(userId),
        (cache, u) -> {
          cache.updateBlocking(userId, false);
          cache.removeIgnoringUser(u);
        },
        R.string.msg_create_block_success, R.string.msg_create_block_failed);
  }

  public void reportSpam(long userId) {
    fetchToStore(twitterApi.reportSpam(userId),
        ConfigStore::addIgnoringUser,
        R.string.msg_report_spam_success, R.string.msg_report_spam_failed);
  }

  public void fetchCreateMute(long userId) {
    fetchToStore(twitterApi.createMute(userId),
        (cache, u) -> {
          cache.updateMuting(userId, true);
          cache.addIgnoringUser(u);
        },
        R.string.msg_create_mute_success, R.string.msg_create_mute_failed);
  }

  public void fetchDestroyMute(long userId) {
    fetchToStore(twitterApi.destroyMute(userId),
        (cache, u) -> {
          cache.updateMuting(userId, false);
          cache.removeIgnoringUser(u);
        },
        R.string.msg_destroy_mute_success, R.string.msg_destroy_mute_failed);
  }

  public void fetchBlockRetweet(Relationship old) {
    fetchToStore(twitterApi.updateFriendship(old.getTargetUserId(), old.isSourceNotificationsEnabled(), false),
        ConfigStore::upsertRelationship,
        R.string.msg_block_retweet_success, R.string.msg_block_retweet_failed);
  }

  public void fetchUnblockRetweet(Relationship old) {
    fetchToStore(twitterApi.updateFriendship(old.getTargetUserId(), old.isSourceNotificationsEnabled(), true),
        ConfigStore::upsertRelationship,
        R.string.msg_unblock_retweet_success, R.string.msg_unblock_retweet_failed);
  }

  private <T> void fetchToStore(Single<T> fetchTask, BiConsumer<ConfigStore, T> storeTask,
                                @StringRes int successRes, @StringRes int failedRes) {
    Util.fetchToStore(fetchTask, cache, storeTask,
        successRes > 0 ? t -> userFeedback.onNext(new UserFeedbackEvent(successRes)) : t -> {},
        throwable -> userFeedback.onNext(new UserFeedbackEvent(failedRes)));
  }

  public void shrink() {
    cache.open();
    cache.shrink();
    cache.close();
  }
}
