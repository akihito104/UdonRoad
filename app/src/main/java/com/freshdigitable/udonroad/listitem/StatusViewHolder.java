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

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.freshdigitable.udonroad.Utils;
import com.freshdigitable.udonroad.media.MediaViewActivity;
import com.freshdigitable.udonroad.media.ThumbnailContainer;

import io.reactivex.disposables.Disposable;

/**
 * Created by akihit on 2017/07/12.
 */

public class StatusViewHolder extends ItemViewHolder {

  private Disposable imageSubs;

  public StatusViewHolder(ViewGroup parent) {
    super(new StatusView(parent.getContext()));
  }

  @Override
  public void bind(ListItem item, StatusViewImageLoader imageLoader) {
    super.bind(item, imageLoader);
    final TwitterListItem twitterListItem = (TwitterListItem) item;
    getView().bind(twitterListItem);
    Utils.maybeDispose(imageSubs);
    if (imageLoader != null) {
      imageSubs = imageLoader.load(twitterListItem, getView());
    }
    setupMediaView(twitterListItem, getView());
    setupQuotedStatusView(twitterListItem, getView().getQuotedStatusView());
  }

  @Override
  public ImageView getUserIcon() {
    return getView().getIcon();
  }

  @Override
  public void onUpdate(ListItem item) {
    getView().update((TwitterListItem) item);
  }

  @Override
  public void recycle() {
    super.recycle();
    getView().reset();
    unloadMediaView(getView());
    final QuotedStatusView quotedStatusView = getView().getQuotedStatusView();
    if (quotedStatusView != null && quotedStatusView.getVisibility() == View.VISIBLE) {
      unloadMediaView(quotedStatusView);
    }
    Utils.maybeDispose(imageSubs);
  }

  @Override
  public void onSelected(long itemId) {
    if (getItemId() == itemId) {
      getView().setSelectedColor();
    } else if (quotedStatusId == itemId) {
      final QuotedStatusView quotedStatusView = getView().getQuotedStatusView();
      if (quotedStatusView != null) {
        quotedStatusView.setSelectedColor();
      }
    }
  }

  @Override
  public void onUnselected(long itemId) {
    if (getItemId() == itemId) {
      getView().setUnselectedColor();
    } else if (quotedStatusId == itemId) {
      final QuotedStatusView quotedStatusView = getView().getQuotedStatusView();
      if (quotedStatusView != null) {
        quotedStatusView.setUnselectedColor();
      }
    }
  }

  private StatusView getView() {
    return (StatusView) itemView;
  }

  private void setupMediaView(final TwitterListItem item, final ThumbnailCapable statusView) {
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
    final TwitterListItem quotedStatus = status.getQuotedItem();
    if (quotedStatus == null) {
      return;
    }
    quotedStatusId = quotedStatus.getId();
    quotedStatusView.setOnClickListener(
        view -> itemViewClickListener.onItemViewClicked(this, quotedStatusId, view));
    setupMediaView(quotedStatus, quotedStatusView);
  }

  private void unloadMediaView(ThumbnailCapable v) {
    final ThumbnailContainer thumbnailContainer = v.getThumbnailContainer();
    for (int i = 0; i < thumbnailContainer.getThumbCount(); i++) {
      thumbnailContainer.getChildAt(i).setOnClickListener(null);
    }
  }

  public boolean hasQuotedView() {
    return getView().getQuotedStatusView() != null;
  }

  public void attachQuotedView(QuotedStatusView quotedView) {
    getView().attachQuotedView(quotedView);
  }

  public QuotedStatusView detachQuotedView() {
    return getView().detachQuotedView();
  }
}
