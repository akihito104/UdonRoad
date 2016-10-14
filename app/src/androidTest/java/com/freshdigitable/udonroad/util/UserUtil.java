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

package com.freshdigitable.udonroad.util;

import twitter4j.URLEntity;
import twitter4j.User;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2016/07/31.
 */
public class UserUtil {
  public static User create() {
    final User mock = mock(User.class);
    when(mock.getScreenName()).thenReturn("akihito104");
    when(mock.getName()).thenReturn("Akihito Matsuda");
    when(mock.getId()).thenReturn(2000L);
    when(mock.getProfileBackgroundColor()).thenReturn("ffffff");
    when(mock.getDescription()).thenReturn("user description is here.");
    when(mock.getDescriptionURLEntities()).thenReturn(new URLEntity[0]);
    when(mock.getURL()).thenReturn(null);
    when(mock.getLocation()).thenReturn(null);
    return mock;
  }
}
