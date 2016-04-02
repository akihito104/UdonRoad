package com.freshdigitable.udonroad;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.LinkedList;
import java.util.List;

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

  interface LastItemBoundListener {
    void onLastItemBound(long statusId);
  }

  private LastItemBoundListener lastItemBoundListener;

  public void setLastItemBoundListener(LastItemBoundListener listener) {
    this.lastItemBoundListener = listener;
  }

  @Override
  public void onBindViewHolder(final ViewHolder holder, int position) {
    Status status = statuses.get(position);
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
      holder.itemView.setBackgroundColor(Color.LTGRAY);
      selectedStatusHolder = new SelectedStatus(holder);
    }
  }

  private SelectedStatus selectedStatusHolder = null;

  private void fixSelectedTweet(ViewHolder vh) {
    if (isStatusViewSelected()) {
      selectedStatusHolder.view.setBackgroundColor(Color.TRANSPARENT);
    }
    selectedStatusHolder = new SelectedStatus(vh);
    selectedStatusHolder.view.setBackgroundColor(Color.LTGRAY);
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
//    Log.d(TAG, "onViewRecycled: ");
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
      v.setUserIconClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          userIconClickedListener.onClicked(
              status.isRetweet() ?
                  status.getRetweetedStatus().getUser() : status.getUser());
          itemViewClicked.onItemViewClicked(ViewHolder.this);
        }
      });
      v.bindStatus(status);
      itemView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          itemViewClicked.onItemViewClicked(ViewHolder.this);
        }
      });
    }

    boolean hasSameStatusId(SelectedStatus other) {
      return other != null && this.status.getId() == other.status.getId();
    }

    private OnItemViewClickListener itemViewClicked;
    private OnUserIconClickedListener userIconClickedListener;

    void onRecycled() {
      ((StatusView)itemView).recycle();
      this.status = null;
      this.itemViewClicked = null;
    }
  }

  private static class SelectedStatus {
    private final Status status;
    private final View view;

    private SelectedStatus(ViewHolder viewHolder) {
      this.status = viewHolder.status;
      this.view = viewHolder.itemView;
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
