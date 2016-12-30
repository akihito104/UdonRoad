/*
 * Copyright (c) 2016. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad.module.realm;

import android.support.annotation.NonNull;

import com.freshdigitable.udonroad.datastore.PerspectivalStatus;
import com.freshdigitable.udonroad.datastore.StatusReaction;
import com.freshdigitable.udonroad.datastore.StatusReactionImpl;

import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;
import twitter4j.GeoLocation;
import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Place;
import twitter4j.RateLimitStatus;
import twitter4j.Scopes;
import twitter4j.Status;
import twitter4j.SymbolEntity;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;

public class StatusRealm extends RealmObject implements PerspectivalStatus {
  static final String KEY_ID = "id";
  static final String KEY_RETWEETED_STATUS_ID = "retweetedStatusId";
  static final String KEY_QUOTAD_STATUS_ID = "quotedStatusId";

  @PrimaryKey
  private long id;
  private Date createdAt;
  @Ignore
  private Status retweetedStatus;
  private long retweetedStatusId;
  private String text;
  private String source;
  private int retweetCount;
  private int favoriteCount;
  private boolean retweet;
  @Ignore
  private User user;
  private long userId;
  private RealmList<URLEntityRealm> urlEntities;
  private RealmList<MediaEntityRealm> mediaEntities;
  private RealmList<UserMentionEntityRealm> userMentionEntities;
  @Ignore
  private Status quotedStatus;
  private long quotedStatusId;
  @Ignore
  private StatusReaction reaction;

  public StatusRealm() {
  }

  StatusRealm(Status status) {
    this.id = status.getId();
    this.createdAt = status.getCreatedAt();
    this.retweetedStatus = status.getRetweetedStatus();
    this.retweet = status.isRetweet();
    if (status.isRetweet()) {
      this.retweetedStatusId = this.retweetedStatus.getId();
    }
    this.text = status.getText();
    this.source = status.getSource();
    this.retweetCount = status.getRetweetCount();
    this.favoriteCount = status.getFavoriteCount();
    this.reaction = new StatusReactionImpl(status);
    this.user = status.getUser();
    this.userId = user.getId();
    this.urlEntities = URLEntityRealm.createList(status.getURLEntities());

    this.mediaEntities = new RealmList<>();
    final MediaEntity[] me = status.getMediaEntities();
    for (MediaEntity m : me) {
      mediaEntities.add(new MediaEntityRealm(m));
    }
    final UserMentionEntity[] userMentionEntities = status.getUserMentionEntities();
    this.userMentionEntities = new RealmList<>();
    for (UserMentionEntity u : userMentionEntities) {
      this.userMentionEntities.add(new UserMentionEntityRealm(u));
    }

    this.quotedStatus = status.getQuotedStatus();
    this.quotedStatusId = status.getQuotedStatusId();
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public long getId() {
    return id;
  }

  public String getText() {
    return text;
  }

  public String getSource() {
    return source;
  }

  public boolean isFavorited() {
    if (reaction == null || reaction.isFavorited() == null) {
      return false;
    }
    return reaction.isFavorited();
  }

  public boolean isRetweeted() {
    if (reaction == null || reaction.isRetweeted() == null) {
      return false;
    }
    return reaction.isRetweeted();
  }

  public int getFavoriteCount() {
    return favoriteCount;
  }

  public User getUser() {
    return user;
  }

  void setUser(User user) {
    this.user = user;
  }

  public boolean isRetweet() {
    return retweet;
  }

  public Status getRetweetedStatus() {
    return retweetedStatus;
  }

  void setRetweetedStatus(Status retweetedStatus) {
    this.retweetedStatus = retweetedStatus;
  }

  public int getRetweetCount() {
    return retweetCount;
  }

  @Override
  public long getQuotedStatusId() {
    return quotedStatusId;
  }

  @Override
  public Status getQuotedStatus() {
    return quotedStatus;
  }

  void setQuotedStatus(Status quotedStatus) {
    this.quotedStatus = quotedStatus;
  }

  public UserMentionEntity[] getUserMentionEntities() {
    return userMentionEntities.toArray(new UserMentionEntity[userMentionEntities.size()]);
  }

  public URLEntity[] getURLEntities() {
    if (urlEntities == null) {
      return new URLEntity[0];
    }
    return urlEntities.toArray(new URLEntity[urlEntities.size()]);
  }

  public MediaEntity[] getMediaEntities() {
    return mediaEntities.toArray(new MediaEntity[mediaEntities.size()]);
  }

  @Override
  public String toString() {
    final String s = text.replaceAll("\n", "");
    final String sub = s.length() > 8
        ? s.substring(0, 8)
        : s;
    return "id:" + id +
        ", date:" + createdAt.getTime() +
        ", @" + user.getScreenName() +
        ", text:" + sub;
  }

  long getRetweetedStatusId() {
    return retweetedStatusId;
  }

  long getUserId() {
    return userId;
  }

  @Override
  public void setStatusReaction(StatusReaction reaction) {
    this.reaction = reaction;
  }

  @Override
  public StatusReaction getStatusReaction() {
    return reaction;
  }

  void merge(@NonNull Status s) {
    final int favoriteCount = s.getFavoriteCount();
    if (favoriteCount >= 0 && favoriteCount != this.favoriteCount) { // favoriteCount is nullable
      this.favoriteCount = favoriteCount;
    }
    final int retweetCount = s.getRetweetCount();
    if (retweetCount >= 0 && retweetCount != this.retweetCount) {  // retweetCount is nullable
      this.retweetCount = retweetCount;
    }
  }

  private static final String NOT_IMPLEMENT_YET = "not implement yet.";

  @Override
  public int getDisplayTextRangeStart() {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }

  @Override
  public int getDisplayTextRangeEnd() {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }

  public long[] getContributors() {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }

  public boolean isRetweetedByMe() {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }

  public long getCurrentUserRetweetId() {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }

  public boolean isPossiblySensitive() {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }

  public String getLang() {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }

  public Scopes getScopes() {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }

  public String[] getWithheldInCountries() {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }

  public boolean isTruncated() {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }

  public long getInReplyToStatusId() {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }

  public long getInReplyToUserId() {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }

  public String getInReplyToScreenName() {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }

  public GeoLocation getGeoLocation() {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }

  public Place getPlace() {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }

  public HashtagEntity[] getHashtagEntities() {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }

  public SymbolEntity[] getSymbolEntities() {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }

  public RateLimitStatus getRateLimitStatus() {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }

  public int getAccessLevel() {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }

  public int compareTo(@NonNull Status another) {
    throw new RuntimeException(NOT_IMPLEMENT_YET);
  }
}
