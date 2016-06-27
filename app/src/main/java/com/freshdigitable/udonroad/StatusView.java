package com.freshdigitable.udonroad;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Date;

import twitter4j.ExtendedMediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.util.TimeSpanConverter;

/**
 * Created by akihit on 2016/01/11.
 */
public class StatusView extends RelativeLayout {
  @SuppressWarnings("unused")
  private static final String TAG = StatusView.class.getSimpleName();
  private OnClickListener userIconClickListener;
  private TextView createdAt;
  private ImageView icon;
  private TextView names;
  private TextView tweet;
  private TextView clientName;
  private ImageView rtIcon;
  private TextView rtCount;
  private ImageView favIcon;
  private TextView favCount;
  private TextView rtUser;
  private ImageView rtUserIcon;
  private LinearLayout rtUserContainer;
  private Date createdAtDate;
  private View mediaGroup;
  private ImageView[] mediaImages;
  private final int grid;
  private QuotedStatusView quotedStatus;

  public StatusView(Context context) {
    this(context, null);
  }

  public StatusView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public StatusView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    grid = getResources().getDimensionPixelSize(R.dimen.grid_margin);
    setPadding(grid, grid, grid, grid);
    mediaHeight = getResources().getDimensionPixelOffset(R.dimen.tweet_user_icon);

    final View v = View.inflate(context, R.layout.view_status, this);
    createdAt = (TextView) v.findViewById(R.id.tl_create_at);
    icon = (ImageView) v.findViewById(R.id.tl_icon);
    names = (TextView) v.findViewById(R.id.tl_names);
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

  private static final TimeSpanConverter timeSpanConv = new TimeSpanConverter();

  public void bindStatus(final Status status) {
    final Status bindingStatus = getBindingStatus(status);

    createdAtDate = bindingStatus.getCreatedAt();
    updateCreatedAt(createdAtDate);
    if (status.isRetweet()) {
      setRetweetedUserVisibility(VISIBLE);
      final String formattedRtUser = formatString(R.string.tweet_retweeting_user,
          status.getUser().getScreenName());
      rtUser.setText(formattedRtUser);
      this.setTextColor(ContextCompat.getColor(getContext(), R.color.colorTwitterActionRetweeted));
    }
    final User user = bindingStatus.getUser();
    icon.setOnClickListener(userIconClickListener);

    final String formattedNames = formatString(R.string.tweet_name_screenName,
        user.getName(), user.getScreenName());
    names.setText(Html.fromHtml(formattedNames));

    tweet.setText(parseText(status));

    final String source = bindingStatus.getSource();
    if (source != null) {
      final String formattedVia = formatString(R.string.tweet_via,
          Html.fromHtml(source).toString());
      clientName.setText(formattedVia);
    } else {
      clientName.setText(formatString(R.string.tweet_via, "none provided"));
    }

    final int rtCount = bindingStatus.getRetweetCount();
    if (rtCount > 0) {
      this.setRtCountVisibility(VISIBLE);
      setTint(rtIcon, bindingStatus.isRetweeted()
          ? R.color.colorTwitterActionRetweeted
          : R.color.colorTwitterActionNormal);
      this.rtCount.setText(String.valueOf(rtCount));
    }

    final int favCount = bindingStatus.getFavoriteCount();
    if (favCount > 0) {
      this.setFavCountVisibility(VISIBLE);
      setTint(favIcon, bindingStatus.isFavorited()
          ? R.color.colorTwitterActionFaved
          : R.color.colorTwitterActionNormal);
      this.favCount.setText(String.valueOf(favCount));
    }

    final ExtendedMediaEntity[] extendedMediaEntities = status.getExtendedMediaEntities();
    final int mediaCount = extendedMediaEntities.length;
    if (mediaCount > 0) {
      mediaWidth = (mediaGroup.getWidth() - grid * (mediaCount - 1)) / mediaCount;
      mediaGroup.setVisibility(VISIBLE);
      for (int i = 0; i < mediaCount; i++) {
        mediaImages[i].setVisibility(VISIBLE);
      }
    }

    final Status quotedBindingStatus = bindingStatus.getQuotedStatus();
    if (quotedBindingStatus != null) {
      quotedStatus.bindStatus(quotedBindingStatus);
      quotedStatus.setVisibility(VISIBLE);
    }
  }

