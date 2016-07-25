/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.app.Activity;
import android.app.Application;
import android.support.v4.app.Fragment;

/**
 * Created by akihit on 2016/07/25.
 */
public class InjectionUtil {
  public static AppComponent getComponent(Fragment fragment) {
    return getComponent(fragment.getActivity());
  }

  public static AppComponent getComponent(Activity activity) {
    return getComponent(activity.getApplication());
  }

  public static AppComponent getComponent(Application app) {
    final MainApplication application = (MainApplication) app;
    return application.getAppComponent();
  }
}
