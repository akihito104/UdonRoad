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

import com.freshdigitable.udonroad.media.MediaViewActivity;
import com.freshdigitable.udonroad.media.ThumbnailContainer;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.User;

import static com.freshdigitable.udonroad.Utils.getBindingStatus;

/**
 * Created by akihit on 2017/06/14.
 */
public class ItemViewHolder<T> extends RecyclerView.ViewHolder {
  private Subscription subscription;
  private OnItemViewClickListener itemViewClickListener;
  private OnUserIconClickedListener userIconClickedListener;
  private long quotedStatusId;

  public ItemViewHolder(View itemView) {
    super(itemView);
  }

  public void bind(T entity) {
    itemView.setOnClickListener(
        v -> itemViewClickListener.onItemViewClicked(this, getItemId(), v));
    if (entity instanceof Status) {
      final Status status = (Status) entity;
      getView().bindStatus(status);
      StatusViewImageHelper.load(status, getView());
      setupUserIcon(status, getView());
      setupMediaView(status, getView());
      setupQuotedStatusView(status, getView().getQuotedStatusView());
    } else if (entity instanceof User) {
      final User user = (User) entity;
      getView().bindUser(user);
      StatusViewImageHelper.load(user, getView());
      setupUserIcon(user, getView());
    }
  }

  public void subscribe(Observable<T> observable) {
    subscription = observable
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(entity -> {
          if (entity instanceof Status) {
            ((StatusView) itemView).update(((Status) entity));
          } else if (entity instanceof User) {
            ((StatusView) itemView).bindUser(((User) entity));
          }
        });
  }

  public void unsubscribe() {
    if (subscription != null && !subscription.isUnsubscribed()) {
      subscription.unsubscribe();
    }
  }

  public boolean hasSameEntityId(long other) {
    return this.getItemId() == other || this.quotedStatusId == other;
  }

  public boolean hasQuotedEntity() {
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

  private void setupUserIcon(Status status, StatusView itemView) {
    final User user = getBindingStatus(status).getUser();
    setupUserIcon(user, itemView);
  }

  private void setupUserIcon(final User user, StatusView itemView) {
    itemView.getIcon().setOnClickListener(
        v -> userIconClickedListener.onUserIconClicked(v, user));
  }

  private void setupMediaView(final Status status, final StatusViewBase statusView) {
    final MediaEntity[] mediaEntities = getBindingStatus(status).getMediaEntities();
    if (mediaEntities.length < 1) {
      return;
    }
    final ThumbnailContainer thumbnailContainer = statusView.getThumbnailContainer();
    final long statusId = status.getId();
    thumbnailContainer.setOnMediaClickListener((view, index) -> {
      itemViewClickListener.onItemViewClicked(this, statusId, view);
      MediaViewActivity.start(view.getContext(), status, index);
    });
  }

  private void setupQuotedStatusView(Status status, final QuotedStatusView quotedStatusView) {
    final Status quotedStatus = status.isRetweet()
        ? status.getRetweetedStatus().getQuotedStatus()
        : status.getQuotedStatus();
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

  public void onSelected(long entityId) {
    if (getItemId() == entityId) {
      getView().setSelectedColor();
    } else if (quotedStatusId == entityId) {
      getView().getQuotedStatusView().setSelectedColor();
    }
  }

  public void onUnselected(long entityId) {
    if (getItemId() == entityId) {
      getView().setUnselectedColor();
    } else if (quotedStatusId == entityId) {
      getView().getQuotedStatusView().setUnselectedColor();
    }
  }

  public long getQuotedEntityId() {
    return quotedStatusId;
  }
}
