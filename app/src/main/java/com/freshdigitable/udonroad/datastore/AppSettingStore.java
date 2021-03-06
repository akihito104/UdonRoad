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

import java.io.File;
import java.util.List;
import java.util.Set;

import io.reactivex.Observable;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.User;
import twitter4j.auth.AccessToken;

/**
 * Created by akihit on 2016/11/07.
 */

public interface AppSettingStore extends BaseCache {
  void addAuthenticatedUser(User authenticatedUser);

  List<? extends User> getAllAuthenticatedUsers();

  Set<String> getAllAuthenticatedUserIds();

  boolean hasAuthenticatedUser();

  void setTwitterAPIConfig(TwitterAPIConfiguration twitterAPIConfig);

  TwitterAPIConfiguration getTwitterAPIConfig();

  boolean isTwitterAPIConfigFetchable();

  void storeAccessToken(AccessToken accessToken);

  AccessToken getCurrentUserAccessToken();

  long getCurrentUserId();

  File getCurrentUserDir();

  void setCurrentUserId(long userId);

  Observable<User> observeCurrentUser();
}
