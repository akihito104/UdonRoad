package com.freshdigitable.udonroad;

import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.util.LinkedList;
import java.util.List;

import twitter4j.ExtendedMediaEntity;
import twitter4j.Status;
import twitter4j.User;

/**
 * Created by akihit on 15/10/18.
 */
public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineAdapter.class.getSimpleName();

  public TimelineAdapter() {
    this(new LinkedList<Status>());
  }

  private TimelineAdapter(List<Status> statuses) {
    this.statuses = new LinkedList<>(statuses);
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new ViewHolder(new StatusView(parent.getContext()));
  }

  private final List<Status> statuses;

  public long getSelectedTweetId() {
    return isStatusViewSelected() ? selectedStatusHolder.status.getId() : -1;
  }

  public boolean isStatusViewSelected() {
    return selectedStatusHolder != null;
  }

  public Status getSelectedStatus() {
    return isStatusViewSelected() ? selectedStatusHolder.status : null;
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
    Status status = get(position);
//    Log.d(TAG, "onBindViewHolder: pos:" + position + ", " + status.toString());
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

    final StatusView itemView = (StatusView) holder.itemView;
    final ExtendedMediaEntity[] extendedMediaEntities = status.getExtendedMediaEntities();
    if (extendedMediaEntities != null) {
      final int mediaHeight = itemView.getMediaHeight();
      final int mediaWidth = itemView.getMediaWidth();
      final ImageView[] mediaImages = itemView.getMediaImages();
      loadMediaView(extendedMediaEntities, mediaHeight, mediaWidth, mediaImages);
    }

    final Status quotedStatus = status.isRetweet()
        ? status.getRetweetedStatus().getQuotedStatus()
        : status.getQuotedStatus();
    if (quotedStatus != null) {
      final QuotedStatusView quotedStatusView = itemView.getQuotedStatusView();
      Picasso.with(quotedStatusView.getContext())
          .load(quotedStatus.getUser().getMiniProfileImageURLHttps())
          .fit()
          .into(quotedStatusView.getIcon());
      loadMediaView(quotedStatus.getExtendedMediaEntities(),
          quotedStatusView.getMediaHeight(), quotedStatusView.getMediaWidth(),
          quotedStatusView.getMediaImages());
    }
  }

  private void loadMediaView(ExtendedMediaEntity[] extendedMediaEntities,
                               int mediaHeight, int mediaWidth,
                               ImageView[] mediaImages) {
    for (int i = 0; i < extendedMediaEntities.length; i++) {
      final RequestCreator rc = Picasso.with(mediaImages[i].getContext())
          .load(extendedMediaEntities[i].getMediaURLHttps() + ":thumb");
      if (mediaHeight == 0 || mediaWidth == 0) {
        rc.fit();
      } else {
        rc.resize(mediaWidth, mediaHeight);
      }
      rc.centerCrop()
          .into(mediaImages[i]);
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
    ViewCompat.animate(holder.itemView).cancel();

    StatusView v = (StatusView) holder.itemView;
    Picasso.with(v.getContext()).cancelRequest(v.getIcon());
    if (v.getRtUserIcon().getVisibility() == View.VISIBLE) {
      Picasso.with(v.getContext()).cancelRequest(v.getRtUserIcon());
    }
    if (v.getMediaImages()[0].getVisibility() == View.VISIBLE) {
      for (ImageView mi : v.getMediaImages()) {
        Picasso.with(v.getContext()).cancelRequest(mi);
      }
    }

    final QuotedStatusView quotedStatusView = v.getQuotedStatusView();
    if (quotedStatusView.getVisibility() == View.VISIBLE) {
      Picasso.with(v.getContext()).cancelRequest(quotedStatusView.getIcon());
      if (quotedStatusView.getMediaImages()[0].getVisibility() == View.VISIBLE) {
        for (ImageView mi : v.getMediaImages()) {
          Picasso.with(v.getContext()).cancelRequest(mi);
        }
      }
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
      selectedTweetChangeListener.onTweetSelected(selectedStatusHolder.status);
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
    holder.onRecycled();
  }

  private OnSelectedTweetChangeListener selectedTweetChangeListener;

  public void setOnSelectedTweetChangeListener(OnSelectedTweetChangeListener listener) {
    this.selectedTweetChangeListener = listener;
  }

  interface OnSelectedTweetChangeListener {
    void onTweetSelected(Status status);
    void onTweetUnselected();
  }

  protected Status get(int position) {
    return statuses.get(position);
  }

  @Override
  public int getItemCount() {
    return this.statuses.size();
  }

  public void addNewStatus(Status status) {
    statuses.add(0, status);
    notifyItemInserted(0);
  }

  public void addNewStatuses(List<Status> statuses) {
    this.statuses.addAll(0, statuses);
    notifyItemRangeInserted(0, statuses.size());
  }

  public void addNewStatusesAtLast(List<Status> statuses) {
    this.statuses.addAll(statuses);
    notifyDataSetChanged();
  }

  public void deleteStatus(long statusId) {
    Status removing = null;
    for (Status s: statuses){
      if (s.getId() == statusId) {
        removing = s;
        break;
      }
    }
    if (removing == null) {
      return;
    }
    synchronized (statuses) {
      int removedItemIndex = statuses.indexOf(removing);
      statuses.remove(removing);
      notifyItemRemoved(removedItemIndex);
      if (getSelectedTweetId() == statusId) {
        clearSelectedTweet();
      }
    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    private Status status;

    public ViewHolder(View itemView) {
      super(itemView);
    }

    void bindStatus(final Status status) {
      this.status = status;
      StatusView v = (StatusView) itemView;
      final User user = status.isRetweet()
          ? status.getRetweetedStatus().getUser()
          : status.getUser();
      v.setUserIconClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          userIconClickedListener.onClicked(user);
        }
      });
      v.bindStatus(status);
      Picasso.with(v.getContext())
          .load(user.getProfileImageURLHttps()).fit().into(v.getIcon());
      if (status.isRetweet()) {
        Picasso.with(v.getContext())
            .load(status.getUser().getMiniProfileImageURLHttps()).fit()
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
      return other != null && this.status.getId() == other.status.getId();
    }

    private OnItemViewClickListener itemViewClicked;
    private OnUserIconClickedListener userIconClickedListener;

    void onRecycled() {
      this.itemView.setOnClickListener(null);
      final StatusView v = (StatusView) this.itemView;
      v.setUserIconClickListener(null);
      v.reset();
      this.status = null;
      this.itemViewClicked = null;
      this.userIconClickedListener = null;
    }
  }

  private static class SelectedStatus {
    private final Status status;
    private final View view;

    private SelectedStatus(ViewHolder viewHolder) {
      this(viewHolder.status, viewHolder.itemView);
    }

    private SelectedStatus(Status status, View view) {
      this.status = status;
      this.view = view;
    }
  }

  interface OnUserIconClickedListener {
    void onClicked(User user);
  }

  private OnUserIconClickedListener userIconClickedListener;

  public void setOnUserIconClickedListener(OnUserIconClickedListener listener) {
    this.userIconClickedListener = listener;
  }

  interface OnItemViewClickListener {
    void onItemViewClicked(ViewHolder viewHolder);
  }
}
