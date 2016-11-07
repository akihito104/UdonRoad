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

import java.io.Serializable;
import java.util.Collection;
import java.util.TreeSet;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func1;
import twitter4j.IDs;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.User;

/**
 * ConfigRequestWorker provides to fetch twitter api and to store its data.
 *
 * Created by akihit on 2016/09/23.
 */
public class ConfigRequestWorker extends RequestWorkerBase<ConfigStore> {
  public static final String TWITTER_API_CONFIG_DATE = "twitterAPIConfigDate";
  private final String TAG = ConfigRequestWorker.class.getSimpleName();
  private final AppSettingStore appSettings;

  @Inject
  public ConfigRequestWorker(@NonNull TwitterApi twitterApi,
                             @NonNull ConfigStore configStore,
                             @NonNull AppSettingStore appSettings,
                             @NonNull UserFeedbackSubscriber userFeedback) {
    super(twitterApi, configStore, userFeedback);
    this.appSettings = appSettings;
  }

  public void open() {
    super.open();
    appSettings.open();
  }

  public void close() {
    appSettings.close();
  }

  public void setup(Action0 completeAction) {
    Observable.concat(
        verifyCredentials(),
        fetchTwitterAPIConfig(),
        fetchAllIgnoringUsers())
        .subscribe(new Action1<Serializable>() {
          @Override
          public void call(Serializable serializable) {
            //nop
          }
        }, onErrorAction, completeAction);
  }

  private Observable<User> verifyCredentials() {
    return twitterApi.verifyCredentials()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(new Action1<User>() {
          @Override
          public void call(User user) {
            appSettings.addAuthenticatedUser(user);
          }
        })
        .doOnError(onErrorAction);
  }

  public Observable<User> getAuthenticatedUser() {
    return twitterApi.getId()
        .observeOn(AndroidSchedulers.mainThread())
        .map(new Func1<Long, User>() {
          @Override
          public User call(Long userId) {
            return appSettings.getAuthenticatedUser(userId);
          }
        });
  }

  private Observable<TwitterAPIConfiguration> fetchTwitterAPIConfig() {
    if (!appSettings.isTwitterAPIConfigFetchable()) {
      Log.d(TAG, "fetchTwitterAPIConfig: not fetch");
      return Observable.empty();
    }
    Log.d(TAG, "fetchTwitterAPIConfig: fetching");
    return twitterApi.getTwitterAPIConfiguration()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(new Action1<TwitterAPIConfiguration>() {
          @Override
          public void call(TwitterAPIConfiguration configuration) {
            appSettings.setTwitterAPIConfig(configuration);
          }
        }).doOnError(onErrorAction);
  }

  private final Action1<Throwable> onErrorAction = new Action1<Throwable>() {
    @Override
    public void call(Throwable throwable) {
      Log.e(TAG, "call: ", throwable);
    }
  };

  private Observable<TreeSet<Long>> fetchAllIgnoringUsers() {
    return Observable.concat(twitterApi.getAllMutesIDs(), twitterApi.getAllBlocksIDs())
        .map(new Func1<IDs, long[]>() {
          @Override
          public long[] call(IDs iDs) {
            return iDs.getIDs();
          }
        })
        .collect(new Func0<TreeSet<Long>>() {
                   @Override
                   public TreeSet<Long> call() {
                     return new TreeSet<>();
                   }
                 },
            new Action2<TreeSet<Long>, long[]>() {
              @Override
              public void call(TreeSet<Long> collector, long[] ids) {
                for (long l : ids) {
                  collector.add(l);
                }
              }
            })
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(new Action1<Collection<Long>>() {
          @Override
          public void call(Collection<Long> ids) {
            cache.replaceIgnoringUsers(ids);
          }
        })
        .doOnError(onErrorAction);
  }

  public void createBlock(final long userId) {
    twitterApi.createBlock(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            addIgnoringUserAction(),
            onErrorFeedback(R.string.msg_create_block_failed),
            onCompleteFeedback(R.string.msg_create_block_success));
  }

  public void destroyBlock(final long userId) {
    twitterApi.destroyBlock(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            removeIgnoringUserAction(),
            onErrorFeedback(R.string.msg_create_block_failed),
            onCompleteFeedback(R.string.msg_create_block_success));
  }

  public void reportSpam(long userId) {
    twitterApi.reportSpam(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            addIgnoringUserAction(),
            onErrorFeedback(R.string.msg_report_spam_failed),
            onCompleteFeedback(R.string.msg_report_spam_success));
  }

  public void createMute(long userId) {
    twitterApi.createMute(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            addIgnoringUserAction(),
            onErrorFeedback(R.string.msg_create_mute_failed),
            onCompleteFeedback(R.string.msg_create_mute_success));
  }

  @NonNull
  private Action1<User> addIgnoringUserAction() {
    return new Action1<User>() {
      @Override
      public void call(User user) {
        cache.addIgnoringUser(user);
      }
    };
  }

  @NonNull
  private Action1<User> removeIgnoringUserAction() {
    return new Action1<User>() {
      @Override
      public void call(User user) {
        cache.removeIgnoringUser(user);
      }
    };
  }
}
