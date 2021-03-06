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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmList;
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
  private int favoritesCount;
  private int listedCount;
  private String profileLinkColor;
  private RealmList<URLEntityRealm> descriptionURLEntities;
  private String location;
  private String url;
  private URLEntityRealm urlEntity;
  private boolean verified;
  private boolean isProtected; // `protected` is reserved word

  public UserRealm() {
  }

  UserRealm(final User user) {
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
    this.favoritesCount = user.getFavouritesCount();
    this.listedCount = user.getListedCount();
    this.profileLinkColor = user.getProfileLinkColor();
    this.descriptionURLEntities = URLEntityRealm.createList(user.getDescriptionURLEntities());
    this.url = user.getURL();
    if (user.getURLEntity() != null) {
      this.urlEntity = new URLEntityRealm(user.getURLEntity());
    }
    this.location = user.getLocation();
    this.verified = user.isVerified();
    this.isProtected = user.isProtected();
  }

  UserRealm(UserMentionEntity mentionEntity) {
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

  @Override
  public String getEmail() {
    throw new RuntimeException("not implemented yet...");
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getScreenName() {
    return screenName;
  }

  @Override
  public String getLocation() {
    return location;
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

  @Override
  public boolean isDefaultProfileImage() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public String getURL() {
    return url;
  }

  @Override
  public boolean isProtected() {
    return isProtected;
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
    return profileLinkColor;
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
    return favoritesCount;
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
    return verified;
  }

  @Override
  public boolean isTranslator() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public int getListedCount() {
    return listedCount;
  }

  @Override
  public boolean isFollowRequestSent() {
    throw new RuntimeException("not implement yet.");
  }

  @Override
  public URLEntity[] getDescriptionURLEntities() {
    return descriptionURLEntities.toArray(new URLEntity[descriptionURLEntities.size()]);
  }

  @Override
  public URLEntity getURLEntity() {
    return urlEntity;
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

  void merge(@NonNull User u, @NonNull Realm realm) {
    if (u.getDescription() != null) { // description is nullable
      if (description == null || !description.equals(u.getDescription())) {
        this.description = u.getDescription();
      }
      final URLEntity[] descriptionURLEntities = u.getDescriptionURLEntities();
      if (descriptionURLEntities != null && descriptionURLEntities.length > 0) {
        for (int i = this.descriptionURLEntities.size() - 1; i >= 0; i--) {
          final URLEntityRealm old = this.descriptionURLEntities.get(i);
          final int index = Arrays.binarySearch(descriptionURLEntities, old, (l, r) -> l.getURL().compareTo(r.getURL()));
          if (index < 0) {
            this.descriptionURLEntities.remove(old);
          } else {
            old.merge(descriptionURLEntities[index]);
          }
        }
        for (URLEntity url : descriptionURLEntities) {
          final int index = Collections.binarySearch(this.descriptionURLEntities, url, (l, r) -> l.getURL().compareTo(r.getURL()));
          if (index < 0) {
            this.descriptionURLEntities.add(URLEntityRealm.findOrCreateFromRealm(url, realm));
          }
        }
      }
    }
    if (favoritesCount != u.getFavouritesCount()) {
      this.favoritesCount = u.getFavouritesCount();
    }
    if (followersCount != u.getFollowersCount()) {
      this.followersCount = u.getFollowersCount();
    }
    if (friendsCount != u.getFriendsCount()) {
      this.friendsCount = u.getFriendsCount();
    }
    if (miniProfileImageURLHttps == null || !miniProfileImageURLHttps.equals(u.getMiniProfileImageURLHttps())) {
      this.miniProfileImageURLHttps = u.getMiniProfileImageURLHttps();
    }
    if (name == null || !name.equals(u.getName())) {
      this.name = u.getName();
    }
    if (profileBannerMobileURL == null || !profileBannerMobileURL.equals(u.getProfileBannerMobileURL())) {
      this.profileBannerMobileURL = u.getProfileBannerMobileURL();
    }
    if (profileImageURLHttps == null || !profileImageURLHttps.equals(u.getProfileImageURLHttps())) {
      this.profileImageURLHttps = u.getProfileImageURLHttps();
    }
    if (profileLinkColor == null || !profileLinkColor.equals(u.getProfileLinkColor())) {
      this.profileLinkColor = u.getProfileLinkColor();
    }
    if (screenName == null || !screenName.equals(u.getScreenName())) {
      this.screenName = u.getScreenName();
    }
    if (statusesCount != u.getStatusesCount()) {
      this.statusesCount = u.getStatusesCount();
    }
    if (url == null || !url.equals(u.getURL())) {
      this.url = u.getURL();
    }
    final URLEntity urlEntity = u.getURLEntity();
    if (urlEntity != null
        && isNewUrlEntity(urlEntity)) {
      this.urlEntity = URLEntityRealm.findOrCreateFromRealm(urlEntity, realm);
    }
    if (location == null || !location.equals(u.getLocation())) {
      this.location = u.getLocation();
    }
    if (verified != u.isVerified()) {
      this.verified = u.isVerified();
    }
    if (isProtected != u.isProtected()) {
      this.isProtected = u.isProtected();
    }
  }

  private boolean isNewUrlEntity(@NonNull URLEntity urlEntity) {
    if (this.urlEntity == null) {
      return true;
    }
    final String url = urlEntity.getURL();
    return !(url.equals(this.urlEntity.getURL())
        || url.equals(this.urlEntity.getExpandedURL()));
  }
}
