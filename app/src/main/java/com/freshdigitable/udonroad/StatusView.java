package com.freshdigitable.udonroad;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import twitter4j.Status;
import twitter4j.User;
import twitter4j.util.TimeSpanConverter;

/**
 * Created by akihit on 2016/01/11.
 */
public class StatusView extends RelativeLayout {
  @SuppressWarnings("unused")
  private static final String TAG = StatusView.class.getSimpleName();
  private OnClickListener userIconClickListener;
  private TextView tlTime;
  private TextView tlRtuser;
  private ImageView tlIcon;
  private TextView tlAccount;
  private TextView tlDisplayname;
  private TextView tlTweet;
  private TextView tlClientname;
  private ImageView tlRtIcon;
  private TextView tlRtcount;
  private ImageView tlFavIcon;
  private TextView tlFavcount;
  private TextView tlAt;
  private TextView tlRtby;

  public StatusView(Context context) {
    this(context, null);
  }

  public StatusView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public StatusView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    int padding = (int) (4 * getResources().getDisplayMetrics().density + 0.5f);
    setPadding(padding, padding, padding, padding);

    final View v = View.inflate(context, R.layout.view_status, this);
    tlTime = (TextView) v.findViewById(R.id.tl_time);
    tlRtuser = (TextView) v.findViewById(R.id.tl_rtuser);
    tlIcon = (ImageView) v.findViewById(R.id.tl_icon);
    tlAccount = (TextView) v.findViewById(R.id.tl_account);
    tlDisplayname = (TextView) v.findViewById(R.id.tl_displayname);
    tlTweet = (TextView) v.findViewById(R.id.tl_tweet);
    tlClientname = (TextView) v.findViewById(R.id.tl_clientname);
    tlRtIcon = (ImageView) v.findViewById(R.id.tl_rt_icon);
    tlRtcount = (TextView) v.findViewById(R.id.tl_rtcount);
    tlFavIcon = (ImageView) v.findViewById(R.id.tl_fav_icon);
    tlFavcount = (TextView) v.findViewById(R.id.tl_favcount);
    tlAt = (TextView) v.findViewById(R.id.tl_at);
    tlRtby = (TextView) v.findViewById(R.id.tl_rtby);
    reset();
  }

  private static final TimeSpanConverter timeSpanConv = new TimeSpanConverter();

  public void bindStatus(final Status status) {
    final Status bindingStatus;
    if (status.isRetweet()) {
      bindingStatus = status.getRetweetedStatus();
    } else {
      bindingStatus = status;
    }
    tlTime.setText(timeSpanConv.toTimeSpanString(bindingStatus.getCreatedAt()));
    if (status.isRetweet()) {
      setRetweetedUserVisibility(VISIBLE);
      tlRtuser.setText(status.getUser().getScreenName());
      this.setTextColor(ContextCompat.getColor(getContext(), R.color.colorTwitterActionRetweeted));
    }
    final User user = bindingStatus.getUser();
    tlIcon.setOnClickListener(userIconClickListener);

    tlAccount.setText(user.getName());
    tlDisplayname.setText(user.getScreenName());
    tlTweet.setText(bindingStatus.getText());
    tlClientname.setText(Html.fromHtml(bindingStatus.getSource()).toString());

    final int rtCount = bindingStatus.getRetweetCount();
    if (rtCount > 0) {
      this.setRtCountVisibility(VISIBLE);
      setTint(tlRtIcon, bindingStatus.isRetweetedByMe() ?
          R.color.colorTwitterActionRetweeted
          : R.color.colorTwitterActionNormal);
      tlRtcount.setText(String.valueOf(rtCount));
    }

    final int favCount = bindingStatus.getFavoriteCount();
    if (favCount > 0) {
      this.setFavCountVisibility(VISIBLE);
      setTint(tlFavIcon, bindingStatus.isFavorited() ?
          R.color.colorTwitterActionFaved
          : R.color.colorTwitterActionNormal);
      tlFavcount.setText(String.valueOf(favCount));
    }
  }

  public ImageView getIcon() {
    return tlIcon;
  }

  private void setTint(ImageView view, @ColorRes int color) {
//    Log.d(TAG, "setTint: " + color);
    DrawableCompat.setTint(view.getDrawable(), ContextCompat.getColor(getContext(), color));
  }

  private void setRtCountVisibility(int visibility) {
    tlRtIcon.setVisibility(visibility);
    tlRtcount.setVisibility(visibility);
  }

  private void setFavCountVisibility(int visibility) {
    tlFavIcon.setVisibility(visibility);
    tlFavcount.setVisibility(visibility);
  }

  private void setRetweetedUserVisibility(int visibility) {
    tlRtby.setVisibility(visibility);
    tlRtuser.setVisibility(visibility);
  }

  private void setTextColor(int color) {
    tlDisplayname.setTextColor(color);
    tlAt.setTextColor(color);
    tlAccount.setTextColor(color);
    tlTime.setTextColor(color);
    tlTweet.setTextColor(color);
  }

  public void reset() {
    setBackgroundColor(Color.TRANSPARENT);
    setRtCountVisibility(GONE);
    setFavCountVisibility(GONE);
    setRetweetedUserVisibility(GONE);
    setTextColor(Color.GRAY);

    tlRtIcon.setVisibility(GONE);
    tlFavIcon.setVisibility(GONE);

    setTint(tlRtIcon, R.color.colorTwitterActionNormal);
    setTint(tlFavIcon, R.color.colorTwitterActionNormal);

    tlIcon.setOnClickListener(null);
    setOnClickListener(null);
    setUserIconClickListener(null);
  }

  public void setUserIconClickListener(OnClickListener userIconClickListener) {
    this.userIconClickListener = userIconClickListener;
  }

  @Override
  public String toString() {
    final CharSequence text = tlTweet.getText();
    final CharSequence cs = text.length() > 10 ? text.subSequence(0, 9) : text;
    return "height: " + getHeight()
        + ", user: " + tlDisplayname.getText()
        + ", text: " + cs;
  }
}
