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

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import twitter4j.MediaEntity;

/**
 * VariantRealm is data class defined to store Realm.
 *
 * Created by akihit on 2016/06/23.
 */
public class VariantRealm extends RealmObject implements MediaEntity.Variant {
  @PrimaryKey
  private String url;
  private int bitrate;
  private String contentType;

  public VariantRealm() {
  }

  VariantRealm(MediaEntity.Variant variant) {
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
