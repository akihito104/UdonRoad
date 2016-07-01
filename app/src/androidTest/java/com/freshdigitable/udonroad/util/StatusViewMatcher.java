/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.util;

import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.test.espresso.core.deps.guava.base.Predicate;
import android.support.test.espresso.core.deps.guava.collect.Iterables;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.v7.widget.RecyclerView;
import android.view.View;

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
}