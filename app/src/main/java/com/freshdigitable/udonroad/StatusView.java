package com.freshdigitable.udonroad;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import twitter4j.ExtendedMediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.User;

/**
 * Created by akihit on 2016/01/11.
 */
public class StatusView extends StatusViewBase {
  @SuppressWarnings("unused")
  private static final String TAG = StatusView.class.getSimpleName();
  protected TextView rtUser;
  protected ImageView rtUserIcon;
  protected LinearLayout rtUserContainer;
  private QuotedStatusView quotedStatus;

  public StatusView(Context context) {
    this(context, null);
  }

  public StatusView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public StatusView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    final View v = View.inflate(context, R.layout.view_status, this);
    createdAt = (TextView) v.findViewById(R.id.tl_create_at);
    icon = (ImageView) v.findViewById(R.id.tl_icon);
    names = (CombinedScreenNameTextView) v.findViewById(R.id.tl_names);
    tweet = (TextView) v.findViewById(R.id.tl_tweet);
    clientName = (TextView) v.findViewById(R.id.tl_via);
    rtIcon = (ImageView) v.findViewById(R.id.tl_rt_icon);
    rtCount = (TextView) v.findViewById(R.id.tl_rtcount);
    favIcon = (ImageView) v.findViewById(R.id.tl_fav_icon);
    favCount = (TextView) v.findViewById(R.id.tl_favcount);
    rtUserContainer = (LinearLayout) v.findViewById(R.id.tl_rt_user_container);
    rtUser = (TextView) v.findViewById(R.id.tl_rt_user);
    rtUserIcon = (ImageView) v.findViewById(R.id.tl_rt_user_icon);
    mediaGroup = v.findViewById(R.id.tl_image_group);
    mediaImages = new ImageView[]{
        (ImageView) v.findViewById(R.id.tl_image_1),
        (ImageView) v.findViewById(R.id.tl_image_2),
        (ImageView) v.findViewById(R.id.tl_image_3),
        (ImageView) v.findViewById(R.id.tl_image_4)
    };
    quotedStatus = (QuotedStatusView) v.findViewById(R.id.tl_quoted);
  }

  @Override
  public void bindStatus(final Status status) {
    super.bindStatus(status);
    if (status.isRetweet()) {
      bindRtUser(status.getUser());
    }

    final Status quotedBindingStatus = getBindingStatus(status).getQuotedStatus();
    if (quotedBindingStatus != null) {
      quotedStatus.bindStatus(quotedBindingStatus);
      quotedStatus.setVisibility(VISIBLE);
    }
  }

  private void bindRtUser(User user) {
    setRetweetedUserVisibility(VISIBLE);
    final String formattedRtUser = formatString(R.string.tweet_retweeting_user,
        user.getScreenName());
    rtUser.setText(formattedRtUser);
  }

  private void setRetweetedUserVisibility(int visibility) {
    rtUserContainer.setVisibility(visibility);
  }

  @Override
  public void reset() {
    super.reset();
    setRetweetedUserVisibility(GONE);
    quotedStatus.setBackgroundResource(R.drawable.s_quoted_frame);
    quotedStatus.setVisibility(GONE);
    quotedStatus.reset();
  }

  @Override
  protected String parseText(Status status) {
    final Status bindingStatus = getBindingStatus(status);
    String text = bindingStatus.getText();
    final String quotedStatusIdStr = Long.toString(bindingStatus.getQuotedStatusId());
    final URLEntity[] urlEntities = status.getURLEntities();
    for (URLEntity u : urlEntities) {
      if (bindingStatus.getQuotedStatus() != null
          && u.getExpandedURL().contains(quotedStatusIdStr)) {
        text = text.replace(u.getURL(), "");
      } else {
        text = text.replace(u.getURL(), u.getDisplayURL());
      }
    }
    final ExtendedMediaEntity[] extendedMediaEntities = status.getExtendedMediaEntities();
    for (ExtendedMediaEntity eme : extendedMediaEntities) {
      text = text.replace(eme.getURL(), "");
    }
    return text;
  }

  public ImageView getRtUserIcon() {
    return rtUserIcon;
  }

  public QuotedStatusView getQuotedStatusView() {
    return quotedStatus;
  }

  @Override
  public String toString() {
    final CharSequence text = tweet.getText();
    final CharSequence cs = text.length() > 10 ? text.subSequence(0, 9) : text;
    return "height: " + getHeight()
        + ", text: " + cs;
  }
}
