package com.freshdigitable.udonroad;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.LinkedList;
import java.util.List;

import twitter4j.Status;

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
  private long selectedTweetId = -1;

  public long getSelectedTweetId() {
    return selectedTweetId;
  }

  public boolean isTweetSelected() {
    return selectedTweetId > 0;
  }

  private View selectedView = null;

  @Override
  public void onBindViewHolder(final ViewHolder holder, int position) {
    Status status = statuses.get(position);
    holder.statusView.setStatus(status);

    holder.statusView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (!(view instanceof StatusView)) {
          return;
        }
        StatusView sv = (StatusView) view;
        if (selectedTweetId != sv.getIncomingTweetId()) {
          fixSelectedTweet(sv);
        } else {
          clearSelectedTweet();
        }
      }
    });
    if (status.getId() == selectedTweetId) {
      holder.statusView.setBackgroundColor(Color.LTGRAY);
      selectedView = holder.statusView;
    }
  }

  private void fixSelectedTweet(StatusView sv) {
    selectedTweetId = sv.getIncomingTweetId();
    if (selectedView != null) {
      selectedView.setBackgroundColor(Color.TRANSPARENT);
    }
    selectedView = sv;
    selectedView.setBackgroundColor(Color.LTGRAY);
    if (selectedTweetChangeListener != null) {
      selectedTweetChangeListener.onTweetSelected();
    }
  }

  public void clearSelectedTweet() {
    selectedTweetId = -1;
    if (selectedView != null) {
      selectedView.setBackgroundColor(Color.TRANSPARENT);
    }
    selectedView = null;
    if (selectedTweetChangeListener != null) {
      selectedTweetChangeListener.onTweetUnselected();
    }
  }

  @Override
  public void onViewRecycled(ViewHolder holder) {
    holder.statusView.onRecycled();
  }

  private OnSelectedTweetChangeListener selectedTweetChangeListener;

  public void setOnSelectedTweetChangeListener(OnSelectedTweetChangeListener listener) {
    this.selectedTweetChangeListener = listener;
  }

  interface OnSelectedTweetChangeListener {
    void onTweetSelected();
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
      if (selectedTweetId == statusId) {
        clearSelectedTweet();
      }
    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    private final StatusView statusView;

    public ViewHolder(View itemView) {
      super(itemView);
      this.statusView = (StatusView) itemView;
    }
  }
}
