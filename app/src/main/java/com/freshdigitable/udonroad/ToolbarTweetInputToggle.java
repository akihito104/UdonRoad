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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.freshdigitable.udonroad.TweetInputFragment.TweetType;

import static com.freshdigitable.udonroad.TweetInputFragment.TYPE_NONE;
import static com.freshdigitable.udonroad.TweetInputFragment.TYPE_QUOTE;
import static com.freshdigitable.udonroad.TweetInputFragment.TYPE_REPLY;

/**
 * Created by akihit on 2017/09/24.
 */

class ToolbarTweetInputToggle {
  private final TweetInputFragment fragment;
  private final Toolbar toolbar;
  private final Drawable navigationIcon;
  private CharSequence prevTitle;
  private CharSequence navContentDescriptionDefault;
  private Drawable navIconDefault;

  ToolbarTweetInputToggle(@NonNull Toolbar toolbar) {
    this(toolbar, TweetInputFragment.create());
  }

  ToolbarTweetInputToggle(@NonNull Toolbar toolbar, @Nullable TweetInputFragment tweetInputFragment) {
    this.toolbar = toolbar;
    this.fragment = tweetInputFragment != null ? tweetInputFragment : TweetInputFragment.create();
    this.navigationIcon = AppCompatResources.getDrawable(toolbar.getContext(), R.drawable.ic_clear_white);
  }

  void expandTweetInputView(@TweetType int type, long statusId) {
    if (!fragment.isNewTweetCreatable()) {
      return;
    }
    fragment.expandTweetInputView(type, statusId);
    toggleToTweetInput(type);
  }

  private void toggleToTweetInput(@TweetType int type) {
    toggleTitleToTweetInput(type);
    toggleNavigationIconToTweetInput();
  }

  private void toggleToDefault() {
    currentType = TYPE_NONE;
    toolbar.setTitle(prevTitle);
    toolbar.setNavigationContentDescription(navContentDescriptionDefault);
    toolbar.setNavigationIcon(navIconDefault);
  }

  private void toggleNavigationIconToTweetInput() {
    navIconDefault = toolbar.getNavigationIcon();
    navContentDescriptionDefault = toolbar.getNavigationContentDescription();

    toolbar.setNavigationIcon(navigationIcon);
    toolbar.setNavigationContentDescription(R.string.navDesc_cancelTweet);
  }

  private @TweetType int currentType = TYPE_NONE;

  private void toggleTitleToTweetInput(@TweetType int type) {
    prevTitle = toolbar.getTitle();
    currentType = type;
    if (type == TYPE_REPLY) {
      toolbar.setTitle(R.string.title_reply);
    } else if (type == TYPE_QUOTE) {
      toolbar.setTitle(R.string.title_comment);
    } else {
      toolbar.setTitle(R.string.title_tweet);
    }
  }

  private void collapseTweetInputView() {
    if (!fragment.isTweetInputViewVisible()) {
      return;
    }
    fragment.collapseStatusInputView();
    toggleToDefault();
  }

  boolean onOptionMenuSelected(MenuItem item) {
    final int itemId = item.getItemId();
    if (itemId == R.id.action_writeTweet) {
      expandTweetInputView(TweetInputFragment.TYPE_DEFAULT, -1);
      return true;
    } else if (itemId == R.id.action_sendTweet) {
      collapseTweetInputView();
      return true;
    } else if (itemId == R.id.action_resumeTweet) {
      fragment.expandForResume();
      toggleToTweetInput(TweetInputFragment.TYPE_DEFAULT);
      return true;
    } else if (itemId == android.R.id.home && fragment.isTweetInputViewVisible()) {
      cancelInput();
      return true;
    }
    return false;
  }

  void cancelInput() {
    fragment.cancelInput();
    toggleToDefault();
  }

  boolean isOpened() {
    return fragment.isTweetInputViewVisible();
  }

  void changeCurrentUser() {
    fragment.changeCurrentUser();
  }

  TweetInputFragment getFragment() {
    return fragment;
  }

  private static final String SS_PREV_TITLE = "ss_prevTitle";
  private static final String SS_CURRENT_TYPE = "ss_currentType";

  void onSaveInstanceState(Bundle outState) {
    outState.putCharSequence(SS_PREV_TITLE, prevTitle);
    outState.putInt(SS_CURRENT_TYPE, currentType);
  }

  void onRestoreInstanceState(Bundle savedInstanceState) {
    currentType = savedInstanceState.getInt(SS_CURRENT_TYPE);
    if (currentType == TYPE_NONE) {
      return;
    } else {
      toggleToTweetInput(currentType);
    }
    prevTitle = savedInstanceState.getCharSequence(SS_PREV_TITLE);
  }
}
