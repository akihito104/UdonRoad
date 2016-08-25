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

package com.freshdigitable.udonroad.datastore;

import android.content.Context;

import rx.Observable;
import twitter4j.User;
import twitter4j.UserMentionEntity;

/**
 * Created by akihit on 2016/07/25.
 */
public interface StatusCache extends StatusCapable {
  void open(Context context);

  void upsertUser(User user);

  void upsertUser(UserMentionEntity mentionEntity);

  User getUser(long userId);

  Observable<User> observeUserById(long userId);

  void clear();

  void close();
}
