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
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.View;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.module.AppComponent;
import com.freshdigitable.udonroad.module.DaggerAppComponent;
import com.freshdigitable.udonroad.module.DataStoreModule;
import com.freshdigitable.udonroad.module.TwitterApiModule;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;
import com.freshdigitable.udonroad.subscriber.UserFeedbackSubscriber;
import com.squareup.leakcanary.LeakCanary;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.realm.Realm;
import twitter4j.auth.AccessToken;

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
  AppSettingStore appSettings;
  @Inject
  UserStreamUtil userStreamUtil;
  @Inject
  UserFeedbackSubscriber userFeedback;

  @Override
  public void onCreate() {
    super.onCreate();
    appComponent = createAppComponent();
    LeakCanary.install(this);
    Realm.init(getApplicationContext());
    appComponent.inject(this);
    final boolean init = init(this);
    registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacksImpl(init));
  }

  @VisibleForTesting
  protected AppComponent createAppComponent() {
    return DaggerAppComponent.builder()
        .twitterApiModule(new TwitterApiModule(getApplicationContext()))
        .dataStoreModule(new DataStoreModule(getApplicationContext()))
        .build();
  }

  public AppComponent getAppComponent() {
    return appComponent;
  }

  private static boolean init(MainApplication application) {
    application.appSettings.open();
    final AccessToken accessToken = application.appSettings.getCurrentUserAccessToken();
    application.appSettings.close();
    if (accessToken == null) {
      return false;
    }
    application.twitterApi.setOAuthAccessToken(accessToken);
    return true;
  }

  private static class ActivityLifecycleCallbacksImpl implements ActivityLifecycleCallbacks {
    private static final String TAG = ActivityLifecycleCallbacksImpl.class.getSimpleName();
    private boolean isTokenSetup = false;
    private final List<String> activities = new ArrayList<>();

    ActivityLifecycleCallbacksImpl(boolean isTokenSetup) {
      this.isTokenSetup = isTokenSetup;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
      activities.add(activity.getClass().getSimpleName());
      Log.d(TAG, "onActivityCreated: count>" + activities.size());
      if (!isTokenSetup) {
        launchOAuthActivity(activity);
        isTokenSetup = setupAccessToken(activity);
      }
    }

    private static void launchOAuthActivity(Activity activity) {
      if (activity instanceof OAuthActivity) {
        return;
      }
      OAuthActivity.start(activity);
      activity.finish();
    }

    private static boolean setupAccessToken(Activity activity) {
      return !(activity instanceof OAuthActivity)
          && MainApplication.init(getApplication(activity));
    }

    @Override
    public void onActivityStarted(Activity activity) {
      if (activity instanceof MainActivity) {
        getApplication(activity).userStreamUtil.connect(StoreType.HOME.storeName);
      }
      if (activity instanceof SnackbarCapable) {
        final View rootView = ((SnackbarCapable) activity).getRootView();
        getApplication(activity).userFeedback.registerRootView(rootView);
      }
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
      if (activity instanceof SnackbarCapable) {
        final View rootView = ((SnackbarCapable) activity).getRootView();
        getApplication(activity).userFeedback.unregisterRootView(rootView);
      }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
      activities.remove(activity.getClass().getSimpleName());
      Log.d(TAG, "onActivityDestroyed: count>" + activities.size());
      if (activity instanceof MainActivity) {
        getApplication(activity).userStreamUtil.disconnect();
      }
      if (activities.size() == 0 && activity instanceof SnackbarCapable) {
        getApplication(activity).userFeedback.unsubscribe();
      }
    }

    private static MainApplication getApplication(Activity activity) {
      return (MainApplication) activity.getApplication();
    }
  }
}
