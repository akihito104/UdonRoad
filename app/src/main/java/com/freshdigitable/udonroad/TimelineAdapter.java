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
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.listitem.ItemViewHolder;
import com.freshdigitable.udonroad.listitem.OnItemViewClickListener;
import com.freshdigitable.udonroad.listitem.OnUserIconClickedListener;
import com.freshdigitable.udonroad.listitem.QuotedStatusView;
import com.freshdigitable.udonroad.listitem.StatusView;
import com.freshdigitable.udonroad.listitem.StatusViewBase;
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
  }

  @Override
  public ItemViewHolder<T> onCreateViewHolder(ViewGroup parent, int viewType) {
    return new ItemViewHolder<>(new StatusView(parent.getContext()));
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

    final long entityId = getEntityId(entity);
    if (entityId == getSelectedEntityId()) {
      selectedEntityHolder = new SelectedEntity(entityId, (StatusView) holder.itemView);
    } else if (entity instanceof Status) {
      final Status status = (Status) entity;
      final long quotedStatusId = status.getQuotedStatusId();
      if (quotedStatusId != -1 && quotedStatusId == getSelectedEntityId()) {
        final QuotedStatusView quotedStatusView = ((StatusView) holder.itemView).getQuotedStatusView();
        selectedEntityHolder = new SelectedEntity(quotedStatusId, quotedStatusView);
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
    holder.subscribe(timelineStore.observeById(holder.getEntityId()));
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
    if (selectedEntityHolder != null && holder.hasSameEntityId(selectedEntityHolder.entityId)) {
      selectedEntityHolder.onViewRecycled();
    }
    holder.recycle();
  }

  private SelectedEntity selectedEntityHolder = null;

  private final OnItemViewClickListener itemViewClickListener = (itemView, entityId, clickedItem) -> {
    if (isStatusViewSelected()
        && entityId == selectedEntityHolder.entityId) {
      if (clickedItem instanceof ThumbnailView) {
        return;
      }
      clearSelectedEntity();
    } else {
      fixSelectedEntity(entityId, itemView);
    }
  };

  public void clearSelectedEntity() {
    if (isStatusViewSelected()) {
      selectedEntityHolder.setUnselectedBackground();
    }
    selectedEntityHolder = null;
    if (selectedEntityChangeListener != null) {
      selectedEntityChangeListener.onEntityUnselected();
    }
  }

  private void fixSelectedEntity(long selectedEntityId, StatusViewBase selectedView) {
    if (isStatusViewSelected()) {
      selectedEntityHolder.setUnselectedBackground();
    }
    selectedEntityHolder = new SelectedEntity(selectedEntityId, selectedView);
    if (selectedEntityChangeListener != null) {
      selectedEntityChangeListener.onEntitySelected(selectedEntityId);
    }
  }

  public long getSelectedEntityId() {
    return isStatusViewSelected() ? selectedEntityHolder.entityId : -1;
  }

  public boolean isStatusViewSelected() {
    return selectedEntityHolder != null;
  }

  @Nullable
  public View getSelectedView() {
    return selectedEntityHolder.view.get();
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
    private final WeakReference<? extends StatusViewBase> view;

    private SelectedEntity(long entityId, StatusViewBase view) {
      this.entityId = entityId;
      this.view = view == null
          ? null
          : new WeakReference<>(view);
      setSelectedBackground();
    }

    private void setSelectedBackground() {
      final StatusViewBase view = this.view.get();
      if (view == null) {
        return;
      }
      view.setSelectedColor();
    }

    private void setUnselectedBackground() {
      final StatusViewBase view = this.view.get();
      if (view == null) {
        return;
      }
      view.setUnselectedColor();
    }

    private void onViewRecycled() {
      view.clear();
    }
  }

  private OnUserIconClickedListener userIconClickedListener;

  public void setOnUserIconClickedListener(OnUserIconClickedListener listener) {
    this.userIconClickedListener = listener;
  }
}