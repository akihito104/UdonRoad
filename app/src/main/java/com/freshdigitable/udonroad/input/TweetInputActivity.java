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

package com.freshdigitable.udonroad.input;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.databinding.ActivityTweetInputBinding;

import javax.inject.Inject;

/**
 * Created by akihit on 2017/12/03.
 */

public class TweetInputActivity extends AppCompatActivity {

  private ActivityTweetInputBinding binding;
  @Inject
  TweetInputViewModel viewModel;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = DataBindingUtil.setContentView(this, R.layout.activity_tweet_input);
    getLifecycle().addObserver(viewModel);
    final MediaContainerPresenter mediaContainerPresenter
        = new MediaContainerPresenter(binding.tweetInputImageContainer, viewModel);
    getLifecycle().addObserver(mediaContainerPresenter);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.tweet_input, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    viewModel.onSaveInstanceState(outState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    viewModel.onViewStateRestored(savedInstanceState);
  }
}
