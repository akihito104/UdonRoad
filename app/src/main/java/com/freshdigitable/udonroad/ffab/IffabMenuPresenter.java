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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import com.freshdigitable.udonroad.R;

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
  private final TransformAnimator transformAnimator = TransformAnimator.create();

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

      toolbarEnabled = a.getBoolean(R.styleable.IndicatableFFAB_enableToolbar, true);

      if (a.hasValue(R.styleable.IndicatableFFAB_menu)) {
        final int menuRes = a.getResourceId(R.styleable.IndicatableFFAB_menu, 0);
        inflateMenu(context, menuRes);
      }
    } finally {
      a.recycle();
    }
  }

  private void inflateMenu(Context context, int menuRes) {
    pendingUpdate = true;
    final IffabMenuItemInflater menuInflater = new IffabMenuItemInflater(context);
    menuInflater.inflate(menuRes, menu);
    pendingUpdate = false;
    updateMenu();
  }

  void initView(IndicatableFFAB iffab) {
    this.ffab = iffab;
    ffab.setOnFlickListener(new OnFlickListener() {
      private Direction prevSelected = Direction.UNDEFINED;

      @Override
      public void onStart() {
        indicator.onActionLeave(prevSelected);
        prevSelected = Direction.UNDEFINED;
        indicator.setVisibility(View.VISIBLE);
      }

      @Override
      public void onMoving(Direction direction) {
        if (prevSelected == direction) {
          return;
        }
        indicator.onActionLeave(prevSelected);
        if (menu.isVisibleByDirection(direction)) {
          indicator.onActionSelected(direction);
        }
        prevSelected = direction;
      }

      @Override
      public void onFlick(Direction direction) {
        menu.dispatchSelectedMenuItem(direction);
        final Message msg = handler.obtainMessage(
            MSG_DISMISS_ACTION_ITEM_VIEW, View.INVISIBLE, 0, indicator);
        handler.sendMessageDelayed(msg, 200);
      }

      @Override
      public void onCancel() {
        indicator.onActionLeave(prevSelected);
        prevSelected = Direction.UNDEFINED;
        indicator.setVisibility(View.INVISIBLE);
      }
    });

    ViewCompat.setElevation(indicator, ffab.getCompatElevation());

    if (toolbarEnabled) {
      final Message message = handler.obtainMessage(MSG_LAYOUT_BOTTOM_TOOLBAR, this);
      handler.sendMessage(message);
    }
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
    final ViewGroup.LayoutParams baseLp = indicator.generateDefaultLayoutParams();
    final ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) ffab.getLayoutParams();
    if (mlp instanceof FrameLayout.LayoutParams) {
      final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(baseLp);
      lp.gravity = ((FrameLayout.LayoutParams) mlp).gravity;
      setIndicatorMarginFromEdge(lp, lp.gravity);
      ((ViewGroup) ffab.getParent()).addView(indicator, lp);
    } else if (mlp instanceof CoordinatorLayout.LayoutParams) {
      final CoordinatorLayout.LayoutParams lp = new CoordinatorLayout.LayoutParams(baseLp);
      lp.gravity = ((CoordinatorLayout.LayoutParams) mlp).gravity;
      setIndicatorMarginFromEdge(lp, lp.gravity);
      ((ViewGroup) ffab.getParent()).addView(indicator, lp);
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

  private void layoutToolbar() {
    final ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) ffab.getLayoutParams();
    final int height = BottomButtonsToolbar.getHeight(ffab.getContext());
    if (layoutParams instanceof FrameLayout.LayoutParams) {
      final FrameLayout.LayoutParams mlp = new FrameLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, height);
      mlp.gravity = ((FrameLayout.LayoutParams) layoutParams).gravity;
      ((ViewGroup) ffab.getParent()).addView(bottomSheet, mlp);
    } else if (layoutParams instanceof CoordinatorLayout.LayoutParams) {
      final CoordinatorLayout.LayoutParams mlp = new CoordinatorLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      mlp.gravity = ((CoordinatorLayout.LayoutParams) layoutParams).gravity;
      mlp.dodgeInsetEdges = Gravity.BOTTOM;
      ensureBottomSheet((CoordinatorLayout) ffab.getParent());
      ((ViewGroup) ffab.getParent()).addView(bottomSheet, mlp);
    }
  }

  private final boolean toolbarEnabled;
  private View bottomSheet;
  private BottomSheetBehavior<View> bottomSheetBehavior;
  private BottomButtonsToolbar bbt;

  private void ensureBottomSheet(@NonNull CoordinatorLayout parent) {
    ensureBottomSheet(parent.getContext(), parent);
  }

  private void ensureBottomSheet(@NonNull Context context, @Nullable CoordinatorLayout parent) {
    if (!toolbarEnabled || bottomSheet != null) {
      return;
    }
    bottomSheet = LayoutInflater.from(context).inflate(R.layout.view_bottom_sheet, parent, false);
    bottomSheet.setVisibility(View.INVISIBLE);
    bbt = bottomSheet.findViewById(R.id.iffabSheet_button);
    bbt.setMenu(menu);
    bottomSheetBehavior = new BottomSheetBehavior<>();
    bottomSheetBehavior.setPeekHeight(BottomButtonsToolbar.getHeight(ffab.getContext()));
  }

  private static final int MODE_FAB = 0;
  private static final int MODE_TOOLBAR = 1;
  private int mode = MODE_FAB;

  boolean isFabMode() {
    return mode == MODE_FAB;
  }

  void transToToolbar() {
    if (mode == MODE_TOOLBAR) {
      return;
    }
    updateMenuItemCheckable(true);
    if (bottomSheet.getVisibility() != View.VISIBLE) {
      setBottomSheetBehavior(bottomSheetBehavior);
      bottomSheet.setVisibility(View.VISIBLE);
      bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
      transformAnimator.transToToolbar(ffab, bbt);
    }
    mode = MODE_TOOLBAR;
  }

  private void setBottomSheetBehavior(BottomSheetBehavior<View> bottomSheetBehavior) {
    if (ffab.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
      ((CoordinatorLayout.LayoutParams) ffab.getLayoutParams()).setBehavior(bottomSheetBehavior);
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
    if (mode == MODE_FAB) {
      if (afterVisibility == View.VISIBLE) {
        ffab.show();
      } else {
        ffab.hide();
      }
    } else {
      mode = MODE_FAB;
      transformAnimator.transToFab(ffab, bbt, afterVisibility);
      bottomSheet.setVisibility(View.INVISIBLE);
      updateMenuItemCheckable(false);
      updateMenu();
    }
    setBottomSheetBehavior(null);
  }

  void hideToolbar() {
    if (bottomSheet.getVisibility() != View.VISIBLE) {
      return;
    }
    bbt.animate()
        .translationY(bbt.getHeight())
        .setInterpolator(new DecelerateInterpolator())
        .setDuration(200)
        .setListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            bbt.setVisibility(View.INVISIBLE);
            bbt.setTranslationY(0);
          }
        })
        .start();
  }

  void showToolbar() {
    if (bottomSheet.getVisibility() == View.VISIBLE) {
      return;
    }
    bbt.setTranslationY(bbt.getHeight());
    bbt.animate()
        .translationY(0)
        .setInterpolator(new AccelerateInterpolator())
        .setDuration(200)
        .setListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            bbt.setVisibility(View.VISIBLE);
            bbt.setTranslationY(0);
          }
        })
        .start();
  }

  IffabMenu getMenu() {
    return menu;
  }

  void onSaveInstanceState(SavedState savedState) {
    savedState.mode = mode;
  }

  void onRestoreInstanceState(SavedState state) {
    mode = state.mode;
    ensureBottomSheet(ffab.getContext(), null);
    syncState();
  }

  private void syncState() {
    if (mode == MODE_FAB) {
      ffab.setVisibility(View.VISIBLE);
      bottomSheet.setVisibility(View.INVISIBLE);
    } else {
      ffab.setVisibility(View.INVISIBLE);
      bottomSheet.setVisibility(View.VISIBLE);
    }
    updateMenuItemCheckable(mode == MODE_TOOLBAR);
    updateMenu();
  }

  static class SavedState extends View.BaseSavedState {
    int mode;

    SavedState(Parcelable source) {
      super(source);
    }

    private SavedState(Parcel in) {
      super(in);
      mode = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
      super.writeToParcel(out, flags);
      out.writeInt(mode);
    }

    public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {
      @Override
      public SavedState createFromParcel(Parcel parcel) {
        return new SavedState(parcel);
      }

      @Override
      public SavedState[] newArray(int size) {
        return new SavedState[size];
      }
    };
  }
}
