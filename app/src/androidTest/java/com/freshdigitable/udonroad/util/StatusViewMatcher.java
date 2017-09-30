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

import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.test.espresso.core.internal.deps.guava.collect.Iterables;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.freshdigitable.udonroad.IconAttachedTextView;
import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.listitem.ItemView;
import com.freshdigitable.udonroad.listitem.QuotedStatusView;
import com.freshdigitable.udonroad.listitem.StatusView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.util.TreeIterables.breadthFirstViewTraversal;

/**
 * Created by akihit on 2016/07/01.
 */
public class StatusViewMatcher {
  @NonNull
  public static Matcher<View> ofStatusView(final Matcher<View> viewMatcher) {
    return ofStatusViewInternal(viewMatcher, StatusView.class);
  }

  public static Matcher<View> ofRTStatusView(final Matcher<View> viewMatcher) {
    return ofStatusViewInternal(viewMatcher, StatusView.class, true);
  }

  private static <T extends View & ItemView> Matcher<View> ofStatusViewInternal(
      final Matcher<View> viewMatcher, Class<T> clz) {
    return ofStatusViewInternal(viewMatcher, clz, false);
  }

  private static <T extends View & ItemView> Matcher<View> ofStatusViewInternal(
      final Matcher<View> viewMatcher, Class<T> clz, boolean isRetweet) {
    return new BoundedMatcher<View, T>(clz) {
      @Override
      protected boolean matchesSafely(T item) {
        if (isRetweet) {
          final View rtUser = item.findViewById(R.id.tl_rt_user);
          if (rtUser == null || rtUser.getVisibility() != View.VISIBLE) {
            return false;
          }
        }
        final Iterable<View> it = Iterables.filter(breadthFirstViewTraversal(item),
            view -> view != null && !(view.getParent() instanceof QuotedStatusView) && viewMatcher.matches(view));
        return it.iterator().hasNext();
      }

      @Override
      public void describeTo(Description description) {
        viewMatcher.describeTo(description);
      }
    };
  }

  public static Matcher<View> ofQuotedStatusView(final Matcher<View> viewMatcher) {
    return     new BoundedMatcher<View, QuotedStatusView>(QuotedStatusView.class) {
      @Override
      protected boolean matchesSafely(QuotedStatusView item) {
        final Iterable<View> it = Iterables.filter(breadthFirstViewTraversal(item),
            view -> view != null && viewMatcher.matches(view));
        return it.iterator().hasNext();
      }

      @Override
      public void describeTo(Description description) {
        viewMatcher.describeTo(description);
      }
    };
  }

  public static <T extends View> Matcher<View> ofItemViewAt(
      @IdRes final int recyclerViewId, final int position, Class<T> clz) {
    final Matcher<View> recyclerViewMatcher = withId(recyclerViewId);
    return new BoundedMatcher<View, T>(clz) {
      @Override
      protected boolean matchesSafely(T item) {
        final RecyclerView recyclerView = (RecyclerView) item.getParent();
        if (!recyclerViewMatcher.matches(recyclerView)) {
          return false;
        }
        final View target = recyclerView.getChildAt(position);
        return target == item;
      }

      @Override
      public void describeTo(Description description) {}
    };
  }

  public static Matcher<View> ofStatusViewAt(@IdRes final int recyclerViewId, final int position) {
    return ofItemViewAt(recyclerViewId, position, StatusView.class);
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

  public static Matcher<View> getFavIcon() {
    return getReactionContainerMatcher(
        R.id.tl_reaction_container, R.drawable.ic_like, IconAttachedTextView.class);
  }

  public static Matcher<View> getFavIconOfQuoted() {
    return getReactionContainerMatcher(
        R.id.q_reaction_container, R.drawable.ic_like, IconAttachedTextView.class);
  }

  public static Matcher<View> getRTIcon() {
    return getReactionContainerMatcher(
        R.id.tl_reaction_container, R.drawable.ic_retweet, IconAttachedTextView.class);
  }

  public static Matcher<View> getHasReplyIcon() {
    return getReactionContainerMatcher(
        R.id.tl_reaction_container, R.drawable.ic_forum, ImageView.class);
  }

  public static <T extends View> Matcher<View> getReactionContainerMatcher(
      @IdRes int containerId, @DrawableRes int expectedId, Class<T> clz) {
    return new BoundedMatcher<View, T>(clz) {
      @Override
      protected boolean matchesSafely(T item) {
        return withId(containerId).matches(item.getParent())
            && withId(expectedId).matches(item);
      }

      @Override
      public void describeTo(Description description) {}
    };
  }
}