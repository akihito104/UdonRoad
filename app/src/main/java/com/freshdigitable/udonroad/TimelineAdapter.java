package com.freshdigitable.udonroad;

import android.graphics.Color;
import android.support.annotation.MainThread;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.LinkedList;
import java.util.List;

import twitter4j.Status;
import twitter4j.User;
import twitter4j.util.TimeSpanConverter;

/**
 * Created by akihit on 15/10/18.
 */
public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineAdapter.class.getSimpleName();

  public TimelineAdapter() {
    this(new LinkedList<Status>());
  }
  public TimelineAdapter(List<Status> statuses) {
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
          selectedTweetId = sv.getIncomingTweetId();
          if (selectedView != null) {
            selectedView.setBackgroundColor(Color.TRANSPARENT);
          }
          view.setBackgroundColor(Color.LTGRAY);
          selectedView = view;
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

  @Override
  public void onViewRecycled(ViewHolder holder) {
    holder.statusView.setBackgroundColor(Color.TRANSPARENT);
    holder.statusView.setRtCountVisibility(View.GONE);
    holder.statusView.setFavCountVisibility(View.GONE);
    holder.statusView.setRetweetedUserVisibility(View.GONE);
    holder.statusView.setTextColor(Color.GRAY);
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

  public void clearSelectedTweet() {
    selectedTweetId = -1;
    if (selectedView != null) {
      selectedView.setBackgroundColor(Color.TRANSPARENT);
    }
    selectedView = null;
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
        selectedTweetId = -1;
        selectedView = null;
      }
    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    private StatusView statusView;

    public ViewHolder(View itemView) {
      super(itemView);
      this.statusView = (StatusView) itemView;
    }
  }
}
