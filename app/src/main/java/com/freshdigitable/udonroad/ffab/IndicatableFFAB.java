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

package com.freshdigitable.udonroad.ffab;

import android.content.Context;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;

import com.freshdigitable.udonroad.R;

/**
 * IndicatableFFAB is a view group of FlickableFAB and ActionIndicatorView.
 *
 * Created by akihit on 2016/09/05.
 */
public class IndicatableFFAB extends FlickableFAB {
  public static final int MODE_FAB = 0;
  public static final int MODE_TOOLBAR = 1;
  public static final int MODE_SHEET = 2;
  private final IffabMenuPresenter presenter;

  public IndicatableFFAB(Context context) {
    this(context, null);
  }

  public IndicatableFFAB(Context context, AttributeSet attrs) {
    this(context, attrs, R.attr.iffabStyle);
  }

  public IndicatableFFAB(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    presenter = new IffabMenuPresenter(this, attrs, defStyleAttr);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    presenter.onFabAttachedToWindow();
  }

  public Menu getMenu() {
    return presenter.getMenu();
  }

  public void setOnIffabItemSelectedListener(OnIffabItemSelectedListener selectedListener) {
    presenter.getMenu().setOnIffabItemSelectedListener(selectedListener);
  }

  public void clear() {
    presenter.clear();
    setOnIffabItemSelectedListener(null);
  }

  public void transToToolbar() {
    presenter.transToToolbar();
  }

  public void transToFAB() {
    transToFAB(VISIBLE);
  }

  public void transToFAB(int afterVisibility) {
    presenter.transToFAB(afterVisibility);
  }

  public interface OnIffabItemSelectedListener {
    void onItemSelected(@NonNull MenuItem item);
  }

  @Override
  public void show() {
    if (presenter.isFabMode()) {
      super.show();
    } else {
      presenter.showToolbar();
    }
  }

  @Override
  public void hide() {
    if (presenter.isFabMode()) {
      super.hide();
    } else {
      presenter.hideToolbar();
    }
  }

  public int getFabMode() {
    return presenter.getMode();
  }

  @Nullable
  @Override
  protected Parcelable onSaveInstanceState() {
    final Parcelable parcelable = super.onSaveInstanceState();
    final IffabMenuPresenter.SavedState savedState = new IffabMenuPresenter.SavedState(parcelable);
    presenter.onSaveInstanceState(savedState);
    return savedState;
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    final IffabMenuPresenter.SavedState savedState = (IffabMenuPresenter.SavedState) state;
    super.onRestoreInstanceState(savedState.getSuperState());
    presenter.onRestoreInstanceState(savedState);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    clearAnimation();
  }
}
