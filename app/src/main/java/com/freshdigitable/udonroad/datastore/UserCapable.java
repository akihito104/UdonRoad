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

import android.support.annotation.Nullable;

import rx.Observable;
import twitter4j.User;
import twitter4j.UserMentionEntity;

/**
 * UserCapable defines interface for user data store.
 *
 * Created by akihit on 2016/09/03.
 */
public interface UserCapable {
  void upsert(User user);

  void upsert(UserMentionEntity mentionEntity);

  @Nullable
  User findUser(long userId);

  Observable<User> observeUserById(long userId);
}
