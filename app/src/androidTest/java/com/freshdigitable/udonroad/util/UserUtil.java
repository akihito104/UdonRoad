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
  public static User createUserA() {
    return builder(2000,"userA")
        .name("User A").build();
  }

  public static User createVerifiedUser() {
    return builder(3000,"verifiedUser")
        .name("Verified User")
        .isVerified(true)
        .isProtected(false).build();
  }

  public static User createProtectedUser() {
    return builder(4000,"protectedUser")
        .name("Protected User")
        .isVerified(false)
        .isProtected(true).build();
  }

  public static User createVerifiedAndProtectedUser() {
    return builder(5000, "gene")
        .name("Verified and Protected User")
        .isVerified(true)
        .isProtected(true).build();
  }

  public static Builder builder(long id, String screenName) {
    return new Builder(id, screenName);
  }

  public static class Builder {
    private final long id;
    private final String screenName;
    String name;
    boolean isVerified = false;
    boolean isProtected = false;
    String profileBackgroundColor = "ffffff";
    String description = "user description is here.";
    URLEntity[] urlEntities = new URLEntity[0];

    Builder(long id, String screenName) {
      this.id = id;
      this.screenName = screenName;
      this.name = screenName;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder isVerified(boolean isVerified) {
      this.isVerified = isVerified;
      return this;
    }

    public Builder isProtected(boolean isProtected) {
      this.isProtected = isProtected;
      return this;
    }

    public Builder profileBackgroundColor(String profileBackgroundColor) {
      this.profileBackgroundColor = profileBackgroundColor;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder urlEntities(URLEntity[] urlEntities) {
      this.urlEntities = urlEntities;
      return this;
    }

    public User build() {
      final User mock = mock(User.class);
      when(mock.getProfileBackgroundColor()).thenReturn(profileBackgroundColor);
      when(mock.getDescription()).thenReturn(description);
      when(mock.getDescriptionURLEntities()).thenReturn(urlEntities);
      when(mock.getURL()).thenReturn(null);
      when(mock.getLocation()).thenReturn(null);
      when(mock.getScreenName()).thenReturn(screenName);
      when(mock.getName()).thenReturn(name);
      when(mock.getId()).thenReturn(id);
      when(mock.isVerified()).thenReturn(isVerified);
      when(mock.isProtected()).thenReturn(isProtected);
      return mock;
    }
  }

  private UserUtil() {
    throw new AssertionError();
  }
}
