package com.freshdigitable.udonroad;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
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
  private final TweetViewBinding binding;

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

  private static final int RETWEETED_TEXT_COLOR = Color.argb(255, 64, 192, 64);
  private static final TimeSpanConverter timeSpanConv = new TimeSpanConverter();

  public void bindStatus(final Status status) {
    Status bindingStatus;
    if (status.isRetweet()) {
      bindingStatus = status.getRetweetedStatus();
    } else {
      bindingStatus = status;
    }
    binding.tlTime.setText(timeSpanConv.toTimeSpanString(bindingStatus.getCreatedAt()));
    if (status.isRetweet()) {
      setRetweetedUserVisibility(View.VISIBLE);
      binding.tlRtuser.setText(status.getUser().getScreenName());
      this.setTextColor(RETWEETED_TEXT_COLOR);
    }
    final User user = bindingStatus.getUser();
    Picasso.with(binding.tlIcon.getContext())
        .load(user.getProfileImageURLHttps()).fit().into(binding.tlIcon);
    binding.tlIcon.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        getContext().startActivity(UserAccountActivity.createIntent(getContext(), user));
      }
    });

    binding.tlAccount.setText(user.getName());
    binding.tlDisplayname.setText(user.getScreenName());
    binding.tlTweet.setText(bindingStatus.getText());
    binding.tlClientname.setText(Html.fromHtml(bindingStatus.getSource()).toString());

    final int rtCount = bindingStatus.getRetweetCount();
    if (rtCount > 0) {
      this.setRtCountVisibility(View.VISIBLE);
      if (bindingStatus.isRetweetedByMe()) {
        binding.tlMyrt.setVisibility(VISIBLE);
        binding.tlRtcount.setText(String.valueOf(rtCount - 1));
      } else {
        binding.tlRtcount.setText(String.valueOf(rtCount));
      }
    }

    final int favCount = bindingStatus.getFavoriteCount();
    if (favCount > 0) {
      this.setFavCountVisibility(View.VISIBLE);
      if (bindingStatus.isFavorited()) {
        binding.tlMyfav.setVisibility(VISIBLE);
        binding.tlFavcount.setText(String.valueOf(favCount - 1));
      } else {
        binding.tlFavcount.setText(String.valueOf(favCount));
      }
    }
  }

  private void setRtCountVisibility(int visibility) {
    binding.tlRt.setVisibility(visibility);
    binding.tlRtcount.setVisibility(visibility);
  }

  private void setFavCountVisibility(int visibility) {
    binding.tlFav.setVisibility(visibility);
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

  public void onRecycled() {
    setBackgroundColor(Color.TRANSPARENT);
    setRtCountVisibility(View.GONE);
    setFavCountVisibility(View.GONE);
    setRetweetedUserVisibility(View.GONE);
    setTextColor(Color.GRAY);
    binding.tlMyrt.setVisibility(GONE);
    binding.tlMyfav.setVisibility(GONE);
    binding.tlIcon.setOnClickListener(null);
    setOnClickListener(null);
  }
}
