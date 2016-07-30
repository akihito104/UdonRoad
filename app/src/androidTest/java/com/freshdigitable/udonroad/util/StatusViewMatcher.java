/*
 * Copyright (c) 2016. Akihito Matsuda (akihito104)
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
import android.support.annotation.NonNull;
import android.support.test.espresso.core.deps.guava.base.Predicate;
import android.support.test.espresso.core.deps.guava.collect.Iterables;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.freshdigitable.udonroad.StatusView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import javax.annotation.Nullable;

import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.util.TreeIterables.breadthFirstViewTraversal;

/**
 * Created by akihit on 2016/07/01.
 */
public class StatusViewMatcher {
  @NonNull
  public static Matcher<View> ofStatusView(final Matcher<View> viewMatcher) {
    return new BoundedMatcher<View, StatusView>(StatusView.class) {
      @Override
      protected boolean matchesSafely(StatusView item) {
        final Iterable<View> it = Iterables.filter(breadthFirstViewTraversal(item),
            new Predicate<View>() {
              @Override
              public boolean apply(@Nullable View view) {
                return view != null
                    && viewMatcher.matches(view);
              }
            });
        return it.iterator().hasNext();
      }

      @Override
      public void describeTo(Description description) {
        viewMatcher.describeTo(description);
      }
    };
  }

  public static Matcher<View> ofStatusViewAt(@IdRes final int recyclerViewId, final int position) {
    final Matcher<View> recyclerViewMatcher = withId(recyclerViewId);
    return new BoundedMatcher<View, StatusView>(StatusView.class) {
      @Override
      public void describeTo(Description description) {
      }

      @Override
      protected boolean matchesSafely(StatusView item) {
        final RecyclerView recyclerView = (RecyclerView) item.getParent();
        if (!recyclerViewMatcher.matches(recyclerView)) {
          return false;
        }
        final View target = recyclerView.getChildAt(position);
        return target == item;
      }
    };
  }

  public static Matcher<View> asUserIcon(@IdRes final int resId, final Matcher<View> root) {
    final Matcher<View> iconMatcher = withId(resId);
    return new BoundedMatcher<View, ImageView>(ImageView.class) {
      @Override
      public void describeTo(Description description) {
      }

      @Override
      protected boolean matchesSafely(ImageView item) {
        return iconMatcher.matches(item)
            && root.matches(item.getParent());
      }
    };
  }
}