/*
 * Copyright (c) 2017. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad.util;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.IdlingResource;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;

import com.freshdigitable.udonroad.R;

import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Created by akihit on 2017/08/04.
 */

public class IdlingResourceUtil {

  public static IdlingResource getActivityStageIdlingResource(
      @NonNull String name, @NonNull Class<? extends Activity> clz, @NonNull Stage stage) {
    return getSimpleIdlingResource(name, () -> findActivityByStage(clz, stage) != null);
  }

  public static <T extends Activity> T findActivityByStage(@NonNull Class<T> clz, @NonNull Stage stage) {
    final Collection<Activity> resumeActivities
        = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage);
    for (Activity activity : resumeActivities) {
      if (activity.getClass().isAssignableFrom(clz)) {
        return clz.cast(activity);
      }
    }
    return null;
  }

  @NonNull
  public static IdlingResource getOpenDrawerIdlingResource(Activity activity) {
    return IdlingResourceUtil.getSimpleIdlingResource("open drawer", () -> {
      final DrawerLayout drawerLayout = activity.findViewById(R.id.nav_drawer_layout);
      final View drawer = activity.findViewById(R.id.nav_drawer);
      return drawerLayout.isDrawerOpen(drawer);
    });
  }

  @NonNull
  public static IdlingResource getCloseDrawerIdlingResource(Activity activity) {
    return IdlingResourceUtil.getSimpleIdlingResource("close drawer", () -> {
      final DrawerLayout drawerLayout = activity.findViewById(R.id.nav_drawer_layout);
      final View drawer = activity.findViewById(R.id.nav_drawer);
      return !drawerLayout.isDrawerOpen(drawer);
    });
  }

  public static IdlingResource getSimpleIdlingResource(String name, Callable<Boolean> isIdle) {
    return new IdlingResource() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public boolean isIdleNow() {
        try {
          final Boolean isIdleNow = isIdle.call();
          if (isIdleNow && callback != null) {
            callback.onTransitionToIdle();
          }
          return isIdleNow;
        } catch (Exception e) {
          Log.e("IdlingResourceUtil", "isIdleNow: ", e);
          return false;
        }
      }

      private ResourceCallback callback;

      @Override
      public void registerIdleTransitionCallback(ResourceCallback callback) {
        this.callback = callback;
      }
    };
  }

  public static void runWithIdlingResource(@NonNull IdlingResource ir, @NonNull Runnable runnable) {
    try {
      IdlingRegistry.getInstance().register(ir);
      runnable.run();
    } finally {
      IdlingRegistry.getInstance().unregister(ir);
    }
  }

  public static class ActivityWaiter {
    public static ActivityWaiter create(Class<? extends Activity> clz) {
      final ActivityWaiter waiter = new ActivityWaiter(clz);
      waiter.run();
      return waiter;
    }

    public static ActivityWaiter create(Activity target) {
      final ActivityWaiter waiter = new ActivityWaiter(target);
      waiter.run();
      return waiter;
    }

    private Activity target;
    private final Class<? extends Activity> clz;

    private ActivityWaiter(Class<? extends Activity> clz) {
      this.clz = clz;
    }

    private ActivityWaiter(Activity target) {
      this.clz = null;
      this.target = target;
    }

    private void run() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
          if (clz != null) {
            target = IdlingResourceUtil.findActivityByStage(clz, Stage.RESUMED);
          }
          if (target != null) {
            target.finish();
          }
        });
    }

    public void waitForDestroyed() {
      while (target != null && !target.isDestroyed()) {}
    }
  }

  private IdlingResourceUtil() {}
}
