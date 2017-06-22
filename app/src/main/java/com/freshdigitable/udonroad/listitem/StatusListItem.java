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
import android.text.Html;
import android.text.format.DateUtils;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.SpannableStringUtil;
import com.freshdigitable.udonroad.Utils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.User;
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

  public StatusListItem(Status item, TextType textType) {
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
        return SpannableStringUtil.create(bindingStatus);
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
        return new Stat() {
          @Override
          public int getType() {
            return icon.type;
          }

          @Override
          public int getCount() {
            return status.getRetweetCount();
          }

          @Override
          public boolean isMarked() {
            return status.isRetweeted();
          }
        };
      }
    }, FAV(TwitterReactionContainer.ReactionIcon.FAV) {
      @Override
      Stat getStat(Status status) {
        return new Stat() {
          @Override
          public int getType() {
            return icon.type;
          }

          @Override
          public int getCount() {
            return status.getFavoriteCount();
          }

          @Override
          public boolean isMarked() {
            return status.isFavorited();
          }
        };
      }
    }, HAS_REPLY(TwitterReactionContainer.ReactionIcon.IN_REPLY_TO) {
      @Override
      Stat getStat(Status status) {
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
            return status.getInReplyToStatusId() > 0;
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
        return context -> {
          final Date createdAtDate = bindingStatus.getCreatedAt();
          final long deltaInSec = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - createdAtDate.getTime());
          if (deltaInSec <= TimeUnit.SECONDS.toSeconds(1)) {
            return context.getString(R.string.created_now);
          }
          if (deltaInSec < TimeUnit.SECONDS.toSeconds(60)) {
            return context.getString(R.string.created_seconds_ago, TimeUnit.SECONDS.toSeconds(deltaInSec));
          }
          if (deltaInSec <= TimeUnit.MINUTES.toSeconds(1)) {
            return context.getString(R.string.created_a_minute_ago);
          }
          if (deltaInSec < TimeUnit.MINUTES.toSeconds(45)) {
            return context.getString(R.string.created_minutes_ago, TimeUnit.SECONDS.toMinutes(deltaInSec));
          }
          if (deltaInSec < TimeUnit.MINUTES.toSeconds(105)) {
            return context.getString(R.string.created_a_hour_ago);
          }
          if (deltaInSec < TimeUnit.DAYS.toSeconds(1)) {
            long hours = deltaInSec + TimeUnit.MINUTES.toSeconds(15);
            return context.getString(R.string.created_hours_ago, TimeUnit.SECONDS.toHours(hours));
          }
          return timeSpanConv.toTimeSpanString(createdAtDate);
        };
      }
    }, ABSOLUTE {
      @Override
      public TimeTextStrategy getStrategy(Status bindingStatus) {
        return context -> DateUtils.formatDateTime(context, bindingStatus.getCreatedAt().getTime(),
            DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
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
  public ListItem getQuotedItem() {
    final Status quotedStatus = bindingStatus.getQuotedStatus();
    return quotedStatus != null
        ? new StatusListItem(quotedStatus, TextType.QUOTED) : null;
  }

  @Override
  public String getSource() {
    final String source = bindingStatus.getSource();
    return source != null
        ? Html.fromHtml(source).toString()
        : "none provided";
  }

  @Override
  public User getUser() {
    return bindingStatus.getUser();
  }

  @Override
  public int getMediaCount() {
    return bindingStatus.getMediaEntities().length;
  }
}
