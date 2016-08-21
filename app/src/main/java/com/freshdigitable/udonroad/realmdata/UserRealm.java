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

package com.freshdigitable.udonroad.realmdata;

import android.support.annotation.NonNull;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import twitter4j.RateLimitStatus;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;

/**
 * Twitter user data to store Realm
 *
 * Created by akihit on 2016/05/04.
 */
public class UserRealm extends RealmObject implements User {
  @PrimaryKey
  private long id;
  private String profileImageURLHttps;
  private String miniProfileImageURLHttps;
  private String name;
  private String screenName;
  private String description;
  private String profileBannerMobileURL;
  private int statusesCount;
  private int followersCount;
  private int friendsCount;

  public UserRealm() {
  }

  public UserRealm(final User user) {
    this.id = user.getId();
    this.profileImageURLHttps = user.getProfileImageURLHttps();
    this.miniProfileImageURLHttps = user.getMiniProfileImageURLHttps();
    this.name = user.getName();
    this.screenName = user.getScreenName();
    this.description = user.getDescription();
    this.profileBannerMobileURL = user.getProfileBannerMobileURL();
    this.statusesCount = user.getStatusesCount();
    this.followersCount = user.getFollowersCount();
    this.friendsCount = user.getFriendsCount();
  }

  public UserRealm(UserMentionEntity mentionEntity) {
    this.id = mentionEntity.getId();
    this.name = mentionEntity.getName();
    this.screenName = mentionEntity.getScreenName();
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
    return this.description;
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
    return miniProfileImageURLHttps;
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
    return this.followersCount;
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
    return this.friendsCount;
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
    return this.profileBannerMobileURL;
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
    return this.statusesCount;
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
