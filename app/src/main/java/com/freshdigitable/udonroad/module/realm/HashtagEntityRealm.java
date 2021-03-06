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
import twitter4j.HashtagEntity;

/**
 * Created by akihit on 2017/08/08.
 */
@RealmClass
public class HashtagEntityRealm implements HashtagEntity, RealmModel {
  @PrimaryKey
  private String text;

  public HashtagEntityRealm() {}

  HashtagEntityRealm(HashtagEntity hashtagEntity) {
    this.text = hashtagEntity.getText();
  }

  @Override
  public String getText() {
    return text;
  }

  @Override
  public int getStart() {
    throw new IllegalStateException("this method is not supported");
  }

  @Override
  public int getEnd() {
    throw new IllegalStateException("this method is not supported");
  }
}
