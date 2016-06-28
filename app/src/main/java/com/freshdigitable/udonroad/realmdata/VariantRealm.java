/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.realmdata;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import twitter4j.ExtendedMediaEntity;

/**
 * Created by akihit on 2016/06/23.
 */
public class VariantRealm extends RealmObject implements ExtendedMediaEntity.Variant {
  @PrimaryKey
  private String url;
  private int bitrate;
  private String contentType;

  public VariantRealm() {
  }

  public VariantRealm(ExtendedMediaEntity.Variant variant) {
    this.bitrate = variant.getBitrate();
    this.contentType = variant.getContentType();
    this.url = variant.getUrl();
  }
  @Override
  public int getBitrate() {
    return bitrate;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public String getUrl() {
    return url;
  }
}
