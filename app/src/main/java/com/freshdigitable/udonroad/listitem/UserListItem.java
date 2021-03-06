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

import com.freshdigitable.udonroad.CombinedScreenNameTextView.CombinedName;

import java.util.Arrays;
import java.util.List;

import twitter4j.MediaEntity;
import twitter4j.User;

/**
 * Created by akihit on 2017/06/15.
 */

public class UserListItem implements TwitterListItem {
  private final User item;

  public UserListItem(User item) {
    this.item = item;
  }

  @Override
  public long getId() {
    return item.getId();
  }

  @Override
  public User getUser() {
    return item;
  }

  @Override
  public CharSequence getText() {
    return item.getDescription();
  }

  @Override
  public List<Stat> getStats() {
    return Arrays.asList(UserStats.FOLLOWERS.getStat(item),
        UserStats.FRIENDS.getStat(item));
  }

  enum UserStats {
    FOLLOWERS(TwitterReactionContainer.ReactionIcon.FOLLOWER) {
      @Override
      Stat getStat(User user) {
        return new Stat() {
          @Override
          public int getType() {
            return icon.type;
          }

          @Override
          public int getCount() {
            return user.getFollowersCount();
          }

          @Override
          public boolean isMarked() {
            return false;
          }
        };
      }
    }, FRIENDS(TwitterReactionContainer.ReactionIcon.FOLLOWING) {
      @Override
      Stat getStat(User user) {
        return new Stat() {
          @Override
          public int getType() {
            return icon.type;
          }

          @Override
          public int getCount() {
            return user.getFriendsCount();
          }

          @Override
          public boolean isMarked() {
            return false;
          }
        };
      }
    };

    final TwitterReactionContainer.ReactionIcon icon;

    UserStats(TwitterReactionContainer.ReactionIcon icon) {
      this.icon = icon;
    }

    abstract Stat getStat(User user);
  }

  @Override
  public String getCreatedTime(Context context) {
    return "";
  }

  @Override
  public String getSource() {
    return "";
  }

  @Override
  public boolean isRetweet() {
    return false;
  }

  @Override
  public User getRetweetUser() {
    return null;
  }

  @Override
  public TwitterListItem getQuotedItem() {
    return null;
  }

  @Override
  public TimeTextStrategy getTimeStrategy() {
    return null;
  }

  @Override
  public MediaEntity[] getMediaEntities() {
    return new MediaEntity[0];
  }

  @Override
  public boolean isPossiblySensitive() {
    return false;
  }

  @Override
  public int getMediaCount() {
    return 0;
  }

  @Override
  public CombinedName getCombinedName() {
    return new TwitterCombinedName(item);
  }
}
