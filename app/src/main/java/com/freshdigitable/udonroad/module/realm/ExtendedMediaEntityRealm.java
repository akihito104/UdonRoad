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

import java.util.HashMap;
import java.util.Map;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;
import twitter4j.ExtendedMediaEntity;

/**
 * ExtendedMediaEntityRealm is a data class to store Realm.
 *
 * Created by akihit on 2016/06/23.
 */
public class ExtendedMediaEntityRealm extends RealmObject implements ExtendedMediaEntity {
  @PrimaryKey
  private long id;
  private String url;
  private String mediaUrlHttps;
  private String type;
  private String displayUrl;
  private String expendedUrl;
  @Ignore
  private int start;
  @Ignore
  private int end;
  private int videoAspectRatioHeight;
  private int videoAspectRatioWidth;
  private long durationMillis;
  @Ignore
  private Map<Integer, Size> sizes;
  private String sizeString;
  private RealmList<VariantRealm> videoVariants;

  public ExtendedMediaEntityRealm() {
  }

  ExtendedMediaEntityRealm(ExtendedMediaEntity m) {
    this.id = m.getId();
    this.url = m.getURL();
    this.mediaUrlHttps = m.getMediaURLHttps();
    this.type = m.getType();
    this.displayUrl = m.getDisplayURL();
    this.expendedUrl = m.getExpandedURL();
    this.start = m.getStart();
    this.end = m.getEnd();
    this.videoAspectRatioHeight = m.getVideoAspectRatioHeight();
    this.videoAspectRatioWidth = m.getVideoAspectRatioWidth();
    this.durationMillis = m.getVideoDurationMillis();

    this.sizes = m.getSizes();
    this.sizeString = parseToSizeString();

    final ExtendedMediaEntity.Variant[] videoVariants = m.getVideoVariants();
    this.videoVariants = new RealmList<>();
    for (ExtendedMediaEntity.Variant v : videoVariants) {
      this.videoVariants.add(new VariantRealm(v));
    }
  }

  private String parseToSizeString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<Integer, Size> e : this.sizes.entrySet()) {
      sb.append(e.getKey()).append(",");
      final Size value = e.getValue();
      sb.append(value.getWidth()).append(",");
      sb.append(value.getHeight()).append(",");
      sb.append(value.getResize());
      sb.append("|");
    }
    return sb.deleteCharAt(sb.lastIndexOf("|")).toString();
  }

  private Map<Integer, Size> parseToSizes() {
    Map<Integer, Size> res = new HashMap<>();
    final String[] sizes = this.sizeString.split("|");
    for (String s : sizes) {
      final String[] values = s.split(",");
      final int key = Integer.parseInt(values[0]);
      final Size value = new Size() {
        @Override
        public int getWidth() {
          return Integer.parseInt(values[1]);
        }

        @Override
        public int getHeight() {
          return Integer.parseInt(values[2]);
        }

        @Override
        public int getResize() {
          return Integer.parseInt(values[3]);
        }
      };
      res.put(key, value);
    }
    return res;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public String getMediaURL() {
    return null;
  }

  @Override
  public String getMediaURLHttps() {
    return mediaUrlHttps;
  }

  @Override
  public Map<Integer, Size> getSizes() {
    if (sizes == null) {
      sizes = parseToSizes();
    }
    return sizes;
  }

  @Override
  public String getType() {
    return type;
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

  @Override
  public int getVideoAspectRatioWidth() {
    return videoAspectRatioWidth;
  }

  @Override
  public int getVideoAspectRatioHeight() {
    return videoAspectRatioHeight;
  }

  @Override
  public long getVideoDurationMillis() {
    return durationMillis;
  }

  @Override
  public Variant[] getVideoVariants() {
    return videoVariants.toArray(new Variant[videoVariants.size()]);
  }

  @Override
  public String getExtAltText() {
    throw new RuntimeException("not implemented yet...");
  }
}
