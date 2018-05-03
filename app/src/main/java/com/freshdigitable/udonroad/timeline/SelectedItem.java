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

public class SelectedItem {
  static final SelectedItem NONE = new SelectedItem(-1, -1);
  private final long id;
  private final long containerId;

  SelectedItem(long selectedContainerId, long selectedItemId) {
    this.id = selectedItemId;
    this.containerId = selectedContainerId;
  }

  public long getId() {
    return id;
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
}
