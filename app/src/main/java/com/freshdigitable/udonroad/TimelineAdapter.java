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

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.listitem.ItemViewHolder;
import com.freshdigitable.udonroad.listitem.ListItem;
import com.freshdigitable.udonroad.listitem.OnItemViewClickListener;
import com.freshdigitable.udonroad.listitem.OnUserIconClickedListener;
import com.freshdigitable.udonroad.listitem.StatusListItem;
import com.freshdigitable.udonroad.listitem.UserListItem;
import com.freshdigitable.udonroad.media.ThumbnailView;

import java.lang.ref.WeakReference;

import rx.Observable;
import twitter4j.Status;
import twitter4j.User;

/**
 * TimelineAdapter is a adapter for RecyclerView.
 *
 * Created by akihit on 15/10/18.
 */
public class TimelineAdapter<T> extends RecyclerView.Adapter<ItemViewHolder> {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineAdapter.class.getSimpleName();

  private final SortedCache<T> timelineStore;

  public TimelineAdapter(SortedCache<T> timelineStore) {
    this.timelineStore = timelineStore;
    setHasStableIds(true);
  }

  @Override
  public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new ItemViewHolder(parent, viewType);
  }

  @Override
  public long getItemId(int position) {
    final ListItem item = wrapListItem(position);
    return item != null ? item.getId() : -1;
  }

  private ListItem wrapListItem(int position) {
    final T item = timelineStore.get(position);
    return wrapListItem(item);
  }

  @Nullable
  private static <T> ListItem wrapListItem(T item) {
    if (item instanceof Status) {
      return new StatusListItem((Status) item);
    } else if (item instanceof User) {
      return new UserListItem((User) item);
    }
    return null;
  }

  @Override
  public void onBindViewHolder(final ItemViewHolder holder, int position) {
    final ListItem item = wrapListItem(position);
    if (item == null) {
      return;
    }
    holder.bind(item);

    if (position == getItemCount() - 1) {
      final long nextCursor = timelineStore.getLastPageCursor();
      if (nextCursor > 0) {
        lastItemBoundListener.onLastItemBound(nextCursor);
      }
    }

    bindSelectedItemView(holder);
  }

  private void bindSelectedItemView(ItemViewHolder holder) {
    if (!isItemSelected()) {
      return;
    }
    final long selectedItemId = getSelectedItemId();
    if (holder.getItemId() == selectedItemId) {
      selectedItemHolder = new SelectedItem(holder);
    } else if (holder.hasQuotedItem()) {
      final long quotedItemId = holder.getQuotedItemId();
      if (quotedItemId == selectedItemId) {
        selectedItemHolder = new SelectedItem(holder, quotedItemId);
      }
    }
  }

  @Override
  public void onViewAttachedToWindow(ItemViewHolder holder) {
    super.onViewAttachedToWindow(holder);
    final Observable<ListItem> observable
        = timelineStore.observeById(holder.getItemId())
        .map(TimelineAdapter::wrapListItem);
    holder.subscribe(observable);
    holder.setItemViewClickListener(itemViewClickListener);
    holder.setUserIconClickedListener(userIconClickedListener);
  }

  @Override
  public void onViewDetachedFromWindow(ItemViewHolder holder) {
    super.onViewDetachedFromWindow(holder);
    holder.setItemViewClickListener(null);
    holder.setUserIconClickedListener(null);
    holder.unsubscribe();
  }

  @Override
  public void onViewRecycled(ItemViewHolder holder) {
    super.onViewRecycled(holder);
    if (holder.hasSameItemId(selectedItemHolder.id)) {
      selectedItemHolder.onViewRecycled();
    }
    holder.recycle();
  }

  private static final SelectedItem EMPTY = new SelectedItem();
  private SelectedItem selectedItemHolder = EMPTY;

  private final OnItemViewClickListener itemViewClickListener = (vh, itemId, clickedItem) -> {
    if (isItemSelected()
        && itemId == selectedItemHolder.id) {
      if (clickedItem instanceof ThumbnailView) {
        return;
      }
      clearSelectedItem();
    } else {
      fixSelectedItem(itemId, vh);
    }
  };

  public void clearSelectedItem() {
    if (isItemSelected()) {
      selectedItemHolder.setUnselectedBackground();
    }
    selectedItemHolder = EMPTY;
    if (selectedItemChangeListener != null) {
      selectedItemChangeListener.onItemUnselected();
    }
  }

  private void fixSelectedItem(long selectedItemId, ItemViewHolder selectedView) {
    if (isItemSelected()) {
      selectedItemHolder.setUnselectedBackground();
    }
    selectedItemHolder = new SelectedItem(selectedView, selectedItemId);
    if (selectedItemChangeListener != null) {
      selectedItemChangeListener.onItemSelected(selectedItemId);
    }
  }

  public long getSelectedItemId() {
    return selectedItemHolder.id;
  }

  public boolean isItemSelected() {
    return selectedItemHolder != EMPTY;
  }

  public int getSelectedItemViewPosition() {
    return timelineStore.getPositionById(selectedItemHolder.containerId);
  }

  public interface LastItemBoundListener {
    void onLastItemBound(long itemId);
  }

  private LastItemBoundListener lastItemBoundListener;

  public void setLastItemBoundListener(LastItemBoundListener listener) {
    this.lastItemBoundListener = listener;
  }

  private OnSelectedItemChangeListener selectedItemChangeListener;

  public void setOnSelectedItemChangeListener(OnSelectedItemChangeListener listener) {
    this.selectedItemChangeListener = listener;
  }

  interface OnSelectedItemChangeListener {
    void onItemSelected(long itemId);
    void onItemUnselected();
  }

  @Override
  public int getItemCount() {
    return timelineStore.getItemCount();
  }

  private static class SelectedItem {
    private final long id;
    private final long containerId;
    private final WeakReference<ItemViewHolder> viewHolder;

    private SelectedItem() {
      this(null, -1);
    }

    private SelectedItem(ItemViewHolder vh) {
      this(vh, vh.getItemId());
    }

    private SelectedItem(ItemViewHolder vh, long id) {
      this.id = id;
      this.containerId = vh != null ? vh.getItemId() : -1;
      this.viewHolder = vh != null
          ? new WeakReference<>(vh)
          : null;
      setSelectedBackground();
    }

    private void setSelectedBackground() {
      final ItemViewHolder vh = getViewHolder();
      if (vh != null) {
        vh.onSelected(id);
      }
    }

    private void setUnselectedBackground() {
      final ItemViewHolder vh = getViewHolder();
      if (vh != null) {
        vh.onUnselected(id);
      }
    }

    private void onViewRecycled() {
      final ItemViewHolder viewHolder = getViewHolder();
      if (viewHolder != null) {
        this.viewHolder.clear();
      }
    }

    @Nullable
    private ItemViewHolder getViewHolder() {
      return this.viewHolder != null ? this.viewHolder.get() : null;
    }
  }

  private OnUserIconClickedListener userIconClickedListener;

  public void setOnUserIconClickedListener(OnUserIconClickedListener listener) {
    this.userIconClickedListener = listener;
  }
}