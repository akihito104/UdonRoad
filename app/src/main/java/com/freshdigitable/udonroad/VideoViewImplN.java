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

package com.freshdigitable.udonroad;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.VideoView;

/**
 * Created by akihit on 2017/04/09.
 */

public class VideoViewImplN extends VideoView {
  private static final String TAG = VideoViewImplN.class.getSimpleName();
  public VideoViewImplN(Context context) {
    super(context);
  }

  public VideoViewImplN(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public VideoViewImplN(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  private void openVideo() {
    Log.d(TAG, "openVideo: ");
  }

}
