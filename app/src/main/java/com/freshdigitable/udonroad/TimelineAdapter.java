package com.freshdigitable.udonroad;

import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import twitter4j.Status;
import twitter4j.User;
import twitter4j.util.TimeSpanConverter;

/**
 * Created by akihit on 15/10/18.
 */
public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {
  public TimelineAdapter(List<Status> statuses) {
    this.statuses = statuses;
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.tweet_view, parent, false);
    return new ViewHolder(view);
  }

  private List<Status> statuses;
  private static final TimeSpanConverter timeSpanConv = new TimeSpanConverter();

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    Status status = statuses.get(position);
    User user = status.getUser();
    Picasso.with(holder.icon.getContext())
        .load(user.getOriginalProfileImageURL()).into(holder.icon);
    holder.account.setText(user.getName());
    holder.screenName.setText(user.getScreenName());
    holder.tweet.setText(status.getText());
    holder.time.setText(timeSpanConv.toTimeSpanString(status.getCreatedAt()));
    holder.clientName.setText(Html.fromHtml(status.getSource()).toString());
    final int rtCount = status.getRetweetCount();
    final int favCount = status.getFavoriteCount();
    if (rtCount == 0 && favCount == 0) {
      return;
    }
    holder.rt.setVisibility(View.VISIBLE);
    holder.fav.setVisibility(View.VISIBLE);
    holder.rtCount.setText(String.valueOf(status.getRetweetCount()));
    holder.rtCount.setVisibility(View.VISIBLE);
    holder.favCount.setText(String.valueOf(status.getFavoriteCount()));
    holder.favCount.setVisibility(View.VISIBLE);
  }

  @Override
  public int getItemCount() {
    return this.statuses.size();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    ImageView icon;
    TextView screenName;
    TextView account;
    TextView tweet;
    TextView time;

    TextView rt;
    TextView rtCount;
    TextView fav;
    TextView favCount;
    TextView clientName;

    public ViewHolder(View itemView) {
      super(itemView);
      this.icon = (ImageView) itemView.findViewById(R.id.tl_icon);
      this.screenName = (TextView) itemView.findViewById(R.id.tl_displayname);
      this.account = (TextView) itemView.findViewById(R.id.tl_account);
      this.tweet = (TextView) itemView.findViewById(R.id.tl_tweet);
      this.time = (TextView) itemView.findViewById(R.id.tl_time);

      this.rt = (TextView) itemView.findViewById(R.id.tl_rt);
      this.rtCount = (TextView) itemView.findViewById(R.id.tl_rtcount);
      this.fav = (TextView) itemView.findViewById(R.id.tl_fav);
      this.favCount = (TextView) itemView.findViewById(R.id.tl_favcount);
      this.clientName = (TextView) itemView.findViewById(R.id.tl_clientname);
    }
  }
}
