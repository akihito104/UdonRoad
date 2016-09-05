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

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.freshdigitable.udonroad.ffab.IndicatableFFAB;
import com.freshdigitable.udonroad.ffab.OnFlingAdapter;
import com.freshdigitable.udonroad.ffab.OnFlingListener.Direction;

import java.util.Map;

/**
 * UserAction combines action and resource icon.
 *
 * Created by akihit on 2016/09/03.
 */
public class UserAction {
  private final ActionResource resource;
  private final Runnable action;

  public UserAction() {
    this(null, null);
  }

  public UserAction(@Nullable ActionResource resource, @Nullable Runnable action) {
    this.resource = resource;
    this.action = action;
  }

  public static void setupFlingableFAB(@NonNull IndicatableFFAB iffab,
                                       @NonNull final Map<Direction, UserAction> actionMap,
                                       @NonNull Context context) {
    for (Direction d : actionMap.keySet()) {
      final ActionResource resource = actionMap.get(d).resource;
      iffab.setIndicatorIcon(d,
          resource == null
              ? null
              : resource.createDrawableWithColor(context, Color.WHITE));
    }
    iffab.setOnFlingListener(new OnFlingAdapter() {
      @Override
      public void onFling(Direction direction) {
        if (!actionMap.containsKey(direction)) {
          return;
        }
        final UserAction userAction = actionMap.get(direction);
        if (userAction.resource == null && userAction.action == null) {
          for (Direction d : direction.getBothNeighbor()) {
            runAction(actionMap.get(d));
          }
        } else {
          runAction(userAction);
        }
      }

      private void runAction(@NonNull UserAction userAction) {
        if (userAction.action == null) {
          throw new RuntimeException("action should set.");
        }
        userAction.action.run();
      }
    });
  }
}
