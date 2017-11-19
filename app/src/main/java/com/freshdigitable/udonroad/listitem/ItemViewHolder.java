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

package com.freshdigitable.udonroad.listitem;

import android.support.annotation.CallSuper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import twitter4j.User;

/**
 * Created by akihit on 2017/06/14.
 */
public abstract class ItemViewHolder extends RecyclerView.ViewHolder {
  private Disposable subscription;
  OnItemViewClickListener itemViewClickListener;
  private OnUserIconClickedListener userIconClickedListener;

  public ItemViewHolder(View view) {
    super(view);
  }

  @CallSuper
  public void bind(final ListItem item, StatusViewImageLoader imageLoader) {
    itemView.setOnClickListener(v ->
        itemViewClickListener.onItemViewClicked(this, getItemId(), v));
    final User user = item.getUser();
    getUserIcon().setOnClickListener(
        v -> userIconClickedListener.onUserIconClicked(v, user));
  }

  public abstract ImageView getUserIcon();

  public void subscribe(Observable<ListItem> observable) {
    if (isSubscribed()) {
      return;
    }
    subscription = observable
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::onUpdate,
            th -> Log.e("ItemViewHolder", "update: ", th));
  }

  public abstract void onUpdate(ListItem item);

  public void unsubscribe() {
    if (isSubscribed()) {
      subscription.dispose();
    }
  }

  public boolean isSubscribed() {
    return subscription != null && !subscription.isDisposed();
  }

  @CallSuper
  public boolean hasSameItemId(long other) {
    return this.getItemId() == other || this.quotedStatusId == other;
  }

  @CallSuper
  public boolean hasQuotedItem() {
    return this.quotedStatusId > 0;
  }

  @CallSuper
  public void recycle() {
    quotedStatusId = -1;
    unsubscribe();
    subscription = null;
  }

  public void setItemViewClickListener(OnItemViewClickListener itemViewClickListener) {
    this.itemViewClickListener = itemViewClickListener;
  }

  public void setUserIconClickedListener(OnUserIconClickedListener userIconClickedListener) {
    this.userIconClickedListener = userIconClickedListener;
  }

  public abstract void onSelected(long itemId);

  public abstract void onUnselected(long itemId);

  long quotedStatusId;

  public long getQuotedItemId() {
    return quotedStatusId;
  }
}
