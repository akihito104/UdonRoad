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
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;

import com.freshdigitable.udonroad.IconAttachedTextView;
import com.freshdigitable.udonroad.R;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import twitter4j.Status;

import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.util.TreeIterables.breadthFirstViewTraversal;

/**
 * Created by akihit on 2016/07/01.
 */
public class StatusViewMatcher {
  @NonNull
  public static Matcher<View> ofStatusView(@NonNull final Status target) {
    return ofStatusViewInternal(withText(target.getText()));
  }

  @NonNull
  public static Matcher<View> ofStatusView(@NonNull final Matcher<View> viewMatcher) {
    return ofStatusViewInternal(viewMatcher);
  }

  @NonNull
  public static Matcher<View> ofRTStatusView(@NonNull final Status target) {
    return ofStatusViewInternal(withText(target.getText()), true);
  }

  @NonNull
  public static Matcher<View> ofRTStatusView(@NonNull final Matcher<View> viewMatcher) {
    return ofStatusViewInternal(viewMatcher, true);
  }

  private static Matcher<View> ofStatusViewInternal(final Matcher<View> viewMatcher) {
    return ofStatusViewInternal(viewMatcher, false);
  }

  private final static SparseIntArray STATUS_VIEW_CHILDREN = new SparseIntArray() {{
    put(R.id.tl_create_at, 0);
    put(R.id.tl_icon, 0);
    put(R.id.tl_image_group, 0);
    put(R.id.tl_names, 0);
    put(R.id.tl_reaction_container, 0);
    put(R.id.tl_rt_user, 0);
    put(R.id.tl_tweet, 0);
    put(R.id.tl_via, 0);
  }};
  private final static SparseIntArray QUOTED_STATUS_VIEW_CHILD = new SparseIntArray() {{
    put(R.id.q_create_at, 0);
    put(R.id.q_icon, 0);
    put(R.id.q_image_group, 0);
    put(R.id.q_names, 0);
    put(R.id.q_reaction_container, 0);
    put(R.id.q_tweet, 0);
    put(R.id.q_via, 0);
  }};
  static boolean isStatusView(View view) {
    return isStatusViewInternal(view, STATUS_VIEW_CHILDREN);
  }

  static boolean isQuotedStatusView(View view) {
    return isStatusViewInternal(view, QUOTED_STATUS_VIEW_CHILD);
  }

  static boolean isStatusViewInternal(View view, SparseIntArray child) {
    if (!(view instanceof ViewGroup)) {
      return false;
    }
    final ViewGroup item = (ViewGroup) view;
    final int childCount = item.getChildCount();
    for (int i = 0; i < childCount; i++) {
      final View v = item.getChildAt(i);
      if (child.get(v.getId(), -1) == 0) {
        return true;
      }
    }
    return false;
  }

  private static Matcher<View> ofStatusViewInternal(final Matcher<View> viewMatcher, boolean isRetweet) {
    return new BoundedMatcher<View, ViewGroup>(ViewGroup.class) {
      @Override
      protected boolean matchesSafely(ViewGroup item) {
        if (!isStatusView(item)) {
          return false;
        }
        final View rtUser = item.findViewById(R.id.tl_rt_user);
        final boolean rtViewInvisible = rtUser == null || rtUser.getVisibility() != View.VISIBLE;
        if (rtViewInvisible == isRetweet) {
          return false;
        }
        final Iterable<View> it = Iterables.filter(breadthFirstViewTraversal(item),
            view -> !isQuotedStatusView(view) && viewMatcher.matches(view));
        return it.iterator().hasNext();
      }

      @Override
      public void describeTo(Description description) {
        viewMatcher.describeTo(description);
      }
    };
  }

  @NonNull
  public static Matcher<View> ofQuotedStatusView(@NonNull final Status target) {
    return ofQuotedStatusView(withText(target.getText()));
  }

  @NonNull
  public static Matcher<View> ofQuotedStatusView(@NonNull final Matcher<View> viewMatcher) {
    return new BoundedMatcher<View, ViewGroup>(ViewGroup.class) {
      @Override
      protected boolean matchesSafely(ViewGroup item) {
        if (!isQuotedStatusView(item)) {
          return false;
        }
        final Iterable<View> it = Iterables.filter(breadthFirstViewTraversal(item),
            viewMatcher::matches);
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
        final ViewParent parent = item.getParent();
        if (!(parent instanceof RecyclerView)) {
          return false;
        }
        final RecyclerView recyclerView = (RecyclerView) parent;
        return recyclerViewMatcher.matches(recyclerView)
            && getPositionFromFirstVisibleItem(recyclerView, item) == position;
      }

      @Override
      public void describeTo(Description description) {}
    };
  }

  private static int getPositionFromFirstVisibleItem(RecyclerView recyclerView, View item) {
    final int childCount = recyclerView.getChildCount();
    int firstVisibleItemIndex = 0;
    for (int i = 0; i < childCount; i++) {
      final View child = recyclerView.getChildAt(i);
      if (child.getY() < 0) {
        firstVisibleItemIndex = i + 1;
      } else if (child == item) {
        return i - firstVisibleItemIndex;
      }
    }
    return -1;
  }

  public static Matcher<View> ofStatusViewAt(@IdRes final int recyclerViewId, final int position) {
    final Matcher<View> viewMatcher = ofItemViewAt(recyclerViewId, position, ViewGroup.class);
    return new BoundedMatcher<View, ViewGroup>(ViewGroup.class) {
      @Override
      protected boolean matchesSafely(ViewGroup item) {
        return isStatusView(item) && viewMatcher.matches(item);
      }

      @Override
      public void describeTo(Description description) {
        viewMatcher.describeTo(description);
      }
    };
  }

  public static Matcher<View> asUserIcon(@IdRes final int resId, final Matcher<View> root) {
    final Matcher<View> iconMatcher = withId(resId);
    return new BoundedMatcher<View, ImageView>(ImageView.class) {
      @Override
      protected boolean matchesSafely(ImageView item) {
        return iconMatcher.matches(item)
            && root.matches(item.getParent());
      }

      @Override
      public void describeTo(Description description) {
        iconMatcher.describeTo(description);
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