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

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import com.freshdigitable.udonroad.AppViewModelProviderFactory;
import com.freshdigitable.udonroad.detail.StatusDetailViewModel;
import com.freshdigitable.udonroad.input.TweetInputViewModel;
import com.freshdigitable.udonroad.timeline.TimelineViewModel;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

/**
 * Created by akihit on 2018/01/07.
 */
@Module
public abstract class ViewModelModule {
  @Binds
  @IntoMap
  @ViewModelKey(TweetInputViewModel.class)
  abstract ViewModel bindTweetInputViewModel(TweetInputViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(TimelineViewModel.class)
  abstract ViewModel bindTimelineViewModel(TimelineViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(StatusDetailViewModel.class)
  abstract ViewModel bindStatusDetailViewModel(StatusDetailViewModel viewModel);

  @Binds
  abstract ViewModelProvider.Factory bindsViewModelProviderFactory(AppViewModelProviderFactory providerFactory);
}
