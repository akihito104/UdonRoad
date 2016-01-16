package com.freshdigitable.udonroad;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import twitter4j.Status;
import twitter4j.User;
import twitter4j.util.TimeSpanConverter;

/**
 * Created by akihit on 2016/01/11.
 */
public class StatusView extends RelativeLayout {
  private long incomingTweetId;
  private final ImageView icon;
  private final TextView screenName;
  private final TextView at;
  private final TextView account;
  private final TextView tweet;
  private final TextView time;

  private final TextView rt;
  private final TextView rtCount;
  private final TextView fav;
  private final TextView favCount;
  private final TextView clientName;

  private final TextView rtby;
  private final TextView retweetedUser;

  public StatusView(Context context) {
    this(context, null);
  }

  public StatusView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public StatusView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    LayoutInflater.from(context).inflate(R.layout.tweet_view, this, true);
    this.icon = (ImageView) findViewById(R.id.tl_icon);
    this.screenName = (TextView) findViewById(R.id.tl_displayname);
    this.at = (TextView) findViewById(R.id.tl_at);
    this.account = (TextView) findViewById(R.id.tl_account);
    this.tweet = (TextView) findViewById(R.id.tl_tweet);
    this.time = (TextView) findViewById(R.id.tl_time);

    this.rt = (TextView) findViewById(R.id.tl_rt);
    this.rtCount = (TextView) findViewById(R.id.tl_rtcount);
    this.fav = (TextView) findViewById(R.id.tl_fav);
    this.favCount = (TextView) findViewById(R.id.tl_favcount);
    this.clientName = (TextView) findViewById(R.id.tl_clientname);

    this.rtby = (TextView) findViewById(R.id.tl_rtby);
    this.retweetedUser = (TextView) findViewById(R.id.tl_rtuser);
  }

  private static final int RETWEETED_TEXT_COLOR = Color.argb(255, 64, 192, 64);
  private static final TimeSpanConverter timeSpanConv = new TimeSpanConverter();

  public void setStatus(final Status status) {
    incomingTweetId = status.getId();
    if (status.isRetweet()) {
      setRetweetedUserVisibility(View.VISIBLE);
      this.retweetedUser.setText(status.getUser().getScreenName());
      this.setTextColor(RETWEETED_TEXT_COLOR);
    }
    Status bindingStatus;
    if (status.isRetweet()) {
      bindingStatus = status.getRetweetedStatus();
    } else {
      bindingStatus = status;
    }
    User user = bindingStatus.getUser();
    Picasso.with(this.icon.getContext())
        .load(user.getProfileImageURLHttps()).fit().into(this.icon);

    this.account.setText(user.getName());
    this.screenName.setText(user.getScreenName());
    this.tweet.setText(bindingStatus.getText());
    this.time.setText(timeSpanConv.toTimeSpanString(bindingStatus.getCreatedAt()));
    this.clientName.setText(Html.fromHtml(bindingStatus.getSource()).toString());

    final int rtCount = bindingStatus.getRetweetCount();
    if (rtCount > 0) {
      this.setRtCountVisibility(View.VISIBLE);
      this.rtCount.setText(String.valueOf(rtCount));
    }

    final int favCount = bindingStatus.getFavoriteCount();
    if (favCount > 0) {
      this.setFavCountVisibility(View.VISIBLE);
      this.favCount.setText(String.valueOf(favCount));
    }
  }

  public long getIncomingTweetId() {
    return this.incomingTweetId;
  }

  private void setRtCountVisibility(int visibility) {
    this.rt.setVisibility(visibility);
    this.rtCount.setVisibility(visibility);
  }

  private void setFavCountVisibility(int visibility) {
    this.fav.setVisibility(visibility);
    this.favCount.setVisibility(visibility);
  }

  private void setRetweetedUserVisibility(int visibility) {
    this.rtby.setVisibility(visibility);
    this.retweetedUser.setVisibility(visibility);
  }

  private void setTextColor(int color) {
    this.screenName.setTextColor(color);
    this.at.setTextColor(color);
    this.account.setTextColor(color);
    this.time.setTextColor(color);
    this.tweet.setTextColor(color);
  }

  public void onRecycled() {
    setBackgroundColor(Color.TRANSPARENT);
    setRtCountVisibility(View.GONE);
    setFavCountVisibility(View.GONE);
    setRetweetedUserVisibility(View.GONE);
    setTextColor(Color.GRAY);
  }
}
