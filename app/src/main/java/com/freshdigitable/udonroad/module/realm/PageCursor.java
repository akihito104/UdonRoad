/*
 * Copyright (c) 2017. Matsuda, Akihit (akihito104)
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

import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

/**
 * Created by akihit on 2017/07/02.
 */
@RealmClass
public class PageCursor implements RealmModel {
  static final int TYPE_NEXT = 1;
  static final int TYPE_PREV = -1;
  @PrimaryKey
  int type;
  long cursor;

  public PageCursor() {}

  PageCursor(int type, long cursor) {
    this.type = type;
    this.cursor = cursor;
  }
}
