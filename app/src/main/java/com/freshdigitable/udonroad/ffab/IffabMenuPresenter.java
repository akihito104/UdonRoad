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

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.ffab.OnFlickListener.Direction;

import java.util.List;

/**
 * IffabMenuPresenter manages iffab view object, FlingableFAB and IndicatorView.
 *
 * Created by akihit on 2017/01/18.
 */

class IffabMenuPresenter {
  private final ActionIndicatorView indicator;
  private IndicatableFFAB ffab;
  private final int indicatorMargin;
  private final IffabMenu menu;

  IffabMenuPresenter(Context context, AttributeSet attrs, int defStyleAttr) {
    indicator = new ActionIndicatorView(context);
    menu = new IffabMenu(context, this);

    final TypedArray a = context.obtainStyledAttributes(attrs,
        R.styleable.IndicatableFFAB, defStyleAttr, R.style.Widget_FFAB_IndicatableFFAB);
    try {
      final int indicatorTint = a.getColor(R.styleable.IndicatableFFAB_indicatorTint, 0);
      indicator.setBackgroundColor(indicatorTint);
      final int indicatorIconTint = a.getColor(R.styleable.IndicatableFFAB_indicatorIconTint, 0);
      indicator.setIndicatorIconTint(indicatorIconTint);
      this.indicatorMargin = a.getDimensionPixelSize(R.styleable.IndicatableFFAB_marginFabToIndicator, 0);

      final boolean toolbarEnabled = a.getBoolean(R.styleable.IndicatableFFAB_enableToolbar, true);
      if (toolbarEnabled) {
        bbt = new BottomButtonsToolbar(context);
        bbt.setVisibility(View.INVISIBLE);
      } else {
        bbt = null;
      }

      if (a.hasValue(R.styleable.IndicatableFFAB_menu)) {
        final int menuRes = a.getResourceId(R.styleable.IndicatableFFAB_menu, 0);
        inflateMenu(context, menuRes);
      }
    } finally {
      a.recycle();
    }
  }

  private void inflateMenu(Context context, int menuRes) {
    setPendingUpdate(true);
    final IffabMenuItemInflater menuInflater = new IffabMenuItemInflater(context);
    menuInflater.inflate(menuRes, menu);
    setPendingUpdate(false);
    if (bbt != null) {
      bbt.setMenu(menu);
    }
    updateMenu();
  }

  void initView(IndicatableFFAB iffab) {
    this.ffab = iffab;
    ffab.setOnFlickListener(new OnFlickAdapter() {
      @Override
      public void onFlick(Direction direction) {
        menu.dispatchSelectedMenuItem(direction);
      }
    });

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
          onFlick();
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
        if (menu.isVisibleByDirection(direction)) {
          indicator.onActionSelected(direction);
        }
        prevSelected = direction;
      }

