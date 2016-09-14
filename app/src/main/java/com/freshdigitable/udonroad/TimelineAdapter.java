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

import java.lang.ref.WeakReference;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import twitter4j.ExtendedMediaEntity;
import twitter4j.Status;
import twitter4j.User;

/**
 * TimelineAdapter is a adapter for RecyclerView.
 *
 * Created by akihit on 15/10/18.
 */
public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineAdapter.class.getSimpleName();
  private final SortedCache<Status> timelineStore;

  public TimelineAdapter(SortedCache<Status> timelineStore) {
    this.timelineStore = timelineStore;
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new ViewHolder(new StatusView(parent.getContext()));
  }

  public long getSelectedTweetId() {
    return isStatusViewSelected() ? selectedStatusHolder.statusId : -1;
  }

  public boolean isStatusViewSelected() {
    return selectedStatusHolder != null;
  }

  @Nullable
  public View getSelectedView() {
    return selectedStatusHolder.view.get();
  }

  public interface LastItemBoundListener {
    void onLastItemBound(long statusId);
  }

  private LastItemBoundListener lastItemBoundListener;

  public void setLastItemBoundListener(LastItemBoundListener listener) {
    this.lastItemBoundListener = listener;
  }

  @Override
  public void onBindViewHolder(final ViewHolder holder, int position) {
    final Status status = timelineStore.get(position);
    final StatusView itemView = (StatusView) holder.itemView;
    itemView.bindStatus(status);
    final long statusId = status.getId();
    if (holder.statusId == statusId) {
//      Log.d(TAG, "onBindViewHolder: pos:" + position + ", " + status.toString());
      return;
    }
    holder.setStatusId(status);
    holder.bind(timelineStore.observeById(statusId));
    if (position == getItemCount() - 1) {
      lastItemBoundListener.onLastItemBound(statusId);
    }

    final long quotedStatusId = status.getQuotedStatusId();
    if (statusId == getSelectedTweetId()) {
      selectedStatusHolder = new SelectedStatus(holder);
    } else if (quotedStatusId != -1 && quotedStatusId == getSelectedTweetId()) {
      final QuotedStatusView quotedStatusView = ((StatusView) holder.itemView).getQuotedStatusView();
      selectedStatusHolder = new SelectedStatus(quotedStatusId, quotedStatusView);
    }
    setupUserIcon(status, itemView);
    setupMediaView(status, itemView);
    setupQuotedStatusView(status, itemView.getQuotedStatusView());
    itemView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        itemViewClickListener.onItemViewClicked(itemView, statusId, v);
      }
    });
  }

  private void setupUserIcon(Status status, StatusView itemView) {
    final User user = StatusViewImageHelper.getBindingUser(status);
    itemView.getIcon().setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        userIconClickedListener.onClicked(v, user);
      }
    });
  }

  private void setupMediaView(final Status status, final StatusViewBase statusView) {
    ExtendedMediaEntity[] extendedMediaEntities = status.getExtendedMediaEntities();
    if (extendedMediaEntities.length < 1) {
      return;
    }
    final MediaContainer mediaContainer = statusView.getMediaContainer();
    final long statusId = status.getId();
    mediaContainer.setOnMediaClickListener(new MediaContainer.OnMediaClickListener() {
      @Override
      public void onMediaClicked(View view, int index) {
        itemViewClickListener.onItemViewClicked(statusView, statusId, view);
        MediaViewActivity.start(view.getContext(), status, index);
      }
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
    quotedStatusView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        itemViewClickListener.onItemViewClicked(quotedStatusView, quotedStatusId, view);
      }
    });
    setupMediaView(quotedStatus, quotedStatusView);
  }

  private void unloadMediaView(StatusViewBase v) {
    final MediaContainer mediaContainer = v.getMediaContainer();
    for (int i = 0; i < mediaContainer.getThumbCount(); i++) {
      mediaContainer.getChildAt(i).setOnClickListener(null);
    }
  }

  @Override
  public void onViewAttachedToWindow(ViewHolder holder) {
    super.onViewAttachedToWindow(holder);
    final Status status = timelineStore.find(holder.statusId);
    StatusViewImageHelper.load(status, (StatusView) holder.itemView);
  }

  @Override
  public void onViewDetachedFromWindow(ViewHolder holder) {
    super.onViewDetachedFromWindow(holder);
    StatusViewImageHelper.unload(holder.itemView.getContext(), holder.statusId);
  }

  private SelectedStatus selectedStatusHolder = null;

  private final OnItemViewClickListener itemViewClickListener = new OnItemViewClickListener() {
    @Override
    public void onItemViewClicked(StatusViewBase itemView, long statusId, View clickedItem) {
      if (isStatusViewSelected()
          && statusId == selectedStatusHolder.statusId) {
        if (clickedItem instanceof MediaImageView) {
          return;
        }
        clearSelectedTweet();
      } else {
        fixSelectedTweet(statusId, itemView);
      }
    }
  };

  public void clearSelectedTweet() {
    if (isStatusViewSelected()) {
      selectedStatusHolder.setUnselectedBackground();
    }
    selectedStatusHolder = null;
    if (selectedTweetChangeListener != null) {
      selectedTweetChangeListener.onTweetUnselected();
    }
  }

  private void fixSelectedTweet(long selectedStatusId, StatusViewBase selectedView) {
    if (isStatusViewSelected()) {
      selectedStatusHolder.setUnselectedBackground();
    }
    selectedStatusHolder = new SelectedStatus(selectedStatusId, selectedView);
    if (selectedTweetChangeListener != null) {
      selectedTweetChangeListener.onTweetSelected(selectedStatusId);
    }
  }

  @Override
  public void onViewRecycled(ViewHolder holder) {
    final StatusView v = (StatusView) holder.itemView;
    unloadMediaView(v);

    final QuotedStatusView quotedStatusView = v.getQuotedStatusView();
    if (quotedStatusView.getVisibility() == View.VISIBLE) {
      unloadMediaView(quotedStatusView);
    }
    if (holder.hasSameStatusId(selectedStatusHolder)) {
      selectedStatusHolder.onViewRecycled();
    }
    v.setOnClickListener(null);
    v.getQuotedStatusView().setOnClickListener(null);
    v.getIcon().setOnClickListener(null);
    v.getMediaContainer().setOnMediaClickListener(null);
    v.reset();
    holder.onRecycled();
    super.onViewRecycled(holder);
  }

  private OnSelectedTweetChangeListener selectedTweetChangeListener;

  public void setOnSelectedTweetChangeListener(OnSelectedTweetChangeListener listener) {
    this.selectedTweetChangeListener = listener;
  }

  interface OnSelectedTweetChangeListener {
    void onTweetSelected(long statusId);
    void onTweetUnselected();
  }

  @Override
  public int getItemCount() {
    return timelineStore.getItemCount();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    private long statusId;
    private Subscription subscription;

    public ViewHolder(View itemView) {
      super(itemView);
    }

    void setStatusId(final Status status) {
      this.statusId = status.getId();
    }

    void bind(Observable<Status> observable) {
      subscription = observable.subscribe(new Action1<Status>() {
        @Override
        public void call(Status status) {
          ((StatusView) itemView).bindStatus(status);
          itemView.invalidate();
        }
      });
    }

    void unbind() {
      if (subscription != null && !subscription.isUnsubscribed()) {
        subscription.unsubscribe();
      }
    }

    boolean hasSameStatusId(SelectedStatus other) {
      return other != null && this.statusId == other.statusId;
    }

    void onRecycled() {
      this.statusId = -1;
      unbind();
      subscription = null;
    }
  }

  private static class SelectedStatus {
    private final long statusId;
    private final WeakReference<? extends StatusViewBase> view;

    private SelectedStatus(ViewHolder viewHolder) {
      this(viewHolder.statusId, (StatusViewBase) viewHolder.itemView);
    }

    private SelectedStatus(long statusId, StatusViewBase view) {
      this.statusId = statusId;
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
    void onItemViewClicked(StatusViewBase itemView, long statusId, View clickedItem);
  }
}