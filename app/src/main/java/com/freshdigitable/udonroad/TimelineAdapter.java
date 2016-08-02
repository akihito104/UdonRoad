/*
 * Copyright (c) 2016. Akihito Matsuda (akihito104)
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

import android.content.Intent;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.freshdigitable.udonroad.datastore.TimelineStore;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import twitter4j.ExtendedMediaEntity;
import twitter4j.Status;
import twitter4j.User;

/**
 * Created by akihit on 15/10/18.
 */
public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineAdapter.class.getSimpleName();
  private final TimelineStore timelineStore;

  public TimelineAdapter(TimelineStore timelineStore) {
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

  public View getSelectedView() {
    return selectedStatusHolder.view;
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
    if (holder.statusId == status.getId()) {
//      Log.d(TAG, "onBindViewHolder: pos:" + position + ", " + status.toString());
      itemView.bindStatus(status);
      return;
    }
    holder.bindStatus(status);
    if (position == getItemCount() - 1) {
      lastItemBoundListener.onLastItemBound(status.getId());
    }

    holder.itemViewClicked = new OnItemViewClickListener() {
      @Override
      public void onItemViewClicked(final ViewHolder viewHolder) {
        if (viewHolder.hasSameStatusId(selectedStatusHolder)) {
          clearSelectedTweet();
        } else {
          fixSelectedTweet(viewHolder);
        }
      }
    };
    holder.userIconClickedListener = userIconClickedListener;

    if (status.getId() == getSelectedTweetId()) {
      setSelectedBackground(holder.itemView);
      selectedStatusHolder = new SelectedStatus(holder);
    }
    loadMediaView(status, itemView.getMediaContainer());

    final Status quotedStatus = status.isRetweet()
        ? status.getRetweetedStatus().getQuotedStatus()
        : status.getQuotedStatus();
    if (quotedStatus != null) {
      final QuotedStatusView quotedStatusView = itemView.getQuotedStatusView();
      Picasso.with(quotedStatusView.getContext())
          .load(quotedStatus.getUser().getMiniProfileImageURLHttps())
          .placeholder(android.R.color.transparent)
          .fit()
          .into(quotedStatusView.getIcon());
      loadMediaView(quotedStatus, quotedStatusView.getMediaContainer());
    }
  }

  private void loadMediaView(final Status status, MediaContainer mediaContainer) {
    ExtendedMediaEntity[] extendedMediaEntities = status.getExtendedMediaEntities();
    if (extendedMediaEntities.length < 1) {
      return;
    }
    final int mediaCount = mediaContainer.getThumbCount();
    for (int i = 0; i < mediaCount; i++) {
      final MediaImageView mediaView = (MediaImageView) mediaContainer.getChildAt(i);
      final int num = i;

      final String type = extendedMediaEntities[i].getType();
      mediaView.setShowIcon("video".equals(type) || "animated_gif".equals(type));

      mediaView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          final Intent intent = MediaViewActivity.create(view.getContext(), status, num);
          view.getContext().startActivity(intent);
        }
      });
      final RequestCreator rc = Picasso.with(mediaContainer.getContext())
          .load(extendedMediaEntities[i].getMediaURLHttps() + ":thumb")
          .placeholder(android.R.color.transparent);
      if (mediaContainer.getHeight() == 0 || mediaContainer.getThumbWidth() == 0) {
        rc.fit();
      } else {
        rc.resize(mediaContainer.getThumbWidth(), mediaContainer.getHeight());
      }
      rc.centerCrop()
          .into(mediaView);
    }
  }

  private void setSelectedBackground(View v) {
    final int color = ContextCompat.getColor(v.getContext(),
        R.color.colorTwitterActionNormalTransparent);
    v.setBackgroundColor(color);
  }

  @Override
  public void onViewAttachedToWindow(ViewHolder holder) {
    super.onViewAttachedToWindow(holder);
//    Log.d(TAG, "onViewAttachedToWindow: " + holder.status.toString());
  }

  @Override
  public void onViewDetachedFromWindow(ViewHolder holder) {
    super.onViewDetachedFromWindow(holder);
//    Log.d(TAG, "onViewDetachedFromWindow: " + holder.status.toString());
  }

  public void unloadMediaView(StatusViewBase v) {
    final MediaContainer mediaContainer = v.getMediaContainer();
    for (int i = 0; i < mediaContainer.getThumbCount(); i++) {
      final ImageView iv = (ImageView) mediaContainer.getChildAt(i);
      iv.setOnClickListener(null);
      Picasso.with(v.getContext()).cancelRequest(iv);
    }
  }

  private SelectedStatus selectedStatusHolder = null;

  private void fixSelectedTweet(ViewHolder vh) {
    if (isStatusViewSelected()) {
      selectedStatusHolder.view.setBackgroundColor(Color.TRANSPARENT);
    }
    selectedStatusHolder = new SelectedStatus(vh);
    setSelectedBackground(selectedStatusHolder.view);
    if (selectedTweetChangeListener != null) {
      selectedTweetChangeListener.onTweetSelected(selectedStatusHolder.statusId);
    }
  }

  public void clearSelectedTweet() {
    if (isStatusViewSelected()) {
      selectedStatusHolder.view.setBackgroundColor(Color.TRANSPARENT);
    }
    selectedStatusHolder = null;
    if (selectedTweetChangeListener != null) {
      selectedTweetChangeListener.onTweetUnselected();
    }
  }

  @Override
  public void onViewRecycled(ViewHolder holder) {
    super.onViewRecycled(holder);

    StatusView v = (StatusView) holder.itemView;
    Picasso.with(v.getContext()).cancelRequest(v.getIcon());
    if (v.getRtUserIcon().getVisibility() == View.VISIBLE) {
      Picasso.with(v.getContext()).cancelRequest(v.getRtUserIcon());
    }
    unloadMediaView(v);

    final QuotedStatusView quotedStatusView = v.getQuotedStatusView();
    if (quotedStatusView.getVisibility() == View.VISIBLE) {
      Picasso.with(v.getContext()).cancelRequest(quotedStatusView.getIcon());
      unloadMediaView(quotedStatusView);
    }

    holder.onRecycled();
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

    public ViewHolder(View itemView) {
      super(itemView);
    }

    void bindStatus(final Status status) {
      this.statusId = status.getId();
      StatusView v = (StatusView) itemView;
      final User user = status.isRetweet()
          ? status.getRetweetedStatus().getUser()
          : status.getUser();
      v.setUserIconClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          userIconClickedListener.onClicked(v, user);
        }
      });
      v.bindStatus(status);
      Picasso.with(v.getContext())
          .load(user.getProfileImageURLHttps())
          .placeholder(android.R.color.transparent)
          .fit()
          .into(v.getIcon());
      if (status.isRetweet()) {
        Picasso.with(v.getContext())
            .load(status.getUser().getMiniProfileImageURLHttps())
            .placeholder(android.R.color.transparent)
            .fit()
            .into(v.getRtUserIcon());
      }
      itemView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          itemViewClicked.onItemViewClicked(ViewHolder.this);
        }
      });
      final QuotedStatusView quotedStatusView = v.getQuotedStatusView();
      if (quotedStatusView.getVisibility() == View.VISIBLE) {
        quotedStatusView.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            itemViewClicked.onItemViewClicked(ViewHolder.this);
          }
        });
      }
    }

    boolean hasSameStatusId(SelectedStatus other) {
      return other != null && this.statusId == other.statusId;
    }

    private OnItemViewClickListener itemViewClicked;
    private OnUserIconClickedListener userIconClickedListener;

    void onRecycled() {
      this.itemView.setOnClickListener(null);
      final StatusView v = (StatusView) this.itemView;
      v.setUserIconClickListener(null);
      v.reset();
      this.statusId = -1;
      this.itemViewClicked = null;
      this.userIconClickedListener = null;
    }
  }

  private static class SelectedStatus {
    private final long statusId;
    private final View view;

    private SelectedStatus(ViewHolder viewHolder) {
      this(viewHolder.statusId, viewHolder.itemView);
    }

    private SelectedStatus(long statusId, View view) {
      this.statusId = statusId;
      this.view = view;
    }
  }

  interface OnUserIconClickedListener {
    void onClicked(View view, User user);
  }

  private OnUserIconClickedListener userIconClickedListener;

  public void setOnUserIconClickedListener(OnUserIconClickedListener listener) {
    this.userIconClickedListener = listener;
  }

  interface OnItemViewClickListener {
    void onItemViewClicked(ViewHolder viewHolder);
  }
}