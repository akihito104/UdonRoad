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

import android.support.test.espresso.IdlingResource;
import android.util.Log;

import java.util.concurrent.Callable;

/**
 * Created by akihit on 2017/08/04.
 */

public class IdlingResourceUtil {
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
          Log.e("OAuthActivityInstTest", "isIdleNow: ", e);
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

  private IdlingResourceUtil() {}
}