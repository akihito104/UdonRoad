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

package com.freshdigitable.udonroad.util;

import android.support.annotation.IdRes;
import android.support.test.espresso.ViewAssertion;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by akihit on 2016/07/01.
 */
public class StatusViewAssertion {
  public static ViewAssertion recyclerViewDescendantsMatches(
      @IdRes final int recyclerViewId, final int position) {
    return (view, noViewFoundException) -> {
      if (!StatusViewMatcher.isStatusView(view)) {
        throw noViewFoundException;
      }
      final RecyclerView recyclerView = (RecyclerView) view.getParent();
      if (recyclerView == null
          || recyclerView.getId() != recyclerViewId) {
        throw noViewFoundException;
      }
      final View actual = recyclerView.getChildAt(position);
      if (view != actual) {
        throw noViewFoundException;
      }
    };
  }
}
