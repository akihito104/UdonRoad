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

import android.support.test.espresso.ViewInteraction;

import com.freshdigitable.udonroad.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by akihit on 2017/09/24.
 */

public class MatcherUtil {

  public static ViewInteraction onCancelWriteMenu() {
    return onView(withContentDescription(R.string.navDesc_cancelTweet));
  }

  public static ViewInteraction onOpenDrawerMenu() {
    return onView(withContentDescription(R.string.navDesc_openDrawer));
  }

  public static ViewInteraction onActionWrite() {
    return onView(withId(R.id.action_writeTweet));
  }

  public static ViewInteraction onIFFAB() {
    return onView(withId(R.id.ffab));
  }

  private MatcherUtil() {}
}