  private int mediaWidth;
  private final int mediaHeight;

  public int getMediaWidth() {
    return mediaWidth > 0
        ? mediaWidth
        : 0;
  }

  public int getMediaHeight() {
    return mediaHeight;
  }

  private Status getBindingStatus(Status status) {
    return status.isRetweet()
        ? status.getRetweetedStatus()
        : status;
  }

  private void updateCreatedAt(Date createdAtDate) {
    createdAt.setText(timeSpanConv.toTimeSpanString(createdAtDate));
  }

  public void updateTime() {
    updateCreatedAt(this.createdAtDate);
    if (quotedStatus.getVisibility() == VISIBLE) {
      quotedStatus.updateTime();
    }
  }

  public ImageView getIcon() {
    return icon;
  }

  public ImageView getRtUserIcon() {
    return rtUserIcon;
  }

  private void setTint(ImageView view, @ColorRes int color) {
//    Log.d(TAG, "setTint: " + color);
    DrawableCompat.setTint(view.getDrawable(), ContextCompat.getColor(getContext(), color));
  }

  private void setRtCountVisibility(int visibility) {
    rtIcon.setVisibility(visibility);
    rtCount.setVisibility(visibility);
  }

  private void setFavCountVisibility(int visibility) {
    favIcon.setVisibility(visibility);
    favCount.setVisibility(visibility);
  }

  private void setRetweetedUserVisibility(int visibility) {
    rtUserContainer.setVisibility(visibility);
  }

  private void setTextColor(int color) {
    names.setTextColor(color);
    createdAt.setTextColor(color);
    tweet.setTextColor(color);
  }

  public void reset() {
    setBackgroundColor(Color.TRANSPARENT);
    quotedStatus.setBackgroundResource(R.drawable.s_quoted_frame);
    setRtCountVisibility(GONE);
    setFavCountVisibility(GONE);
    setRetweetedUserVisibility(GONE);
    setTextColor(Color.GRAY);

    rtIcon.setVisibility(GONE);
    favIcon.setVisibility(GONE);

    setTint(rtIcon, R.color.colorTwitterActionNormal);
    setTint(favIcon, R.color.colorTwitterActionNormal);

    icon.setImageResource(android.R.color.transparent);
    icon.setOnClickListener(null);
    setOnClickListener(null);
    setUserIconClickListener(null);

    for (ImageView mi : mediaImages) {
      mi.setImageDrawable(null);
      final ViewGroup.LayoutParams layoutParams = mi.getLayoutParams();
      layoutParams.width = 0;
      mi.setLayoutParams(layoutParams);
      mi.setVisibility(GONE);
    }
    mediaGroup.setVisibility(GONE);
    quotedStatus.setVisibility(GONE);
    quotedStatus.reset();
  }

  public void setUserIconClickListener(OnClickListener userIconClickListener) {
    this.userIconClickListener = userIconClickListener;
  }

  private String formatString(@StringRes int id, Object... items) {
    final String format = getResources().getString(id);
    return String.format(format, items);
  }

  private String parseText(Status status) {
    String text = getBindingStatus(status).getText();
    final URLEntity[] urlEntities = status.getURLEntities();
    for (URLEntity u : urlEntities) {
      text = text.replace(u.getURL(), u.getDisplayURL());
    }
    final ExtendedMediaEntity[] extendedMediaEntities = status.getExtendedMediaEntities();
    for (ExtendedMediaEntity eme : extendedMediaEntities) {
      text = text.replace(eme.getURL(), "");
    }
    return text;
  }

  public ImageView[] getMediaImages() {
    return mediaImages;
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
