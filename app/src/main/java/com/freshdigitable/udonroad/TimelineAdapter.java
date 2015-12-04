package com.freshdigitable.udonroad;

import android.graphics.Color;
import android.support.annotation.MainThread;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
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
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.tweet_view, parent, false);
    return new ViewHolder(view);
  }

  private List<Status> statuses;
  private long selectedTweetId = -1;

  public long getSelectedTweetId() {
    return selectedTweetId;
  }

  private View selectedView = null;
  private static final TimeSpanConverter timeSpanConv = new TimeSpanConverter();

  @Override
  public void onBindViewHolder(final ViewHolder holder, int position) {
    Status status = statuses.get(position);
    final long incomingTweetId = status.getId();
    if (status.isRetweet()) {
      holder.setRetweetedUserVisibility(View.VISIBLE);
      holder.retweetedUser.setText(status.getUser().getScreenName());
      status = status.getRetweetedStatus();
      holder.setTextColor(Color.argb(255, 64, 192, 64));
    }
    User user = status.getUser();
    Picasso.with(holder.icon.getContext())
        .load(user.getProfileImageURLHttps()).into(holder.icon);

    holder.view.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (selectedTweetId != incomingTweetId) {
          selectedTweetId = incomingTweetId;
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
    holder.view.setBackgroundColor(Color.TRANSPARENT);
    if (status.getId() == selectedTweetId) {
      holder.view.setBackgroundColor(Color.LTGRAY);
      selectedView = holder.view;
    }
    holder.tweetId = status.getId();
    holder.account.setText(user.getName());
    holder.screenName.setText(user.getScreenName());
    holder.tweet.setText(status.getText());
    holder.time.setText(timeSpanConv.toTimeSpanString(status.getCreatedAt()));
    holder.clientName.setText(Html.fromHtml(status.getSource()).toString());

    final int rtCount = status.getRetweetCount();
    if (rtCount > 0) {
      holder.setRtCountVisibility(View.VISIBLE);
      holder.rtCount.setText(String.valueOf(rtCount));
    }

    final int favCount = status.getFavoriteCount();
    if (favCount > 0) {
      holder.setFavCountVisibility(View.VISIBLE);
      holder.favCount.setText(String.valueOf(favCount));
    }
  }

  @Override
  public void onViewRecycled(ViewHolder holder) {
    holder.setRtCountVisibility(View.GONE);
    holder.setFavCountVisibility(View.GONE);
    holder.setRetweetedUserVisibility(View.GONE);
    holder.setTextColor(Color.GRAY);
  }

  @Override
  public int getItemCount() {
    return this.statuses.size();
  }

  @MainThread
  public void addNewStatus(Status status) {
    statuses.add(0, status);
//    notifyItemInserted(0);
    notifyDataSetChanged();
  }

  @MainThread
  public void addNewStatuses(List<Status> statuses) {
    this.statuses.addAll(0, statuses);
    notifyDataSetChanged();
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
      statuses.remove(removing);
    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    long tweetId;
    View view;
    ImageView icon;
    TextView screenName;
    TextView at;
    TextView account;
    TextView tweet;
    TextView time;

    TextView rt;
    TextView rtCount;
    TextView fav;
    TextView favCount;
    TextView clientName;

    TextView rtby;
    TextView retweetedUser;

    public ViewHolder(View itemView) {
      super(itemView);
      this.view = itemView;
      this.icon = (ImageView) itemView.findViewById(R.id.tl_icon);
      this.screenName = (TextView) itemView.findViewById(R.id.tl_displayname);
      this.at = (TextView) itemView.findViewById(R.id.tl_at);
      this.account = (TextView) itemView.findViewById(R.id.tl_account);
      this.tweet = (TextView) itemView.findViewById(R.id.tl_tweet);
      this.time = (TextView) itemView.findViewById(R.id.tl_time);

      this.rt = (TextView) itemView.findViewById(R.id.tl_rt);
      this.rtCount = (TextView) itemView.findViewById(R.id.tl_rtcount);
      this.fav = (TextView) itemView.findViewById(R.id.tl_fav);
      this.favCount = (TextView) itemView.findViewById(R.id.tl_favcount);
      this.clientName = (TextView) itemView.findViewById(R.id.tl_clientname);

      this.rtby = (TextView) itemView.findViewById(R.id.tl_rtby);
      this.retweetedUser = (TextView) itemView.findViewById(R.id.tl_rtuser);
    }

    public void setRtCountVisibility(int visibility) {
      this.rt.setVisibility(visibility);
      this.rtCount.setVisibility(visibility);
    }

    public void setFavCountVisibility(int visibility) {
      this.fav.setVisibility(visibility);
      this.favCount.setVisibility(visibility);
    }

    public void setRetweetedUserVisibility(int visibility) {
      this.rtby.setVisibility(visibility);
      this.retweetedUser.setVisibility(visibility);
    }

    public void setTextColor(int color) {
      this.screenName.setTextColor(color);
      this.at.setTextColor(color);
      this.account.setTextColor(color);
      this.time.setTextColor(color);
      this.tweet.setTextColor(color);
    }
  }
}
