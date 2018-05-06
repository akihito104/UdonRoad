/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad.timeline;

import android.os.Parcel;
import android.os.Parcelable;

public class SelectedItem implements Parcelable {
  public static final SelectedItem NONE = new SelectedItem(-1, -1);
  private final long containerId;
  private final long id;

  SelectedItem(long selectedContainerId, long selectedItemId) {
    this.containerId = selectedContainerId;
    this.id = selectedItemId;
  }

  public long getId() {
    return id;
  }

  public boolean isSame(long itemId) {
    return isSame(itemId, itemId);
  }

  public boolean isSame(long containerItemId, long itemId) {
    return this.containerId == containerItemId && this.id == itemId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SelectedItem that = (SelectedItem) o;

    return id == that.id
        && containerId == that.containerId;
  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + (int) (containerId ^ (containerId >>> 32));
    return result;
  }

  protected SelectedItem(Parcel in) {
    this(in.readLong(), in.readLong());
  }

  public static final Creator<SelectedItem> CREATOR = new Creator<SelectedItem>() {
    @Override
    public SelectedItem createFromParcel(Parcel in) {
      return new SelectedItem(in);
    }

    @Override
    public SelectedItem[] newArray(int size) {
      return new SelectedItem[size];
    }
  };

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(containerId);
    dest.writeLong(id);
  }

  @Override
  public int describeContents() {
    return 0;
  }
}
