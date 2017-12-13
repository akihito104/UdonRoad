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

package com.freshdigitable.udonroad.input;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.Utils;

import io.reactivex.disposables.Disposable;

/**
 * Created by akihit on 2017/12/04.
 */

class TweetSendPresenter implements LifecycleObserver {
  private final SparseArray<MenuItem> menuItems = new SparseArray<>();
  private final Disposable modelSubs;
  private final TweetInputViewModel viewModel;

  TweetSendPresenter(Menu menu, MenuInflater inflater, TweetInputViewModel viewModel) {
    inflater.inflate(R.menu.tweet_input, menu);
    for (@IdRes int resId : new int[]{R.id.action_writeTweet, R.id.action_sendTweet, R.id.action_resumeTweet}) {
      menuItems.put(resId, menu.findItem(resId));
    }
    modelSubs = viewModel.observeModel().subscribe(model -> {

    });
    this.viewModel = viewModel;
  }

  void setupMenuAvailability(@IdRes int visibleItemId) {
    for (int i = 0; i < menuItems.size(); i++) {
      final MenuItem item = menuItems.valueAt(i);
      item.setVisible(item.getItemId() == visibleItemId);
      setupItemEnable(item, viewModel.getModel());
    }
  }

  private void setupItemEnable(@NonNull MenuItem item, TweetInputModel model) {
    final int itemId = item.getItemId();
    if (!item.isVisible()) {
      return;
    }
    if (itemId == R.id.action_writeTweet) {
      item.setEnabled(model.isCleared());
    } else if (itemId == R.id.action_sendTweet) {
      item.setEnabled(!TextUtils.isEmpty(model.getText()));
    }
  }

  void onItemClicked(MenuItem item) {
    final int itemId = item.getItemId();
    if (itemId == R.id.action_sendTweet) {
      menuItems.get(R.id.action_sendTweet).setEnabled(false);
    }
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  public void onDestroy() {
    Utils.maybeDispose(modelSubs);
    menuItems.clear();
  }
}

