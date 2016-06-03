/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import twitter4j.User;

/**
 * User information ViewGroup
 *
 * Created by akihit on 2016/06/03.
 */
public class UserInfoView extends RelativeLayout {
  private ImageView banner;
  private TextView name;
  private TextView screenName;
  private ImageView icon;
  private TextView description;
  private TextView tweetCount;
  private TextView followerCount;
  private TextView friendsCount;

  public UserInfoView(Context context) {
    this(context, null);
  }

  public UserInfoView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public UserInfoView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public UserInfoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init(context);
  }

  private void init(Context context) {
    final View v = View.inflate(context, R.layout.view_user_info, this);
    name = (TextView) v.findViewById(R.id.user_name);
    screenName = (TextView) v.findViewById(R.id.user_screen_name);
    description = (TextView) v.findViewById(R.id.user_description);
    banner = (ImageView) v.findViewById(R.id.user_banner);
    icon = (ImageView) v.findViewById(R.id.user_icon);
    tweetCount = (TextView) v.findViewById(R.id.user_tweets_count);
    followerCount = (TextView) v.findViewById(R.id.user_follower_count);
    friendsCount = (TextView) v.findViewById(R.id.user_friends_count);
  }

  public void bindData(User user) {
    name.setText(user.getName());
    screenName.setText("@" + user.getScreenName());
    description.setText(user.getDescription());
    Picasso.with(getContext()).load(user.getProfileBannerMobileURL()).fit().into(banner);
    Picasso.with(getContext()).load(user.getProfileImageURLHttps()).into(icon);
    tweetCount.setText("* " + user.getStatusesCount());
    followerCount.setText("< " + user.getFollowersCount());
    friendsCount.setText("> " + user.getFriendsCount());
  }
}
