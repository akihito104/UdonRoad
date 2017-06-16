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

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.media.MediaViewActivity;
import com.freshdigitable.udonroad.media.ThumbnailContainer;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by akihit on 2017/06/14.
 */
public class ItemViewHolder extends RecyclerView.ViewHolder {
  private Subscription subscription;
  private OnItemViewClickListener itemViewClickListener;
  private OnUserIconClickedListener userIconClickedListener;
  private long quotedStatusId;

  public ItemViewHolder(final ViewGroup parent, final int viewType) {
    super(new StatusView(parent.getContext()));
  }

  public void bind(final ListItem item) {
    final TwitterListItem twitterListItem = (TwitterListItem) item;
    getView().bind(twitterListItem);
    itemView.setOnClickListener(v ->
        itemViewClickListener.onItemViewClicked(this, getItemId(), v));
    getView().getIcon().setOnClickListener(
        v -> userIconClickedListener.onUserIconClicked(v, item.getUser()));
    StatusViewImageHelper.load(twitterListItem, getView());
    setupMediaView(item, getView());
    setupQuotedStatusView(twitterListItem, getView().getQuotedStatusView());
  }

  public void subscribe(Observable<ListItem> observable) {
    subscription = observable
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(item -> {
          getView().update((TwitterListItem) item);
        });
  }

  public void unsubscribe() {
    if (subscription != null && !subscription.isUnsubscribed()) {
      subscription.unsubscribe();
    }
  }

  public boolean hasSameItemId(long other) {
    return this.getItemId() == other || this.quotedStatusId == other;
  }

  public boolean hasQuotedItem() {
    return this.quotedStatusId > 0;
  }

  public void recycle() {
    unloadMediaView(getView());

    final QuotedStatusView quotedStatusView = getView().getQuotedStatusView();
    if (quotedStatusView.getVisibility() == View.VISIBLE) {
      unloadMediaView(quotedStatusView);
    }

    StatusViewImageHelper.unload(getView(), getItemId());
    getView().reset();

    quotedStatusId = -1;
    unsubscribe();
    subscription = null;
  }

  private StatusView getView() {
    return (StatusView) itemView;
  }

  private void setupMediaView(final ListItem item, final StatusViewBase statusView) {
    if (item.getMediaCount() < 1) {
      return;
    }
    final ThumbnailContainer thumbnailContainer = statusView.getThumbnailContainer();
    final long statusId = item.getId();
    thumbnailContainer.setOnMediaClickListener((view, index) -> {
      itemViewClickListener.onItemViewClicked(this, statusId, view);
      MediaViewActivity.start(view.getContext(), item, index);
    });
  }

  private void setupQuotedStatusView(TwitterListItem status, final QuotedStatusView quotedStatusView) {
    final ListItem quotedStatus = status.getQuotedItem();
    if (quotedStatus == null) {
      return;
    }
    quotedStatusId = quotedStatus.getId();
    quotedStatusView.setOnClickListener(
        view -> itemViewClickListener.onItemViewClicked(this, quotedStatusId, view));
    setupMediaView(quotedStatus, quotedStatusView);
  }

  private void unloadMediaView(StatusViewBase v) {
    final ThumbnailContainer thumbnailContainer = v.getThumbnailContainer();
    for (int i = 0; i < thumbnailContainer.getThumbCount(); i++) {
      thumbnailContainer.getChildAt(i).setOnClickListener(null);
    }
  }

  public void setItemViewClickListener(OnItemViewClickListener itemViewClickListener) {
    this.itemViewClickListener = itemViewClickListener;
  }

  public void setUserIconClickedListener(OnUserIconClickedListener userIconClickedListener) {
    this.userIconClickedListener = userIconClickedListener;
  }

  public void onSelected(long itemId) {
    if (getItemId() == itemId) {
      getView().setSelectedColor();
    } else if (quotedStatusId == itemId) {
      getView().getQuotedStatusView().setSelectedColor();
    }
  }

  public void onUnselected(long itemId) {
    if (getItemId() == itemId) {
      getView().setUnselectedColor();
    } else if (quotedStatusId == itemId) {
      getView().getQuotedStatusView().setUnselectedColor();
    }
  }

  public long getQuotedItemId() {
    return quotedStatusId;
  }
}
