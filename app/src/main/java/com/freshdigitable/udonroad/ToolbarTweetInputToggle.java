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

package com.freshdigitable.udonroad;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.Toolbar;

import com.freshdigitable.udonroad.TweetInputFragment.TweetType;

import static com.freshdigitable.udonroad.TweetInputFragment.TYPE_QUOTE;
import static com.freshdigitable.udonroad.TweetInputFragment.TYPE_REPLY;

/**
 * Created by akihit on 2017/09/24.
 */

class ToolbarTweetInputToggle {
  private final TweetInputFragment fragment;
  private final Toolbar toolbar;
  private final DrawerArrowDrawable navigationIcon;
  private CharSequence prevTitle;
  private CharSequence navContentDescriptionDefault;
  private Drawable navIconDefault;

  ToolbarTweetInputToggle(@NonNull Toolbar toolbar) {
    this.toolbar = toolbar;
    this.fragment = TweetInputFragment.create();
    final Drawable defaultNavIcon = toolbar.getNavigationIcon();
    this.navigationIcon = defaultNavIcon != null && defaultNavIcon instanceof DrawerArrowDrawable ?
        (DrawerArrowDrawable) defaultNavIcon
        : new DrawerArrowDrawable(toolbar.getContext());
  }

  void expandTweetInputView(@TweetType int type, long statusId) {
    fragment.stretchTweetInputView(type, statusId);
    prevTitle = toolbar.getTitle();
    if (type == TYPE_REPLY) {
      toolbar.setTitle(R.string.title_reply);
    } else if (type == TYPE_QUOTE) {
      toolbar.setTitle(R.string.title_comment);
    } else {
      toolbar.setTitle(R.string.title_tweet);
    }
    navIconDefault = toolbar.getNavigationIcon();
    navContentDescriptionDefault = toolbar.getNavigationContentDescription();

    toolbar.setNavigationIcon(navigationIcon);
    navigationIcon.setProgress(1.f);
    toolbar.setNavigationContentDescription(R.string.navDesc_cancelTweet);
  }

  void collapseTweetInputView() {
    if (fragment.isStatusInputViewVisible()) {
      fragment.collapseStatusInputView();
    }
  }

  void onTweetInputViewClosed() {
    toolbar.setTitle(prevTitle);
    toolbar.setNavigationContentDescription(navContentDescriptionDefault);
    toolbar.setNavigationIcon(navIconDefault);
    navigationIcon.setProgress(0.f);
  }

  boolean isOpened() {
    return fragment.isStatusInputViewVisible();
  }

  void changeCurrentUser() {
    fragment.changeCurrentUser();
  }

  TweetInputFragment getFragment() {
    return fragment;
  }
}
