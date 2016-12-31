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

package com.freshdigitable.udonroad.datastore;

import android.support.annotation.NonNull;

import java.util.Date;

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

/**
 * PerspectivalStatusImpl is as a wrapper class of Status.
 *
 * Created by akihit on 2016/12/08.
 */

public class PerspectivalStatusImpl implements PerspectivalStatus {
  private final Status status;
  private StatusReaction reaction;
  private final PerspectivalStatus retweetedStatus;
  private final PerspectivalStatus quotedStatus;

  public PerspectivalStatusImpl(@NonNull Status status) {
    this.status = status;
    this.reaction = new StatusReactionImpl(status);
    this.retweetedStatus = status.isRetweet()
        ? new PerspectivalStatusImpl(status.getRetweetedStatus()) : null;
    this.quotedStatus = status.getQuotedStatusId() > 0
        ? new PerspectivalStatusImpl(status.getQuotedStatus()) : null;
  }

  @Override
  public StatusReaction getStatusReaction() {
    return reaction;
  }

  @Override
  public void setStatusReaction(StatusReaction reaction) {
    this.reaction = reaction;
  }

  @Override
  public Date getCreatedAt() {
    return status.getCreatedAt();
  }

  @Override
  public long getId() {
    return status.getId();
  }

  @Override
  public String getText() {
    return status.getText();
  }

  @Override
  public int getDisplayTextRangeStart() {
    return status.getDisplayTextRangeStart();
  }

  @Override
  public int getDisplayTextRangeEnd() {
    return status.getDisplayTextRangeEnd();
  }

  @Override
  public String getSource() {
    return status.getSource();
  }

  @Override
  public boolean isTruncated() {
    return status.isTruncated();
  }

  @Override
  public long getInReplyToStatusId() {
    return status.getInReplyToStatusId();
  }

  @Override
  public long getInReplyToUserId() {
    return status.getInReplyToUserId();
  }

  @Override
  public String getInReplyToScreenName() {
    return status.getInReplyToScreenName();
  }

  @Override
  public GeoLocation getGeoLocation() {
    return status.getGeoLocation();
  }

  @Override
  public Place getPlace() {
    return status.getPlace();
  }

  @Override
  public boolean isFavorited() {
    if (reaction == null || reaction.isFavorited() == null) {
      return false;
    }
    return reaction.isFavorited();
  }

  @Override
  public boolean isRetweeted() {
    if (reaction == null || reaction.isRetweeted() == null) {
      return false;
    }
    return reaction.isRetweeted();
  }

  @Override
  public int getFavoriteCount() {
    return status.getFavoriteCount();
  }

  @Override
  public User getUser() {
    return status.getUser();
  }

  @Override
  public boolean isRetweet() {
    return status.isRetweet();
  }

  @Override
  public PerspectivalStatus getRetweetedStatus() {
    return retweetedStatus;
  }

  @Override
  public long[] getContributors() {
    return status.getContributors();
  }

  @Override
  public int getRetweetCount() {
    return status.getRetweetCount();
  }

  @Override
  public boolean isRetweetedByMe() {
    return status.isRetweetedByMe();
  }

  @Override
  public long getCurrentUserRetweetId() {
    return status.getCurrentUserRetweetId();
  }

  @Override
  public boolean isPossiblySensitive() {
    return status.isPossiblySensitive();
  }

  @Override
  public String getLang() {
    return status.getLang();
  }

  @Override
  public Scopes getScopes() {
    return status.getScopes();
  }

  @Override
  public String[] getWithheldInCountries() {
    return status.getWithheldInCountries();
  }

  @Override
  public long getQuotedStatusId() {
    return status.getQuotedStatusId();
  }

  @Override
  public PerspectivalStatus getQuotedStatus() {
    return quotedStatus;
  }

  @Override
  public int compareTo(@NonNull Status status) {
    return status.compareTo(status);
  }

  @Override
  public UserMentionEntity[] getUserMentionEntities() {
    return status.getUserMentionEntities();
  }

  @Override
  public URLEntity[] getURLEntities() {
    return status.getURLEntities();
  }

  @Override
  public HashtagEntity[] getHashtagEntities() {
    return status.getHashtagEntities();
  }

  @Override
  public MediaEntity[] getMediaEntities() {
    return status.getMediaEntities();
  }

  @Override
  public SymbolEntity[] getSymbolEntities() {
    return status.getSymbolEntities();
  }

  @Override
  public RateLimitStatus getRateLimitStatus() {
    return status.getRateLimitStatus();
  }

  @Override
  public int getAccessLevel() {
    return status.getAccessLevel();
  }
}