      void onFlick() {
        final Message msg = handler.obtainMessage(
            MSG_DISMISS_ACTION_ITEM_VIEW, View.INVISIBLE, 0, indicator);
        handler.sendMessageDelayed(msg, 200);
      }
    });

    ViewCompat.setElevation(indicator, ffab.getCompatElevation());

    if (bbt != null) {
      ViewCompat.setElevation(bbt, ffab.getCompatElevation());
      if (bbt.getParent() == null) {
        final Message message = handler.obtainMessage(MSG_LAYOUT_BOTTOM_TOOLBAR, this);
        handler.sendMessage(message);
      }
    }
  }

  void setIndicatorVisibility(int visibility) {
    indicator.setVisibility(visibility);
  }

  void clear() {
    if (indicator != null) {
      indicator.clear();
    }
    if (bbt != null) {
      bbt.clear();
    }
  }

  void updateMenu() {
    if (pendingUpdate) {
      return;
    }
    final List<IffabMenuItem> items = menu.getEnableItems();
    for (IffabMenuItem i : items) {
      indicator.setDrawable(i.getDirection(), i.getIcon());
    }
    if (bbt != null) {
      bbt.updateItems();
    }
  }

  private boolean pendingUpdate = false;

  void setPendingUpdate(boolean pendingUpdate) {
    this.pendingUpdate = pendingUpdate;
  }

  private static final int MSG_LAYOUT_ACTION_ITEM_VIEW = 1;
  private static final int MSG_DISMISS_ACTION_ITEM_VIEW = 2;
  private static final int MSG_LAYOUT_BOTTOM_TOOLBAR = 3;

  private final Handler handler = new Handler(Looper.getMainLooper(), msg -> {
    if (msg.what == MSG_LAYOUT_ACTION_ITEM_VIEW) {
      ((IffabMenuPresenter) msg.obj).layoutActionItemView();
      return true;
    }
    if (msg.what == MSG_DISMISS_ACTION_ITEM_VIEW) {
      ((ActionIndicatorView) msg.obj).setVisibility(msg.arg1);
      return true;
    }
    if (msg.what == MSG_LAYOUT_BOTTOM_TOOLBAR) {
      ((IffabMenuPresenter) msg.obj).layoutToolbar();
      return true;
    }
    return false;
  });

  void onFabAttachedToWindow() {
    if (indicator.getParent() != null) {
      return;
    }
    final Message msg = handler.obtainMessage(MSG_LAYOUT_ACTION_ITEM_VIEW, this);
    handler.sendMessage(msg);
  }

  private void layoutActionItemView() {
    final int fabWidth = ffab.getWidth();
    final int fabHeight = ffab.getHeight() * 9 / 10;
    final ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) ffab.getLayoutParams();
    if (layoutParams instanceof FrameLayout.LayoutParams) {
      final FrameLayout.LayoutParams mlp = new FrameLayout.LayoutParams(fabWidth, fabHeight);
      mlp.gravity = ((FrameLayout.LayoutParams) layoutParams).gravity;
      setIndicatorMarginFromEdge(mlp, mlp.gravity);
      ((ViewGroup) ffab.getParent()).addView(indicator, mlp);
    } else if (layoutParams instanceof CoordinatorLayout.LayoutParams) {
      final CoordinatorLayout.LayoutParams mlp = new CoordinatorLayout.LayoutParams(fabWidth, fabHeight);
      mlp.gravity = ((CoordinatorLayout.LayoutParams) layoutParams).gravity;
      setIndicatorMarginFromEdge(mlp, mlp.gravity);
      ((ViewGroup) ffab.getParent()).addView(indicator, mlp);
    }
  }

  private void setIndicatorMarginFromEdge(ViewGroup.MarginLayoutParams mlp, int gravity) {
    final ViewGroup.MarginLayoutParams ffabLp = (ViewGroup.MarginLayoutParams) ffab.getLayoutParams();
    if (hasGravityFlagOf(Gravity.BOTTOM, gravity)) {
      mlp.bottomMargin = ffabLp.bottomMargin + mlp.height + indicatorMargin;
    } else if (hasGravityFlagOf(Gravity.TOP, gravity)) {
      mlp.topMargin = ffabLp.topMargin + mlp.height + indicatorMargin;
    } else if (hasGravityFlagOf(Gravity.RIGHT, gravity)) {
      mlp.rightMargin = ffabLp.rightMargin + mlp.width + indicatorMargin;
    } else if (hasGravityFlagOf(Gravity.LEFT, gravity)) {
      mlp.leftMargin = ffabLp.leftMargin + mlp.width + indicatorMargin;
    } else if (hasGravityFlagOf(Gravity.START, gravity)) {
      MarginLayoutParamsCompat.setMarginStart(
          mlp, MarginLayoutParamsCompat.getMarginStart(ffabLp) + mlp.width + indicatorMargin);
    } else if (hasGravityFlagOf(Gravity.END, gravity)) {
      MarginLayoutParamsCompat.setMarginEnd(
          mlp, MarginLayoutParamsCompat.getMarginEnd(ffabLp) + mlp.width + indicatorMargin);
    }
  }

  private static boolean hasGravityFlagOf(int expected, int actual) {
    return (expected & actual) == expected;
  }

  private final BottomButtonsToolbar bbt;

  void transToToolbar() {
    updateMenuItemCheckable(true);
    animateToShowToolbar();
  }

  private void animateToShowToolbar() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      TransformAnimator.transToToolbar(ffab, bbt);
    } else {
      ffab.hide(new FloatingActionButton.OnVisibilityChangedListener() {
        @Override
        public void onHidden(FloatingActionButton fab) {
          super.onHidden(fab);
          bbt.setVisibility(View.VISIBLE);
        }
      });
    }
  }

  private void animateToHideToolbar(int afterVisibility) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      TransformAnimator.transToFab(ffab, bbt, afterVisibility);
    } else {
      bbt.setVisibility(View.INVISIBLE);
      ffab.show();
    }
  }

  private void updateMenuItemCheckable(boolean checkable) {
    final int menuSize = menu.size();
    pendingUpdate = true;
    for (int i = 0; i < menuSize; i++) {
      menu.getItem(i).setCheckable(checkable);
    }
    pendingUpdate = false;
  }

  void transToFAB(int afterVisibility) {
    animateToHideToolbar(afterVisibility);
    pendingUpdate = true;
    final int menuSize = menu.size();
    for (int i = 0; i < menuSize; i++) {
      menu.getItem(i).setChecked(false);
    }
    updateMenuItemCheckable(false);
    pendingUpdate = false;
    updateMenu();
  }

  private void layoutToolbar() {
    final ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) ffab.getLayoutParams();
    final int height = BottomButtonsToolbar.getHeight(ffab.getContext());
    if (layoutParams instanceof FrameLayout.LayoutParams) {
      final FrameLayout.LayoutParams mlp = new FrameLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, height);
      mlp.gravity = ((FrameLayout.LayoutParams) layoutParams).gravity;
      ((ViewGroup) ffab.getParent()).addView(bbt, mlp);
    } else if (layoutParams instanceof CoordinatorLayout.LayoutParams) {
      final CoordinatorLayout.LayoutParams mlp = new CoordinatorLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, height);
      mlp.gravity = ((CoordinatorLayout.LayoutParams) layoutParams).gravity;
      mlp.dodgeInsetEdges = Gravity.BOTTOM;
      ((ViewGroup) ffab.getParent()).addView(bbt, mlp);
    }
  }

  void hideToolbar() {
    if (bbt.getVisibility() != View.VISIBLE) {
      return;
    }
    bbt.animate()
        .translationY(bbt.getHeight())
        .setInterpolator(new DecelerateInterpolator())
        .setDuration(200)
        .start();
  }

  boolean isToolbarDroppedDown() {
    return bbt != null
        && bbt.getVisibility() == View.VISIBLE
        && bbt.getTranslationY() != 0;
  }

  void showToolbar() {
    bbt.animate()
        .translationY(0)
        .setInterpolator(new AccelerateInterpolator())
        .setDuration(200)
        .start();
  }

  IffabMenu getMenu() {
    return menu;
  }
}
