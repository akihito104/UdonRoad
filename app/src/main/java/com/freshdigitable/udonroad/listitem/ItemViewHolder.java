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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.media.MediaViewActivity;
import com.freshdigitable.udonroad.media.ThumbnailContainer;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * Created by akihit on 2017/06/14.
 */
public class ItemViewHolder extends RecyclerView.ViewHolder {
  public static final int TYPE_STATUS = 1;
  public static final int TYPE_USER = 2;
  private Disposable subscription;
  private OnItemViewClickListener itemViewClickListener;
  private OnUserIconClickedListener userIconClickedListener;
  private final BindDelegator<? extends ItemView> binder;

  public ItemViewHolder(final ViewGroup parent, final int viewType) {
    super(create(parent, viewType));
    binder = createBinder();
  }

  private BindDelegator<? extends ItemView> createBinder() {
    if (itemView instanceof StatusView) {
      return new StatusViewBinder(this);
    } else {
      return new UserViewBinder(this);
    }
  }

  private static ItemView create(final ViewGroup parent, final int viewType) {
    if (viewType == TYPE_STATUS) {
      return new StatusView(parent.getContext());
    } else {
      return new UserItemView(parent.getContext());
    }
  }

  public void bind(final ListItem item) {
    final TwitterListItem twitterListItem = (TwitterListItem) item;
    binder.bind(twitterListItem);
    itemView.setOnClickListener(v ->
        itemViewClickListener.onItemViewClicked(this, getItemId(), v));
    getView().getIcon().setOnClickListener(
        v -> userIconClickedListener.onUserIconClicked(v, item.getUser()));
  }

  public void subscribe(Observable<ListItem> observable) {
    subscription = observable
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(binder::onUpdate,
            th -> Log.e("ItemViewHolder", "update: ", th));
  }

  public void unsubscribe() {
    if (subscription != null && !subscription.isDisposed()) {
      subscription.dispose();
    }
  }

  public boolean hasSameItemId(long other) {
    return this.getItemId() == other || this.quotedStatusId == other;
  }

  public boolean hasQuotedItem() {
    return this.quotedStatusId > 0;
  }

  public void recycle() {
    binder.recycle();
    getView().reset();

    quotedStatusId = -1;
    unsubscribe();
    subscription = null;
  }

  private ItemView getView() {
    return binder.getView();
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

  public void setItemViewClickListener(OnItemViewClickListener itemViewClickListener) {
    this.itemViewClickListener = itemViewClickListener;
  }

  public void setUserIconClickedListener(OnUserIconClickedListener userIconClickedListener) {
    this.userIconClickedListener = userIconClickedListener;
  }

  public void onSelected(long itemId) {
    binder.onSelected(itemId);
  }

  public void onUnselected(long itemId) {
    binder.onUnselected(itemId);
  }

  private long quotedStatusId;
  public long getQuotedItemId() {
    return quotedStatusId;
  }

  interface BindDelegator<T> {
    void bind(TwitterListItem item);

    void onUpdate(ListItem item);

    void recycle();

    void onSelected(long itemId);

    void onUnselected(long itemId);

    T getView();
  }

  private static class StatusViewBinder implements BindDelegator<StatusView> {
    private final ItemViewHolder holder;

    StatusViewBinder(ItemViewHolder holder) {
      this.holder = holder;
    }

    @Override
    public void bind(TwitterListItem item) {
      getView().bind(item);
      StatusViewImageHelper.load(item, getView());
      holder.setupMediaView(item, getView());
      holder.setupQuotedStatusView(item, getView().getQuotedStatusView());
    }

    @Override
    public void onUpdate(ListItem item) {
      getView().update((TwitterListItem) item);
    }

    @Override
    public void recycle() {
      holder.unloadMediaView(getView());
      final QuotedStatusView quotedStatusView = getView().getQuotedStatusView();
      if (quotedStatusView != null && quotedStatusView.getVisibility() == View.VISIBLE) {
        holder.unloadMediaView(quotedStatusView);
      }
      StatusViewImageHelper.unload(getView(), holder.getItemId());
    }

    @Override
    public void onSelected(long itemId) {
      if (holder.getItemId() == itemId) {
        getView().setSelectedColor();
      } else if (holder.quotedStatusId == itemId) {
        final QuotedStatusView quotedStatusView = getView().getQuotedStatusView();
        if (quotedStatusView != null) {
          quotedStatusView.setSelectedColor();
        }
      }
    }

    @Override
    public void onUnselected(long itemId) {
      if (holder.getItemId() == itemId) {
        getView().setUnselectedColor();
      } else if (holder.quotedStatusId == itemId) {
        final QuotedStatusView quotedStatusView = getView().getQuotedStatusView();
        if (quotedStatusView != null) {
          quotedStatusView.setUnselectedColor();
        }
      }
    }

    @Override
    public StatusView getView() {
      return (StatusView) holder.itemView;
    }
  }

  private static class UserViewBinder implements BindDelegator<UserItemView> {
    private final ItemViewHolder holder;

    UserViewBinder(ItemViewHolder viewHolder) {
      this.holder = viewHolder;
    }

    @Override
    public void bind(TwitterListItem item) {
      getView().bind(item);
      StatusViewImageHelper.loadUserIcon(item.getUser(), holder.getItemId(), getView());
    }

    @Override
    public void onUpdate(ListItem item) {
      getView().update((TwitterListItem) item);
    }

    @Override
    public void recycle() {
      StatusViewImageHelper.unloadUserIcon(getView());
    }

    @Override
    public void onSelected(long itemId) {
    }

    @Override
    public void onUnselected(long itemId) {
    }

    @Override
    public UserItemView getView() {
      return (UserItemView) holder.itemView;
    }
  }
}
