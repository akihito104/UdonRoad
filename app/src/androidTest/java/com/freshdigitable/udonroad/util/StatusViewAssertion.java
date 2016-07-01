/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.util;

import android.support.annotation.IdRes;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAssertion;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.freshdigitable.udonroad.StatusView;

/**
 * Created by akihit on 2016/07/01.
 */
public class StatusViewAssertion {
  public static ViewAssertion recyclerViewDescendantsMatches(
      @IdRes final int recyclerViewId, final int position) {
    return new ViewAssertion() {
      @Override
      public void check(View view, NoMatchingViewException noViewFoundException) {
        if (!(view instanceof StatusView)) {
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
      }
    };
  }
}
