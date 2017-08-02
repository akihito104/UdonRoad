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
import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.util.TreeSet;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
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
        }, th -> {
          Log.e(TAG, "call: ", th);
          emitter.onError(th);
        });
  }

  public void fetchRelationship(long userId) {
    twitterApi.showFriendship(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(r -> {
              cache.open();
              cache.upsertRelationship(r);
              cache.close();
            },
            onErrorFeedback(R.string.msg_fetch_relationship_failed));
  }

  public void fetchCreateFriendship(long userId) {
    twitterApi.createFriendship(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(u -> {
              cache.open();
              cache.updateFriendship(userId, true);
              cache.close();
              userFeedback.onNext(new UserFeedbackEvent(R.string.msg_create_friendship_success));
            },
            onErrorFeedback(R.string.msg_create_friendship_failed));
  }

  public void fetchDestroyFriendship(long userId) {
    twitterApi.destroyFriendship(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(u -> {
              cache.open();
              cache.updateFriendship(userId, false);
              cache.close();
              userFeedback.onNext(new UserFeedbackEvent(R.string.msg_destroy_friendship_success));
            },
            onErrorFeedback(R.string.msg_destroy_friendship_failed));
  }

  public void fetchCreateBlock(long userId) {
    twitterApi.createBlock(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(u -> {
              cache.open();
              cache.updateBlocking(userId, true);
              cache.addIgnoringUser(u);
              cache.close();
              userFeedback.onNext(new UserFeedbackEvent(R.string.msg_create_block_success));
            },
            onErrorFeedback(R.string.msg_create_block_failed));

  }

  public void fetchDestroyBlock(long userId) {
    twitterApi.destroyBlock(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(u -> {
              cache.open();
              cache.updateBlocking(userId, false);
              cache.removeIgnoringUser(u);
              cache.close();
              userFeedback.onNext(new UserFeedbackEvent(R.string.msg_create_block_success));
            },
            onErrorFeedback(R.string.msg_create_block_failed));
  }

  public void reportSpam(long userId) {
    twitterApi.reportSpam(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(user -> {
              cache.open();
              cache.addIgnoringUser(user);
              cache.close();
              userFeedback.onNext(new UserFeedbackEvent(R.string.msg_report_spam_success));
            },
            onErrorFeedback(R.string.msg_report_spam_failed));
  }

  public void fetchCreateMute(long userId) {
    twitterApi.createMute(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(u -> {
              cache.open();
              cache.updateMuting(userId, true);
              cache.addIgnoringUser(u);
              cache.close();
              userFeedback.onNext(new UserFeedbackEvent(R.string.msg_create_mute_success));
            },
            onErrorFeedback(R.string.msg_create_mute_failed));
  }

  public void fetchDestroyMute(long userId) {
    twitterApi.destroyMute(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(u -> {
              cache.open();
              cache.updateMuting(userId, false);
              cache.removeIgnoringUser(u);
              cache.close();
              userFeedback.onNext(new UserFeedbackEvent(R.string.msg_destroy_mute_success));
            },
            onErrorFeedback(R.string.msg_destroy_mute_failed));
  }

  public void fetchBlockRetweet(Relationship old) {
    twitterApi.updateFriendship(old.getTargetUserId(), old.isSourceNotificationsEnabled(), false)
        .subscribe(r -> {
              cache.open();
              cache.upsertRelationship(r);
              cache.close();
              userFeedback.onNext(new UserFeedbackEvent(R.string.msg_block_retweet_success));
            },
            onErrorFeedback(R.string.msg_block_retweet_failed));
  }

  public void fetchUnblockRetweet(Relationship old) {
    twitterApi.updateFriendship(old.getTargetUserId(), old.isSourceNotificationsEnabled(), true)
        .subscribe(r -> {
              cache.open();
              cache.upsertRelationship(r);
              cache.close();
              userFeedback.onNext(new UserFeedbackEvent(R.string.msg_unblock_retweet_success));
            },
            onErrorFeedback(R.string.msg_unblock_retweet_failed));
  }

  public void shrink() {
    cache.open();
    cache.shrink();
    cache.close();
  }

  @NonNull
  private Consumer<Throwable> onErrorFeedback(@StringRes final int msg) {
    return throwable -> userFeedback.onNext(new UserFeedbackEvent(msg));
  }
}
