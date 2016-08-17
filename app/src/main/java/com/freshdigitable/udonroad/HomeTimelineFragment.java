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

package com.freshdigitable.udonroad;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * displays Authenticated user home timeline.
 *
 * Created by akihit on 2016/06/07.
 */
public class HomeTimelineFragment extends TimelineFragment {
  @SuppressWarnings("unused")
  private static final String TAG = HomeTimelineFragment.class.getSimpleName();
  private UserStreamUtil userStream;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    userStream = new UserStreamUtil(super.timelineSubscriber.getStatusStore());
    InjectionUtil.getComponent(this).inject(userStream);
    return super.onCreateView(inflater, container, savedInstanceState);
  }

  @Override
  public void onStart() {
    super.onStart();
    userStream.connect();
  }

  @Override
  public void onStop() {
    Log.d(TAG, "onStop: ");
    super.onStop();
  }

  @Override
  public void onDestroyView() {
    Log.d(TAG, "onDestroyView: ");
    super.onDestroyView();
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy: ");
    userStream.disconnect();
    super.onDestroy();
  }
}
