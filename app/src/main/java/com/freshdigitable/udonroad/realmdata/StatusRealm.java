/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.realmdata;

import android.support.annotation.NonNull;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import twitter4j.ExtendedMediaEntity;
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

public class StatusRealm extends RealmObject implements Status {
  @PrimaryKey
  private long id;
  private Date createdAt;
  private RetweetedStatusRealm retweetedStatus;
  private String text;
  private String source;
  private int retweetCount;
  private int favoriteCount;
  private boolean retweetByMe;
  private boolean favorited;
  private UserRealm user;

  public StatusRealm() {
  }

  public StatusRealm(Status status) {
    this.id = status.getId();
    this.createdAt = status.getCreatedAt();
    this.retweetedStatus = status.isRetweet() ?
        new RetweetedStatusRealm(status.getRetweetedStatus()) : null;
    this.text = status.getText();
    this.source = status.getSource();
    this.retweetCount = status.getRetweetCount();
    this.favoriteCount = status.getFavoriteCount();
    this.retweetByMe = status.isRetweetedByMe();
    this.favorited = status.isFavorited();
    this.user = new UserRealm(status.getUser());
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public boolean isTruncated() {
    throw new RuntimeException("not implement yet.");
  }

  public long getInReplyToStatusId() {
    throw new RuntimeException("not implement yet.");
  }

  public long getInReplyToUserId() {
    throw new RuntimeException("not implement yet.");
  }

  public String getInReplyToScreenName() {
    throw new RuntimeException("not implement yet.");
  }

  public GeoLocation getGeoLocation() {
    throw new RuntimeException("not implement yet.");
  }

  public Place getPlace() {
    throw new RuntimeException("not implement yet.");
  }

  public boolean isFavorited() {
    return favorited;
  }

  public void setFavorited(boolean favorited) {
    this.favorited = favorited;
  }

  public boolean isRetweeted() {
    throw new RuntimeException("not implement yet.");
  }

  public int getFavoriteCount() {
    return favoriteCount;
  }

  public void setFavoriteCount(int favoriteCount) {
    this.favoriteCount = favoriteCount;
  }

  public User getUser() {
    return user;
  }

  public void setUser(UserRealm user) {
    this.user = user;
  }

  public boolean isRetweet() {
    return retweetedStatus != null;
  }

  public RetweetedStatusRealm getRetweetedStatus() {
    return retweetedStatus;
  }

  public void setRetweetedStatus(RetweetedStatusRealm retweetedStatus) {
    this.retweetedStatus = retweetedStatus;
  }

  public long[] getContributors() {
    throw new RuntimeException("not implement yet.");
  }

  public int getRetweetCount() {
    return retweetCount;
  }

  public void setRetweetCount(int retweetCount) {
    this.retweetCount = retweetCount;
  }

  public boolean isRetweetedByMe() {
    return retweetByMe;
  }

  public void setRetweetByMe(boolean retweetByMe) {
    this.retweetByMe = retweetByMe;
  }

  public long getCurrentUserRetweetId() {
    throw new RuntimeException("not implement yet.");
  }

  public boolean isPossiblySensitive() {
    throw new RuntimeException("not implement yet.");
  }

  public String getLang() {
    throw new RuntimeException("not implement yet.");
  }

  public Scopes getScopes() {
    throw new RuntimeException("not implement yet.");
  }

  public String[] getWithheldInCountries() {
    throw new RuntimeException("not implement yet.");
  }

  public long getQuotedStatusId() {
    throw new RuntimeException("not implement yet.");
  }

  public Status getQuotedStatus() {
    throw new RuntimeException("not implement yet.");
  }

  public int compareTo(@NonNull Status another) {
    throw new RuntimeException("not implement yet.");
  }

  public UserMentionEntity[] getUserMentionEntities() {
    throw new RuntimeException("not implement yet.");
  }

  public URLEntity[] getURLEntities() {
    throw new RuntimeException("not implement yet.");
  }

  public HashtagEntity[] getHashtagEntities() {
    throw new RuntimeException("not implement yet.");
  }

  public MediaEntity[] getMediaEntities() {
    throw new RuntimeException("not implement yet.");
  }

  public ExtendedMediaEntity[] getExtendedMediaEntities() {
    throw new RuntimeException("not implement yet.");
  }

  public SymbolEntity[] getSymbolEntities() {
    throw new RuntimeException("not implement yet.");
  }

  public RateLimitStatus getRateLimitStatus() {
    throw new RuntimeException("not implement yet.");
  }

  public int getAccessLevel() {
    throw new RuntimeException("not implement yet.");
  }
}
