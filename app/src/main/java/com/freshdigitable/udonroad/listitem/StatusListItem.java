/*
 * Copyright (c) 2017. Matsuda, Akihit (akihito104)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freshdigitable.udonroad.listitem;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;

import com.freshdigitable.udonroad.CombinedScreenNameTextView;
import com.freshdigitable.udonroad.OnSpanClickListener.SpanItem;
import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;
import twitter4j.util.TimeSpanConverter;

/**
 * Created by akihit on 2017/06/15.
 */

public class StatusListItem implements TwitterListItem {
  private static final TimeSpanConverter timeSpanConv = new TimeSpanConverter();
  private final Status bindingStatus;
  private final boolean isRetweet;
  private final long id;
  private final TextType textType;
  private final User rtUser;
  private final TimeTextType timeTextType;

  public StatusListItem(Status item) {
    this(item, TextType.DEFAULT, TimeTextType.RELATIVE);
  }

  private StatusListItem(Status item, TextType textType) {
    this(item, textType, TimeTextType.RELATIVE);
  }

  public StatusListItem(Status item, TextType textType, TimeTextType timeTextType) {
    this.isRetweet = item.isRetweet();
    this.id = item.getId();
    this.bindingStatus = Utils.getBindingStatus(item);
    this.textType = textType;
    this.rtUser = item.getUser();
    this.timeTextType = timeTextType;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public CharSequence getText() {
    return textType.getText(bindingStatus);
  }

  public enum TextType {
    DEFAULT {
      @Override
      CharSequence getText(Status bindingStatus) {
        String text = bindingStatus.getText();
        final String quotedStatusIdStr = Long.toString(bindingStatus.getQuotedStatusId());
        final URLEntity[] urlEntities = bindingStatus.getURLEntities();
        for (URLEntity u : urlEntities) {
          if (bindingStatus.getQuotedStatus() != null
              && u.getExpandedURL().contains(quotedStatusIdStr)) {
            text = text.replace(u.getURL(), "");
          } else {
            text = text.replace(u.getURL(), u.getDisplayURL());
          }
        }
        return removeMediaUrl(text, bindingStatus.getMediaEntities());
      }
    }, QUOTED {
      @Override
      CharSequence getText(Status status) {
        String text = status.getText();
        final URLEntity[] urlEntities = status.getURLEntities();
        for (URLEntity u : urlEntities) {
          text = text.replace(u.getURL(), u.getDisplayURL());
        }
        return removeMediaUrl(text, status.getMediaEntities());
      }
    }, DETAIL {
      @Override
      CharSequence getText(Status bindingStatus) {
        return DEFAULT.getText(bindingStatus);
      }
    };

    abstract CharSequence getText(Status bindingStatus);

    String removeMediaUrl(String text, MediaEntity[] mediaEntities) {
      for (MediaEntity me : mediaEntities) {
        text = text.replace(me.getURL(), "");
      }
      return text;
    }
  }

  public List<SpanItem> createSpanItems() {
    final String text = textType.getText(bindingStatus).toString();
    final ArrayList<SpanItem> res = new ArrayList<>();

    final URLEntity[] urlEntities = bindingStatus.getURLEntities();
    for (URLEntity urlEntity : urlEntities) {
      final int start = text.indexOf(urlEntity.getDisplayURL());
      if (start < 0) {
        continue;
      }
      final int end = start + urlEntity.getDisplayURL().length();
      res.add(new SpanItem(SpanItem.TYPE_URL, start, end, urlEntity.getExpandedURL()));
    }

    final UserMentionEntity[] userMentionEntities = bindingStatus.getUserMentionEntities();
    for (UserMentionEntity mention : userMentionEntities) {
      final Matcher matcher = Pattern.compile("[@＠]" + mention.getScreenName()).matcher(text);
      while (matcher.find()) {
        res.add(new SpanItem(SpanItem.TYPE_MENTION, matcher.start(), matcher.end(), mention.getId()));
      }
    }

    final HashtagEntity[] hashtagEntities = bindingStatus.getHashtagEntities();
    for (HashtagEntity hashtagEntity : hashtagEntities) {
      final String hashtag = "#" + hashtagEntity.getText();
      final Matcher matcher = Pattern.compile("[#＃]" + hashtagEntity.getText()).matcher(text);
      while (matcher.find()) {
        res.add(new SpanItem(SpanItem.TYPE_HASHTAG, matcher.start(), matcher.end(), hashtag));
      }
    }
    return res;
  }

  @Override
  public List<Stat> getStats() {
    return Arrays.asList(
        StatusStats.RETWEET.getStat(bindingStatus),
        StatusStats.FAV.getStat(bindingStatus),
        StatusStats.HAS_REPLY.getStat(bindingStatus));
  }

  enum StatusStats {
    RETWEET(TwitterReactionContainer.ReactionIcon.RETWEET){
      @Override
      Stat getStat(Status status) {
        final int retweetCount = status.getRetweetCount();
        final boolean retweeted = status.isRetweeted();
        return new Stat() {
          @Override
          public int getType() {
            return icon.type;
          }

          @Override
          public int getCount() {
            return retweetCount;
          }

          @Override
          public boolean isMarked() {
            return retweeted;
          }
        };
      }
    }, FAV(TwitterReactionContainer.ReactionIcon.FAV) {
      @Override
      Stat getStat(Status status) {
        final int favoriteCount = status.getFavoriteCount();
        final boolean favorited = status.isFavorited();
        return new Stat() {
          @Override
          public int getType() {
            return icon.type;
          }

          @Override
          public int getCount() {
            return favoriteCount;
          }

          @Override
          public boolean isMarked() {
            return favorited;
          }
        };
      }
    }, HAS_REPLY(TwitterReactionContainer.ReactionIcon.IN_REPLY_TO) {
      @Override
      Stat getStat(Status status) {
        final boolean marked = status.getInReplyToStatusId() > 0;
        return new Stat() {
          @Override
          public int getType() {
            return icon.type;
          }

          @Override
          public int getCount() {
            return -1;
          }

          @Override
          public boolean isMarked() {
            return marked;
          }
        };
      }
    };

    final TwitterReactionContainer.ReactionIcon icon;

    StatusStats(TwitterReactionContainer.ReactionIcon icon) {
      this.icon = icon;
    }

    abstract Stat getStat(Status status);
  }

  @Override
  public String getCreatedTime(final Context context) {
    return timeTextType.getStrategy(bindingStatus).getCreatedTime(context);
  }

  @Override
  public TimeTextStrategy getTimeStrategy() {
    return timeTextType.getStrategy(bindingStatus);
  }

  public enum TimeTextType {
    RELATIVE {
      @Override
      public TimeTextStrategy getStrategy(Status bindingStatus) {
        final Date createdAtDate = bindingStatus.getCreatedAt();
        final long createdTime = createdAtDate.getTime();
        return context -> {
          final long deltaInSec = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - createdTime);
          if (deltaInSec <= TimeUnit.SECONDS.toSeconds(1)) {
            return context.getString(R.string.created_now);
          }
          if (deltaInSec < TimeUnit.SECONDS.toSeconds(60)) {
            return context.getString(R.string.created_seconds_ago, TimeUnit.SECONDS.toSeconds(deltaInSec));
          }
          final Resources resources = context.getResources();
          if (deltaInSec < TimeUnit.MINUTES.toSeconds(45)) {
            final int min = (int) TimeUnit.SECONDS.toMinutes(deltaInSec);
            return resources.getQuantityString(R.plurals.created_minutes_ago, min, min);
          }
          if (deltaInSec < TimeUnit.MINUTES.toSeconds(105)) {
            return resources.getQuantityString(R.plurals.created_hours_ago, 1, 1);
          }
          if (deltaInSec < TimeUnit.DAYS.toSeconds(1)) {
            long hours = deltaInSec + TimeUnit.MINUTES.toSeconds(15);
            final int h = (int) TimeUnit.SECONDS.toHours(hours);
            return resources.getQuantityString(R.plurals.created_hours_ago, h, h);
          }
          return timeSpanConv.toTimeSpanString(createdAtDate);
        };
      }
    }, ABSOLUTE {
      @Override
      public TimeTextStrategy getStrategy(Status bindingStatus) {
        return context -> {
          final long createdTime = bindingStatus.getCreatedAt().getTime();
          return DateUtils.formatDateTime(context, createdTime,
              DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
        };
      }
    };

    abstract TimeTextStrategy getStrategy(Status bindingStatus);
  }

  @Override
  public MediaEntity[] getMediaEntities() {
    return bindingStatus.getMediaEntities();
  }

  @Override
  public boolean isPossiblySensitive() {
    return bindingStatus.isPossiblySensitive();
  }

  @Override
  public boolean isRetweet() {
    return isRetweet;
  }

  @Override
  public User getRetweetUser() {
    return rtUser;
  }

  @Override
  public TwitterListItem getQuotedItem() {
    final Status quotedStatus = bindingStatus.getQuotedStatus();
    return quotedStatus != null
        ? new StatusListItem(quotedStatus, TextType.QUOTED) : null;
  }

  private static final Pattern SOURCE_PATTERN = Pattern.compile("<a href=\".*\".*>(.*)</a>");
  private static final String SOURCE_NOT_PROVIDED = "none provided";

  @Override
  public String getSource() {
    final String source = bindingStatus.getSource();
    return source != null ? parseToClientName(source)
        : SOURCE_NOT_PROVIDED;
  }

  private static String parseToClientName(@NonNull String source) {
    final Matcher matcher = SOURCE_PATTERN.matcher(source);
    return matcher.find() ? matcher.group(1)
        : SOURCE_NOT_PROVIDED;
  }

  @Override
  public User getUser() {
    return bindingStatus.getUser();
  }

  @Override
  public int getMediaCount() {
    return bindingStatus.getMediaEntities().length;
  }

  @Override
  public CombinedScreenNameTextView.CombinedName getCombinedName() {
    return new TwitterCombinedName(getUser());
  }
}
