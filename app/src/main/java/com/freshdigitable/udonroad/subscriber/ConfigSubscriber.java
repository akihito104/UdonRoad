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

import android.content.SharedPreferences;
import android.util.Log;

import com.android.annotations.NonNull;
import com.freshdigitable.udonroad.FeedbackSubscriber;
import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.io.Serializable;
import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

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
 * ConfigSubscriber provides to fetch twitter api and to store its data.
 *
 * Created by akihit on 2016/09/23.
 */
public class ConfigSubscriber {
  public static final String TWITTER_API_CONFIG_DATE = "twitterAPIConfigDate";
  private final String TAG = ConfigSubscriber.class.getSimpleName();
  private final TwitterApi twitterApi;
  private final ConfigStore configStore;
  private final SharedPreferences prefs;

  @Inject
  public ConfigSubscriber(@NonNull TwitterApi twitterApi,
                          @NonNull ConfigStore configStore,
                          @NonNull SharedPreferences prefs) {
    this.twitterApi = twitterApi;
    this.configStore = configStore;
    this.prefs = prefs;
  }

  public void open() {
    configStore.open();
  }

  public void close() {
    configStore.close();
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
            configStore.addAuthenticatedUser(user);
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
            return configStore.getAuthenticatedUser(userId);
          }
        });
  }

  private Observable<TwitterAPIConfiguration> fetchTwitterAPIConfig() {
    if (!isTwitterAPIConfigFetchable()) {
      Log.d(TAG, "fetchTwitterAPIConfig: not fetch");
      return Observable.empty();
    }
    Log.d(TAG, "fetchTwitterAPIConfig: fetching");
    return twitterApi.getTwitterAPIConfiguration()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(new Action1<TwitterAPIConfiguration>() {
          @Override
          public void call(TwitterAPIConfiguration configuration) {
            configStore.setTwitterAPIConfig(configuration);
          }
        }).doOnError(onErrorAction)
        .doOnCompleted(new Action0() {
          @Override
          public void call() {
            setFetchTwitterAPIConfigTime(System.currentTimeMillis());
          }
        });
  }

  private void setFetchTwitterAPIConfigTime(long timestamp) {
    prefs.edit()
        .putLong(TWITTER_API_CONFIG_DATE, timestamp)
        .apply();
  }

  private long getFetchTwitterAPIConfigTime() {
    return prefs.getLong(TWITTER_API_CONFIG_DATE, -1);
  }

  private boolean isTwitterAPIConfigFetchable() {
    final TwitterAPIConfiguration twitterAPIConfig = configStore.getTwitterAPIConfig();
    if (twitterAPIConfig == null) {
      return true;
    }
    final long lastTime = getFetchTwitterAPIConfigTime();
    if (lastTime == -1) {
      return true;
    }
    final long now = System.currentTimeMillis();
    return now - lastTime > TimeUnit.DAYS.toMillis(1);
  }

  public ConfigStore getConfigStore() {
    return configStore;
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
            configStore.replaceIgnoringUsers(ids);
          }
        })
        .doOnError(onErrorAction);
  }

  private FeedbackSubscriber feedback;

  public void setFeedbackSubscriber(FeedbackSubscriber feedback) {
    this.feedback = feedback;
  }

  public void createBlock(final long userId) {
    twitterApi.createBlock(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            addIgnoringUserAction(),
            feedback.onErrorDefault(R.string.msg_create_block_failed),
            feedback.onCompleteDefault(R.string.msg_create_block_success));
  }

  public void destroyBlock(final long userId) {
    twitterApi.destroyBlock(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            removeIgnoringUserAction(),
            feedback.onErrorDefault(R.string.msg_create_block_failed),
            feedback.onCompleteDefault(R.string.msg_create_block_success));
  }

  public void reportSpam(long userId) {
    twitterApi.reportSpam(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            addIgnoringUserAction(),
            feedback.onErrorDefault(R.string.msg_report_spam_failed),
            feedback.onCompleteDefault(R.string.msg_report_spam_success));
  }

  public void createMute(long userId) {
    twitterApi.createMute(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            addIgnoringUserAction(),
            feedback.onErrorDefault(R.string.msg_create_mute_failed),
            feedback.onCompleteDefault(R.string.msg_create_mute_success));
  }

  @NonNull
  private Action1<User> addIgnoringUserAction() {
    return new Action1<User>() {
      @Override
      public void call(User user) {
        configStore.addIgnoringUser(user);
      }
    };
  }

  @NonNull
  private Action1<User> removeIgnoringUserAction() {
    return new Action1<User>() {
      @Override
      public void call(User user) {
        configStore.removeIgnoringUser(user);
      }
    };
  }
}
