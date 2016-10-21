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
import android.support.annotation.Nullable;
import android.text.TextUtils;

import io.realm.Realm;
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
  private String expandedUrl;
  private String displayUrl;
  @Ignore
  private int start;
  @Ignore
  private int end;

  public URLEntityRealm() {
  }

  URLEntityRealm(URLEntity urlEntity) {
    this.url = urlEntity.getURL();
    this.expandedUrl = urlEntity.getExpandedURL();
    this.displayUrl = urlEntity.getDisplayURL();
    this.start = urlEntity.getStart();
    this.end = urlEntity.getEnd();
  }

  @NonNull
  static RealmList<URLEntityRealm> createList(URLEntity[] urlEntities) {
    if (urlEntities == null || urlEntities.length < 1) {
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
    return expandedUrl;
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

  @NonNull
  static URLEntityRealm findOrCreateFromRealm(URLEntity urlEntity, Realm realm) {
    final URLEntityRealm found = realm.where(URLEntityRealm.class)
        .equalTo("url", urlEntity.getURL())
        .findFirst();
    if (found != null) {
      if (hasDisplayUrl(found)) {
        return found;
      }
      final URLEntityRealm expanded = findExpandedUrl(urlEntity, realm);
      if (expanded == null) {
        return found;
      }
      found.displayUrl = expanded.getDisplayURL();
      found.expandedUrl = expanded.getExpandedURL();
      return found;
    } else {
      if (hasDisplayUrl(urlEntity)) {
        return createFromRealm(urlEntity, realm);
      }
      final URLEntityRealm expanded = findExpandedUrl(urlEntity, realm);
      if (expanded != null) {
        return expanded;
      }
      return createFromRealm(urlEntity, realm);
    }
  }

  @NonNull
  private static URLEntityRealm createFromRealm(URLEntity urlEntity, Realm realm) {
    final URLEntityRealm created = realm.createObject(URLEntityRealm.class, urlEntity.getURL());
    created.displayUrl = urlEntity.getDisplayURL();
    created.expandedUrl = urlEntity.getExpandedURL();
    return created;
  }

  private static boolean hasDisplayUrl(URLEntity found) {
    return !TextUtils.isEmpty(found.getDisplayURL())
        && !TextUtils.isEmpty(found.getExpandedURL());
  }

  @Nullable
  private static URLEntityRealm findExpandedUrl(URLEntity urlEntity, Realm realm) {
    if (urlEntity.getExpandedURL() != null) {
      return null;
    }
    return realm.where(URLEntityRealm.class)
        .equalTo("expandedUrl", urlEntity.getURL())
        .findFirst();
  }
}
