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
import android.view.View;

import com.freshdigitable.udonroad.TweetInputFragment.TweetType;

import static com.freshdigitable.udonroad.TweetInputFragment.TYPE_DEFAULT;
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

  ToolbarTweetInputToggle(@NonNull TweetInputFragment fragment, @NonNull Toolbar toolbar) {
    this.fragment = fragment;
    this.toolbar = toolbar;
    this.navigationIcon = new DrawerArrowDrawable(toolbar.getContext());
  }

  void expandTweetInputView(@TweetType int type, long statusId) {
    if (type != TYPE_DEFAULT) {
      fragment.stretchTweetInputView(type, statusId);
    }
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

    toolbar.setNavigationIcon(this.navigationIcon);
    toolbar.setNavigationOnClickListener(getOnCollapseClickListener());
    navigationIcon.setProgress(1.f);
    toolbar.setNavigationContentDescription(R.string.navDesc_cancelTweet);
  }

  @NonNull
  View.OnClickListener getOnCollapseClickListener() {
    return v -> {
      collapseTweetInputView();
      if (closeListener != null) {
        closeListener.onTweetInputClosed(v);
      }
    };
  }

  void collapseTweetInputView() {
    fragment.collapseStatusInputView();
    toolbar.setTitle(prevTitle);
    toolbar.setNavigationOnClickListener(null);
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

  interface OnCloseListener {
    void onTweetInputClosed(View view);
  }

  private OnCloseListener closeListener;

  void setOnCloseListener(OnCloseListener listener) {
    closeListener = listener;
  }
}
