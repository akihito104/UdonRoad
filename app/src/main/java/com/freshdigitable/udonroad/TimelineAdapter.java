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

import com.freshdigitable.udonroad.StatusViewBase.OnUserIconClickedListener;
import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.media.MediaViewActivity;
import com.freshdigitable.udonroad.media.ThumbnailContainer;
import com.freshdigitable.udonroad.media.ThumbnailView;

import java.lang.ref.WeakReference;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.User;

import static com.freshdigitable.udonroad.Utils.getBindingStatus;

/**
 * TimelineAdapter is a adapter for RecyclerView.
 *
 * Created by akihit on 15/10/18.
 */
public class TimelineAdapter<T> extends RecyclerView.Adapter<TimelineAdapter.ViewHolder<T>> {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineAdapter.class.getSimpleName();

  private final SortedCache<T> timelineStore;

  public TimelineAdapter(SortedCache<T> timelineStore) {
    this.timelineStore = timelineStore;
  }

  @Override
  public ViewHolder<T> onCreateViewHolder(ViewGroup parent, int viewType) {
    return new ViewHolder<>(new StatusView(parent.getContext()));
  }

  @Override
  public void onBindViewHolder(final ViewHolder<T> holder, int position) {
    final T entity = timelineStore.get(position);
    final StatusView itemView = (StatusView) holder.itemView;
    final long entityId = getEntityId(entity);
    holder.setEntityId(entityId);
    if (position == getItemCount() - 1) {
      final long nextCursor = timelineStore.getLastPageCursor();
      if (nextCursor > 0) {
        lastItemBoundListener.onLastItemBound(nextCursor);
      }
    }
    itemView.setOnClickListener(
        v -> itemViewClickListener.onItemViewClicked(itemView, entityId, v));

    if (entity instanceof Status) {
      final Status status = (Status) entity;
      itemView.bindStatus(status);
      final long quotedStatusId = status.getQuotedStatusId();
      if (entityId == getSelectedEntityId()) {
        selectedEntityHolder = new SelectedEntity(holder);
      } else if (quotedStatusId != -1 && quotedStatusId == getSelectedEntityId()) {
        final QuotedStatusView quotedStatusView = ((StatusView) holder.itemView).getQuotedStatusView();
        selectedEntityHolder = new SelectedEntity(quotedStatusId, quotedStatusView);
      }
      StatusViewImageHelper.load(status, (StatusView) holder.itemView);
      setupUserIcon(status, itemView);
      setupMediaView(status, itemView);
      setupQuotedStatusView(status, itemView.getQuotedStatusView());
    } else if (entity instanceof User) {
      final User user = (User) entity;
      itemView.bindUser(user);
      if (entityId == getSelectedEntityId()) {
        selectedEntityHolder = new SelectedEntity(holder);
      }
      StatusViewImageHelper.load(user, (StatusView) holder.itemView);
      setupUserIcon(user, itemView);
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
      itemViewClickListener.onItemViewClicked(statusView, statusId, view);
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
    final long quotedStatusId = quotedStatus.getId();
    quotedStatusView.setOnClickListener(
        view -> itemViewClickListener.onItemViewClicked(quotedStatusView, quotedStatusId, view));
    setupMediaView(quotedStatus, quotedStatusView);
  }

  private void unloadMediaView(StatusViewBase v) {
    final ThumbnailContainer thumbnailContainer = v.getThumbnailContainer();
    for (int i = 0; i < thumbnailContainer.getThumbCount(); i++) {
      thumbnailContainer.getChildAt(i).setOnClickListener(null);
    }
  }

  @Override
  public void onViewAttachedToWindow(ViewHolder<T> holder) {
    super.onViewAttachedToWindow(holder);
    holder.bind(timelineStore.observeById(holder.entityId));
  }

  @Override
  public void onViewDetachedFromWindow(ViewHolder<T> holder) {
    super.onViewDetachedFromWindow(holder);
    holder.unbind();
  }

  @Override
  public void onViewRecycled(ViewHolder<T> holder) {
    super.onViewRecycled(holder);
    final StatusView v = (StatusView) holder.itemView;
    unloadMediaView(v);

    final QuotedStatusView quotedStatusView = v.getQuotedStatusView();
    if (quotedStatusView.getVisibility() == View.VISIBLE) {
      unloadMediaView(quotedStatusView);
    }
    if (holder.hasSameEntityId(selectedEntityHolder)) {
      selectedEntityHolder.onViewRecycled();
    }
    StatusViewImageHelper.unload((FullStatusView) holder.itemView, holder.entityId);
    v.setOnClickListener(null);
    v.getQuotedStatusView().setOnClickListener(null);
    v.getIcon().setOnClickListener(null);
    v.getThumbnailContainer().setOnMediaClickListener(null);
    v.reset();
    holder.onRecycled();
  }

  static class ViewHolder<T> extends RecyclerView.ViewHolder {
    private long entityId;
    private Subscription subscription;

    ViewHolder(View itemView) {
      super(itemView);
    }

    void setEntityId(long id) {
      entityId = id;
    }

    void bind(Observable<T> observable) {
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

    void unbind() {
      if (subscription != null && !subscription.isUnsubscribed()) {
        subscription.unsubscribe();
      }
    }

    boolean hasSameEntityId(SelectedEntity other) {
      return other != null && this.entityId == other.entityId;
    }

    void onRecycled() {
      this.entityId = -1;
      unbind();
      subscription = null;
    }
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

    private SelectedEntity(ViewHolder viewHolder) {
      this(viewHolder.entityId, (StatusViewBase) viewHolder.itemView);
    }

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

  interface OnItemViewClickListener {
    void onItemViewClicked(StatusViewBase itemView, long entityId, View clickedItem);
  }
}