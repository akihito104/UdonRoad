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

import java.util.Collection;

import twitter4j.TwitterAPIConfiguration;
import twitter4j.User;

/**
 * ConfigStore defines scheme to store user configurations.
 *
 * Created by akihit on 2016/07/30.
 */
public interface ConfigStore {
  void open();

  void close();

  void clear();

  void addAuthenticatedUser(User authenticatedUser);

  User getAuthenticatedUser(long userId);

  void setTwitterAPIConfig(TwitterAPIConfiguration twitterAPIConfig);

  TwitterAPIConfiguration getTwitterAPIConfig();

  void replaceIgnoringUsers(Collection<Long> iDs);

  boolean isIgnoredUser(long userId);

  void addIgnoringUser(User user);

  void removeIgnoringUser(User user);
}
