package com.freshdigitable.udonroad;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.freshdigitable.udonroad.databinding.TweetViewBinding;
import com.squareup.picasso.Picasso;

import twitter4j.Status;
import twitter4j.User;
import twitter4j.util.TimeSpanConverter;

/**
 * Created by akihit on 2016/01/11.
 */
public class StatusView extends RelativeLayout {
  private static final String TAG = StatusView.class.getSimpleName();
  private final TweetViewBinding binding;
  private OnClickListener userIconClickListener;

  public StatusView(Context context) {
    this(context, null);
  }

  public StatusView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public StatusView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.tweet_view, this, true);
  }

  private static final TimeSpanConverter timeSpanConv = new TimeSpanConverter();

  public void bindStatus(final Status status) {
    final Status bindingStatus;
    if (status.isRetweet()) {
      bindingStatus = status.getRetweetedStatus();
    } else {
      bindingStatus = status;
    }
    binding.tlTime.setText(timeSpanConv.toTimeSpanString(bindingStatus.getCreatedAt()));
    if (status.isRetweet()) {
      setRetweetedUserVisibility(VISIBLE);
      binding.tlRtuser.setText(status.getUser().getScreenName());
      this.setTextColor(ContextCompat.getColor(getContext(), R.color.colorTwitterActionRetweeted));
    }
    final User user = bindingStatus.getUser();
    Picasso.with(binding.tlIcon.getContext())
        .load(user.getProfileImageURLHttps()).fit().into(binding.tlIcon);
    binding.tlIcon.setOnClickListener(userIconClickListener);

    binding.tlAccount.setText(user.getName());
    binding.tlDisplayname.setText(user.getScreenName());
    binding.tlTweet.setText(bindingStatus.getText());
    binding.tlClientname.setText(Html.fromHtml(bindingStatus.getSource()).toString());

    final int rtCount = bindingStatus.getRetweetCount();
    if (rtCount > 0) {
      this.setRtCountVisibility(VISIBLE);
      setTint(binding.tlRtIcon, bindingStatus.isRetweetedByMe() ?
          R.color.colorTwitterActionRetweeted
          : R.color.colorTwitterActionNormal);
      binding.tlRtcount.setText(String.valueOf(rtCount));
    }

    final int favCount = bindingStatus.getFavoriteCount();
    if (favCount > 0) {
      this.setFavCountVisibility(VISIBLE);
      setTint(binding.tlFavIcon, bindingStatus.isFavorited() ?
          R.color.colorTwitterActionFaved
          : R.color.colorTwitterActionNormal);
      binding.tlFavcount.setText(String.valueOf(favCount));
    }
  }

  private void setTint(ImageView view, @ColorRes int color) {
//    Log.d(TAG, "setTint: " + color);
    DrawableCompat.setTint(view.getDrawable(), ContextCompat.getColor(getContext(), color));
  }

  private void setRtCountVisibility(int visibility) {
    binding.tlRtIcon.setVisibility(visibility);
    binding.tlRtcount.setVisibility(visibility);
  }

  private void setFavCountVisibility(int visibility) {
    binding.tlFavIcon.setVisibility(visibility);
    binding.tlFavcount.setVisibility(visibility);
  }

  private void setRetweetedUserVisibility(int visibility) {
    binding.tlRtby.setVisibility(visibility);
    binding.tlRtuser.setVisibility(visibility);
  }

  private void setTextColor(int color) {
    binding.tlDisplayname.setTextColor(color);
    binding.tlAt.setTextColor(color);
    binding.tlAccount.setTextColor(color);
    binding.tlTime.setTextColor(color);
    binding.tlTweet.setTextColor(color);
  }

  public void recycle() {
    setBackgroundColor(Color.TRANSPARENT);
    setRtCountVisibility(GONE);
    setFavCountVisibility(GONE);
    setRetweetedUserVisibility(GONE);
    setTextColor(Color.GRAY);

    binding.tlRtIcon.setVisibility(GONE);
    binding.tlFavIcon.setVisibility(GONE);

    setTint(binding.tlRtIcon, R.color.colorTwitterActionNormal);
    setTint(binding.tlFavIcon, R.color.colorTwitterActionNormal);

    binding.tlIcon.setOnClickListener(null);
    setOnClickListener(null);
    setUserIconClickListener(null);
  }

  public void setUserIconClickListener(OnClickListener userIconClickListener) {
    this.userIconClickListener = userIconClickListener;
  }
}
