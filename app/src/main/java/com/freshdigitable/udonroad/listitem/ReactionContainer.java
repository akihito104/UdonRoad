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

package com.freshdigitable.udonroad.listitem;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by akihit on 2017/06/17.
 */

public abstract class ReactionContainer extends LinearLayout {
  final List<ListItem.Stat> reactions;

  public ReactionContainer(Context context) {
    this(context, null);
  }

  public ReactionContainer(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ReactionContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    reactions = new ArrayList<>();
  }

  abstract void update(List<ListItem.Stat> stats);
}
