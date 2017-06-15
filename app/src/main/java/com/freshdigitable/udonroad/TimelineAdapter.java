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
import com.freshdigitable.udonroad.listitem.OnItemViewClickListener;
import com.freshdigitable.udonroad.listitem.OnUserIconClickedListener;
import com.freshdigitable.udonroad.listitem.StatusView;
import com.freshdigitable.udonroad.media.ThumbnailView;

import java.lang.ref.WeakReference;

import twitter4j.Status;
import twitter4j.User;

/**
 * TimelineAdapter is a adapter for RecyclerView.
 *
 * Created by akihit on 15/10/18.
 */
public class TimelineAdapter<T> extends RecyclerView.Adapter<ItemViewHolder<T>> {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineAdapter.class.getSimpleName();

  private final SortedCache<T> timelineStore;

  public TimelineAdapter(SortedCache<T> timelineStore) {
    this.timelineStore = timelineStore;
    setHasStableIds(true);
  }

  @Override
  public ItemViewHolder<T> onCreateViewHolder(ViewGroup parent, int viewType) {
    return new ItemViewHolder<>(new StatusView(parent.getContext()));
  }

  @Override
  public long getItemId(int position) {
    final T entity = timelineStore.get(position);
    return getEntityId(entity);
  }

  @Override
  public void onBindViewHolder(final ItemViewHolder<T> holder, int position) {
    final T entity = timelineStore.get(position);
    holder.bind(entity);

    if (position == getItemCount() - 1) {
      final long nextCursor = timelineStore.getLastPageCursor();
      if (nextCursor > 0) {
        lastItemBoundListener.onLastItemBound(nextCursor);
      }
    }

    bindSelectedItemView(holder);
  }

  private void bindSelectedItemView(ItemViewHolder<T> holder) {
    if (!isStatusViewSelected()) {
      return;
    }
    final long selectedEntityId = getSelectedEntityId();
    if (holder.getItemId() == selectedEntityId) {
      selectedEntityHolder = new SelectedEntity(holder);
    } else if (holder.hasQuotedEntity()) {
      final long quotedEntityId = holder.getQuotedEntityId();
      if (quotedEntityId == selectedEntityId) {
        selectedEntityHolder = new SelectedEntity(holder, quotedEntityId);
      }
    }
  }

  private long getEntityId(T entity) {
    if (entity instanceof Status) {
      return ((Status) entity).getId();
    } else if (entity instanceof User) {
      return ((User) entity).getId();
    }
    return -1;
  }

  @Override
  public void onViewAttachedToWindow(ItemViewHolder<T> holder) {
    super.onViewAttachedToWindow(holder);
    holder.subscribe(timelineStore.observeById(holder.getItemId()));
    holder.setItemViewClickListener(itemViewClickListener);
    holder.setUserIconClickedListener(userIconClickedListener);
  }

  @Override
  public void onViewDetachedFromWindow(ItemViewHolder<T> holder) {
    super.onViewDetachedFromWindow(holder);
    holder.setItemViewClickListener(null);
    holder.setUserIconClickedListener(null);
    holder.unsubscribe();
  }

  @Override
  public void onViewRecycled(ItemViewHolder<T> holder) {
    super.onViewRecycled(holder);
    if (holder.hasSameEntityId(selectedEntityHolder.entityId)) {
      selectedEntityHolder.onViewRecycled();
    }
    holder.recycle();
  }

  private static final SelectedEntity EMPTY = new SelectedEntity();
  private SelectedEntity selectedEntityHolder = EMPTY;

  private final OnItemViewClickListener itemViewClickListener = (vh, entityId, clickedItem) -> {
    if (isStatusViewSelected()
        && entityId == selectedEntityHolder.entityId) {
      if (clickedItem instanceof ThumbnailView) {
        return;
      }
      clearSelectedEntity();
    } else {
      fixSelectedEntity(entityId, vh);
    }
  };

  public void clearSelectedEntity() {
    if (isStatusViewSelected()) {
      selectedEntityHolder.setUnselectedBackground();
    }
    selectedEntityHolder = EMPTY;
    if (selectedEntityChangeListener != null) {
      selectedEntityChangeListener.onEntityUnselected();
    }
  }

  private void fixSelectedEntity(long selectedEntityId, ItemViewHolder selectedView) {
    if (isStatusViewSelected()) {
      selectedEntityHolder.setUnselectedBackground();
    }
    selectedEntityHolder = new SelectedEntity(selectedView, selectedEntityId);
    if (selectedEntityChangeListener != null) {
      selectedEntityChangeListener.onEntitySelected(selectedEntityId);
    }
  }

  public long getSelectedEntityId() {
    return selectedEntityHolder.entityId;
  }

  public boolean isStatusViewSelected() {
    return selectedEntityHolder != EMPTY;
  }

  public interface LastItemBoundListener {
    void onLastItemBound(long entityId);
  }

  private LastItemBoundListener lastItemBoundListener;

  public void setLastItemBoundListener(LastItemBoundListener listener) {
    this.lastItemBoundListener = listener;
  }

  private OnSelectedEntityChangeListener selectedEntityChangeListener;

  public void setOnSelectedEntityChangeListener(OnSelectedEntityChangeListener listener) {
    this.selectedEntityChangeListener = listener;
  }

  interface OnSelectedEntityChangeListener {
    void onEntitySelected(long entityId);
    void onEntityUnselected();
  }

  @Override
  public int getItemCount() {
    return timelineStore.getItemCount();
  }

  private static class SelectedEntity {
    private final long entityId;
    private final WeakReference<ItemViewHolder> viewHolder;

    private SelectedEntity() {
      this(null, -1);
    }

    private SelectedEntity(ItemViewHolder vh) {
      this(vh, vh.getItemId());
    }

    private SelectedEntity(ItemViewHolder vh, long entityId) {
      this.entityId = entityId;
      this.viewHolder = vh == null
          ? null
          : new WeakReference<>(vh);
      setSelectedBackground();
    }

    private void setSelectedBackground() {
      final ItemViewHolder vh = getViewHolder();
      if (vh != null) {
        vh.onSelected(entityId);
      }
    }

    private void setUnselectedBackground() {
      final ItemViewHolder vh = getViewHolder();
      if (vh != null) {
        vh.onUnselected(entityId);
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