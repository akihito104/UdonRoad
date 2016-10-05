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

import android.content.SharedPreferences;
import android.util.Log;

import com.android.annotations.NonNull;
import com.freshdigitable.udonroad.datastore.ConfigStore;

import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
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
  private static final String TWITTER_API_CONFIG_DATE = "twitterAPIConfigDate";
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

  public void verifyCredencials() {
    twitterApi.verifyCredentials()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<User>() {
          @Override
          public void call(User user) {
            configStore.addAuthenticatedUser(user);
          }
        }, onErrorAction);
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

  public void fetchTwitterAPIConfig() {
    if (!isTwitterAPIConfigFetchable()) {
      Log.d(TAG, "fetchTwitterAPIConfig: not fetch");
      return;
    }
    Log.d(TAG, "fetchTwitterAPIConfig: fetching");
    setFetchTwitterAPIConfigTime(System.currentTimeMillis());
    twitterApi.getTwitterAPIConfiguration()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<TwitterAPIConfiguration>() {
          @Override
          public void call(TwitterAPIConfiguration configuration) {
            configStore.setTwitterAPIConfig(configuration);
          }
        }, onErrorAction);
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

  public void fetchAllIgnoringUsers() {
    Observable.concat(twitterApi.getAllMutesIDs(), twitterApi.getAllBlocksIDs())
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
        .subscribe(new Action1<Collection<Long>>() {
          @Override
          public void call(Collection<Long> ids) {
            configStore.replaceIgnoringUsers(ids);
          }
        }, onErrorAction);
  }
}
