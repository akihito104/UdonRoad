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

package com.freshdigitable.udonroad.ffab;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.ffab.OnFlickListener.Direction;

import java.util.List;

import static android.view.View.VISIBLE;

/**
 * IffabMenuPresenter manages iffab view object, FlingableFAB and IndicatorView.
 *
 * Created by akihit on 2017/01/18.
 */

class IffabMenuPresenter {
  private ActionIndicatorView indicator;
  private FlickableFAB ffab;
  private int indicatorMargin;
  private IffabMenu menu;

  void initForMenu(IffabMenu menu) {
    this.menu = menu;
  }

  void initView(IndicatableFFAB iffab) {
    final ViewGroup root = (ViewGroup) iffab.getRootView().findViewById(android.R.id.content);
    indicator = (ActionIndicatorView) LayoutInflater.from(iffab.getContext())
        .inflate(R.layout.view_action_indicator_layout, root);

    this.ffab = iffab;
    ffab.setOnFlingListener(new OnFlickAdapter() {
      @Override
      public void onFlick(Direction direction) {
        menu.dispatchMenuItemSelected(direction);
      }
    });
    ViewCompat.setElevation(indicator, ffab.getCompatElevation());

    ffab.setOnTouchListener(new View.OnTouchListener() {
      private MotionEvent old;

      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        final int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
          old = MotionEvent.obtain(motionEvent);
          onStart();
          return false;
        }
        final Direction direction = Direction.getDirection(old, motionEvent);
        if (action == MotionEvent.ACTION_MOVE) {
          onMoving(direction);
        } else if (action == MotionEvent.ACTION_UP) {
          old.recycle();
          onFlick(view.getHandler());
        }
        return false;
      }

      private Direction prevSelected = Direction.UNDEFINED;

      void onStart() {
        indicator.onActionLeave(prevSelected);
        prevSelected = Direction.UNDEFINED;
        indicator.setVisibility(View.VISIBLE);
      }

      void onMoving(Direction direction) {
        if (prevSelected == direction) {
          return;
        }
        indicator.onActionLeave(prevSelected);
        if (menu.isVisibleByDirectin(direction)) {
          indicator.onActionSelected(direction);
        }
        prevSelected = direction;
      }

      void onFlick(Handler handler) {
        handler.postDelayed(() -> indicator.setVisibility(View.INVISIBLE), 200);
      }
    });
  }

  void setFabIcon(Drawable fabIcon) {
    ffab.setImageDrawable(fabIcon);
  }

  void setFabTint(int fabTint) {
    ViewCompat.setBackgroundTintList(ffab, ColorStateList.valueOf(fabTint));
  }

  void setIndicatorTint(int indicatorTint) {
    indicator.setBackgroundColor(indicatorTint);
  }

  void setIndicatorVisibility(int visibility) {
    indicator.setVisibility(visibility);
  }

  void setIndicatorMargin() {
    if (indicator.getVisibility() != VISIBLE) {
      return;
    }
    final MarginLayoutParams layoutParams = (MarginLayoutParams) indicator.getLayoutParams();
    final MarginLayoutParams ffabLp = (MarginLayoutParams) ffab.getLayoutParams();
    layoutParams.bottomMargin = indicatorMargin + ffab.getHeight() + ffabLp.bottomMargin;
  }

  void setIndicatorMargin(int indicatorMargin) {
    this.indicatorMargin = indicatorMargin;
  }

//  void show() {
//    ffab.show();
//  }

//  void hide() {
//    ffab.hide();
//  }

  void clear() {
    indicator.clear();
  }

  void updateMenu() {
    if (pendingUpdate) {
      return;
    }
    final List<IffabMenuItem> items = menu.getVisibleItems();
    for (IffabMenuItem i : items) {
      indicator.setDrawable(i.getDirection(), i.getIcon());
    }
  }

  private boolean pendingUpdate = false;

  void setPendingUpdate(boolean pendingUpdate) {
    this.pendingUpdate = pendingUpdate;
  }

  void setIndicatorIconTint(int indicatorIconTint) {
    indicator.setIndicatorIconTint(indicatorIconTint);
  }
}
