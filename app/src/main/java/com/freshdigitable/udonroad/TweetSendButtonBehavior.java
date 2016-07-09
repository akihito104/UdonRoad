/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

/**
 * Created by akihit on 2016/07/07.
 */
public class TweetSendButtonBehavior extends CoordinatorLayout.Behavior<FloatingActionButton> {
  public TweetSendButtonBehavior() {
    super();
  }

  public TweetSendButtonBehavior(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionButton child,
                                 View dependency) {
    return dependency instanceof AppBarLayout
        && dependency.findViewById(R.id.main_tweet_input_view) != null;
  }

  @Override
  public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
    if (!(dependency instanceof AppBarLayout)) {
      return false;
    }
    updateFab(child, (AppBarLayout) dependency);
    return false;
  }

  public boolean updateFab(FloatingActionButton child, AppBarLayout dependency) {
    final CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) child.getLayoutParams();
    if (lp.getAnchorId() != dependency.getId()) {
      return false;
    }
    final TweetInputView v = (TweetInputView) dependency.findViewById(R.id.main_tweet_input_view);
    if (v.isVisible()) {
      child.show();
    } else {
      child.hide();
    }
    return true;
  }

  @Override
  public boolean onLayoutChild(CoordinatorLayout parent, FloatingActionButton child, int layoutDirection) {
    final List<View> dependencies = parent.getDependencies(child);
    for (View d : dependencies) {
      if (d instanceof AppBarLayout
          && updateFab(child, (AppBarLayout) d)) {
        break;
      }
    }
    parent.onLayoutChild(child, layoutDirection);
    return true;
  }
}
