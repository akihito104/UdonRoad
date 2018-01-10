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

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatDelegate;
import android.view.View;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.StoreManager;
import com.freshdigitable.udonroad.datastore.UpdateSubjectFactory;
import com.freshdigitable.udonroad.fetcher.StatusListFetcherModule;
import com.freshdigitable.udonroad.module.AppComponent;
import com.freshdigitable.udonroad.module.DaggerAppComponent;
import com.freshdigitable.udonroad.module.DataStoreModule;
import com.freshdigitable.udonroad.module.TwitterApiModule;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;
import com.freshdigitable.udonroad.module.twitter.TwitterStreamApi;
import com.freshdigitable.udonroad.repository.RepositoryModule;
import com.freshdigitable.udonroad.subscriber.AppSettingRequestWorker;
import com.freshdigitable.udonroad.subscriber.UserFeedbackSubscriber;
import com.squareup.leakcanary.LeakCanary;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;
import twitter4j.auth.AccessToken;

import static android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * MainApplication is custom Application class.
 *
 * Created by akihit on 2016/06/16.
 */
public class MainApplication extends Application {
  private AppComponent appComponent;
  @Inject
  TwitterApi twitterApi;
  @Inject
  TwitterStreamApi twitterStreamApi;

  @Inject
  AppSettingRequestWorker appSettingWorker;
  @Inject
  AppSettingStore appSettings;
  @Inject
  UserStreamUtil userStreamUtil;
  @Inject
  UserFeedbackSubscriber userFeedback;
  @Inject
  UpdateSubjectFactory updateSubjectFactory;
  @Inject
  StoreManager storeManager;

  @Override
  public void onCreate() {
    super.onCreate();
    if (LeakCanary.isInAnalyzerProcess(this)) {
      // This process is dedicated to LeakCanary for heap analysis.
      // You should not init your app in this process.
      return;
    }
    LeakCanary.install(this);

    appComponent = createAppComponent();
    appComponent.inject(this);
    storeManager.init(getApplicationContext());
    init(this);
    registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacksImpl());
    AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());
    }
  }

  @VisibleForTesting
  protected AppComponent createAppComponent() {
    return DaggerAppComponent.builder()
        .twitterApiModule(new TwitterApiModule(getApplicationContext()))
        .dataStoreModule(new DataStoreModule(getApplicationContext()))
        .repositoryModule(new RepositoryModule(getApplicationContext()))
        .statusListFetcherModule(new StatusListFetcherModule())
        .build();
  }

  public AppComponent getAppComponent() {
    return appComponent;
  }

  private static void init(MainApplication application) {
    application.appSettings.open();
    final SharedPreferences sp = getDefaultSharedPreferences(application);
    final String loginUser = sp.getString(application.getString(R.string.settings_key_loginUser), "-1");
    if (!loginUser.equals("-1")) {
      application.appSettings.setCurrentUserId(Long.parseLong(loginUser));
    }
    application.appSettings.close();
  }

  private boolean loggedIn = false;

  void login() {
    login(this);
  }

  private static void login(MainApplication app) {
    app.appSettings.open();
    final long currentUserId = app.appSettings.getCurrentUserId();
    app.appSettings.close();
    if (currentUserId >= 0) {
      login(app, currentUserId);
    }
  }

  void login(long userId) {
    login(this, userId);
  }

  private static void login(MainApplication app, long userId) {
    Timber.tag("MainApplication").d("login: %s", userId);
    app.appSettings.setCurrentUserId(userId);
    final AccessToken accessToken = app.appSettings.getCurrentUserAccessToken();
    if (accessToken == null) {
      app.loggedIn = false;
      return;
    }
    app.twitterApi.setOAuthAccessToken(accessToken);
    app.twitterStreamApi.setOAuthAccessToken(accessToken);
    app.loggedIn = true;
    app.appSettingWorker.setup();
    app.appSettingWorker.verifyCredentials();
  }

  void logout() {
    logout(this);
  }

  private static void logout(MainApplication app) {
    Timber.tag("MainApplication").d("logout: ");
    app.userStreamUtil.disconnect();
    app.twitterApi.setOAuthAccessToken(null);
    app.twitterStreamApi.setOAuthAccessToken(null);
    app.loggedIn = false;
  }

  void connectStream() {
    userStreamUtil.connect(StoreType.HOME.storeName);
  }

  private static class ActivityLifecycleCallbacksImpl implements ActivityLifecycleCallbacks {
    private static final String TAG = ActivityLifecycleCallbacksImpl.class.getSimpleName();
    private final List<String> activities = new ArrayList<>();

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
      Timber.tag(TAG).d("onActivityCreated: count>%s", activities.size());
      if (!getApplication(activity).loggedIn) {
        getApplication(activity).login();
      }
      if (!getApplication(activity).loggedIn) {
        launchOAuthActivity(activity);
      }
      if (activities.isEmpty() && savedInstanceState == null) {
        getApplication(activity).storeManager.cleanUp();
      }
      activities.add(activity.getClass().getSimpleName());
    }

    private static void launchOAuthActivity(Activity activity) {
      if (activity instanceof OAuthActivity) {
        return;
      }
      OAuthActivity.start(activity);
      activity.finish();
    }

    @Override
    public void onActivityStarted(Activity activity) {
      Timber.tag(TAG).d("onActivityStarted: ");
      if (activity instanceof MainActivity) {
        getApplication(activity).connectStream();
      }
      if (activity instanceof SnackbarCapable) {
        final View rootView = ((SnackbarCapable) activity).getRootView();
        getApplication(activity).userFeedback.registerRootView(rootView);
      }
    }

    @Override
    public void onActivityResumed(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {
      if (activity instanceof SnackbarCapable) {
        final View rootView = ((SnackbarCapable) activity).getRootView();
        getApplication(activity).userFeedback.unregisterRootView(rootView);
      }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }

    @Override
    public void onActivityDestroyed(Activity activity) {
      activities.remove(activity.getClass().getSimpleName());
      Timber.tag(TAG).d("onActivityDestroyed: count>%s", activities.size());
      if (activities.size() == 0) {
        MainApplication.logout(getApplication(activity));
        getApplication(activity).userFeedback.unsubscribe();
        getApplication(activity).updateSubjectFactory.clear();
      }
    }

    private static MainApplication getApplication(Activity activity) {
      return (MainApplication) activity.getApplication();
    }
  }
}
