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

package com.freshdigitable.udonroad.module;

import com.freshdigitable.udonroad.MainActivity;
import com.freshdigitable.udonroad.MainFragmentModule;
import com.freshdigitable.udonroad.oauth.OAuthActivity;
import com.freshdigitable.udonroad.UserSettingsActivity;
import com.freshdigitable.udonroad.media.MediaFragmentModule;
import com.freshdigitable.udonroad.oauth.OAuthActivityModule;
import com.freshdigitable.udonroad.user.UserFragmentModel;
import com.freshdigitable.udonroad.user.UserInfoActivity;
import com.freshdigitable.udonroad.input.TweetInputActivity;
import com.freshdigitable.udonroad.media.MediaViewActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

/**
 * Created by akihit on 2018/03/25.
 */
@Module
public abstract class ActivityBuilders {
  @ContributesAndroidInjector(modules = {OAuthActivityModule.class})
  abstract OAuthActivity contributeOAuthActivity();

  @ContributesAndroidInjector(modules = {MainFragmentModule.class})
  abstract MainActivity contributeMainActivity();

  @ContributesAndroidInjector(modules = {MediaFragmentModule.class})
  abstract MediaViewActivity contributeMediaViewActivity();

  @ContributesAndroidInjector(modules = {UserFragmentModel.class})
  abstract UserInfoActivity contributeUserInfoActivity();

  @ContributesAndroidInjector
  abstract TweetInputActivity contributeTweetInputActivity();

  @ContributesAndroidInjector(modules = {UserSettingsActivity.UserSettingFragmentModel.class})
  abstract UserSettingsActivity contributeUserSettingsActivity();
}
