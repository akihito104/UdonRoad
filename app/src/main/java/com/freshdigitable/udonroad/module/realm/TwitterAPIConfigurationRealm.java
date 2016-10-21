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

import java.util.Map;

import io.realm.RealmModel;
import io.realm.annotations.RealmClass;
import twitter4j.MediaEntity;
import twitter4j.RateLimitStatus;
import twitter4j.TwitterAPIConfiguration;

/**
 * TwitterAPIConfigurationRealm is a data class to store Realm.
 *
 * Created by akihit on 2016/07/30.
 */
@RealmClass
public class TwitterAPIConfigurationRealm implements TwitterAPIConfiguration, RealmModel {
  private int photoSizeLimit;
  private int shortURLLength;
  private int shortURLLengthHttps;
  private int charactersReservedPerMedia;
  private int maxMediaPerUpload;

  public TwitterAPIConfigurationRealm() {
  }

  TwitterAPIConfigurationRealm(TwitterAPIConfiguration configuration) {
    this.photoSizeLimit = configuration.getPhotoSizeLimit();
    this.shortURLLength = configuration.getShortURLLength();
    this.shortURLLengthHttps = configuration.getShortURLLengthHttps();
    this.charactersReservedPerMedia = configuration.getCharactersReservedPerMedia();
    this.maxMediaPerUpload = configuration.getMaxMediaPerUpload();
  }

  @Override
  public int getPhotoSizeLimit() {
    return photoSizeLimit;
  }

  @Override
  public int getShortURLLength() {
    return shortURLLength;
  }

  @Override
  public int getShortURLLengthHttps() {
    return shortURLLengthHttps;
  }

  @Override
  public int getCharactersReservedPerMedia() {
    return charactersReservedPerMedia;
  }

  @Override
  public int getDmTextCharacterLimit() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public Map<Integer, MediaEntity.Size> getPhotoSizes() {
    throw new RuntimeException("not implemented yet.");
  }

  @Override
  public String[] getNonUsernamePaths() {
    throw new RuntimeException("not implemented yet.");
  }

  @Override
  public int getMaxMediaPerUpload() {
    return maxMediaPerUpload;
  }

  @Override
  public RateLimitStatus getRateLimitStatus() {
    throw new RuntimeException("not implemented yet.");
  }

  @Override
  public int getAccessLevel() {
    throw new RuntimeException("not implemented yet.");
  }
}
