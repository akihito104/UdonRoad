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
@SuppressWarnings("unused")
public class TweetSendButtonBehavior extends CoordinatorLayout.Behavior<FloatingActionButton> {
  @SuppressWarnings("unused")
  public TweetSendButtonBehavior() {
    super();
  }

  @SuppressWarnings("unused")
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
    final TweetInputView v = dependency.findViewById(R.id.main_tweet_input_view);
    if (v != null && v.isVisible()) {
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
