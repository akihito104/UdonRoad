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
import android.support.annotation.NonNull;
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
  private final IffabMenuPresenter presenter;

  public IndicatableFFAB(Context context) {
    this(context, null);
  }

  public IndicatableFFAB(Context context, AttributeSet attrs) {
    this(context, attrs, R.attr.iffabStyle);
  }

  public IndicatableFFAB(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    presenter = new IffabMenuPresenter(context, attrs, defStyleAttr);
    presenter.initView(this);
    if (isInEditMode()) {
      presenter.setIndicatorVisibility(VISIBLE);
    }
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

  private static final int MODE_FAB = 0;
  private static final int MODE_TOOLBAR = 1;
  private int mode = MODE_FAB;

  public void transToToolbar() {
    presenter.transToToolbar();
    mode = MODE_TOOLBAR;
  }

  public void transToFAB() {
    transToFAB(VISIBLE);
  }

  public void transToFAB(int afterVisibility) {
    presenter.transToFAB(afterVisibility);
    mode = MODE_FAB;
  }

  public interface OnIffabItemSelectedListener {
    void onItemSelected(@NonNull MenuItem item);
  }

  @Override
  public void show() {
    if (mode == MODE_FAB) {
      super.show();
    } else {
      presenter.showToolbar();
    }
  }

  @Override
  public void hide() {
    if (mode == MODE_FAB) {
      super.hide();
    } else {
      presenter.hideToolbar();
    }
  }
}
