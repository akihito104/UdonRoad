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

import com.android.annotations.NonNull;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;
import twitter4j.URLEntity;

/**
 * URLEntityRealm is realm object implementation for URLEntity of twitter4j.
 *
 * Created by akihit on 2016/06/23.
 */
public class URLEntityRealm extends RealmObject implements URLEntity {
  @PrimaryKey
  private String url;
  private String expendedUrl;
  private String displayUrl;
  @Ignore
  private int start;
  @Ignore
  private int end;

  public URLEntityRealm() {
  }

  URLEntityRealm(URLEntity urlEntity) {
    this.url = urlEntity.getURL();
    this.expendedUrl = urlEntity.getExpandedURL();
    this.displayUrl = urlEntity.getDisplayURL();
    this.start = urlEntity.getStart();
    this.end = urlEntity.getEnd();
  }

  @NonNull
  static RealmList<URLEntityRealm> createList(URLEntity[] urlEntities) {
    if (urlEntities == null) {
      return new RealmList<>();
    }
    RealmList<URLEntityRealm> urlEntityRealms = new RealmList<>();
    for (URLEntity u : urlEntities) {
      urlEntityRealms.add(new URLEntityRealm(u));
    }
    return urlEntityRealms;
  }

  @Override
  public String getText() {
    return url;
  }

  @Override
  public String getURL() {
    return url;
  }

  @Override
  public String getExpandedURL() {
    return expendedUrl;
  }

  @Override
  public String getDisplayURL() {
    return displayUrl;
  }

  @Override
  public int getStart() {
    return start;
  }

  @Override
  public int getEnd() {
    return end;
  }
}
