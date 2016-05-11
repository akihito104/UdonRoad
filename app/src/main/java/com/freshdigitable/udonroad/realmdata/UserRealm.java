/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.realmdata;

import android.support.annotation.NonNull;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import twitter4j.RateLimitStatus;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.User;

/**
 * Twitter user data to store Realm
 *
 * Created by akihit on 2016/05/04.
 */
public class UserRealm extends RealmObject implements User {
  @PrimaryKey
  private long id;
  private String profileImageURLHttps;
  private String name;
  private String screenName;

  public UserRealm() {
  }

  public UserRealm(final User user) {
    this.id = user.getId();
    this.profileImageURLHttps = user.getProfileImageURLHttps();
    this.name = user.getName();
    this.screenName = user.getScreenName();
  }

  @Override
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getScreenName() {
    return screenName;
  }

  public void setScreenName(String screenName) {
    this.screenName = screenName;
  }

  @Override
  public String getLocation() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getDescription() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public boolean isContributorsEnabled() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getProfileImageURL() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getBiggerProfileImageURL() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getMiniProfileImageURL() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getOriginalProfileImageURL() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getProfileImageURLHttps() {
    return profileImageURLHttps;
  }

  @Override
  public String getBiggerProfileImageURLHttps() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getMiniProfileImageURLHttps() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getOriginalProfileImageURLHttps() {
    throw new RuntimeException("not implement yet.");
  }

  public void setProfileImageURLHttps(String urlHttps) {
    profileImageURLHttps = urlHttps;
  }

  @Override
  public boolean isDefaultProfileImage() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getURL() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public boolean isProtected() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public int getFollowersCount() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public Status getStatus() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getProfileBackgroundColor() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getProfileTextColor() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getProfileLinkColor() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getProfileSidebarFillColor() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getProfileSidebarBorderColor() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public boolean isProfileUseBackgroundImage() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public boolean isDefaultProfile() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public boolean isShowAllInlineMedia() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public int getFriendsCount() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public Date getCreatedAt() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public int getFavouritesCount() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public int getUtcOffset() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getTimeZone() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getProfileBackgroundImageURL() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getProfileBackgroundImageUrlHttps() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getProfileBannerURL() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getProfileBannerRetinaURL() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getProfileBannerIPadURL() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getProfileBannerIPadRetinaURL() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getProfileBannerMobileURL() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getProfileBannerMobileRetinaURL() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public boolean isProfileBackgroundTiled() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getLang() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public int getStatusesCount() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public boolean isGeoEnabled() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public boolean isVerified() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public boolean isTranslator() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public int getListedCount() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public boolean isFollowRequestSent() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public URLEntity[] getDescriptionURLEntities() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public URLEntity getURLEntity() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String[] getWithheldInCountries() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public int compareTo(@NonNull User another) {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public RateLimitStatus getRateLimitStatus() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public int getAccessLevel() {
    throw new RuntimeException("not implement yet.");
  }
}
