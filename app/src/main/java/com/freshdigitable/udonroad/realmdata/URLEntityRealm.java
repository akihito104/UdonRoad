/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.realmdata;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import twitter4j.URLEntity;

/**
 * Created by akihit on 2016/06/23.
 */
public class URLEntityRealm extends RealmObject implements URLEntity {
  @PrimaryKey
  private String url;
  private String expendedUrl;
  private String displayUrl;
  private int start;
  private int end;

  public URLEntityRealm() {
  }

  public URLEntityRealm(URLEntity urlEntity) {
    this.url = urlEntity.getURL();
    this.expendedUrl = urlEntity.getExpandedURL();
    this.displayUrl = urlEntity.getDisplayURL();
    this.start = urlEntity.getStart();
    this.end = urlEntity.getEnd();
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
