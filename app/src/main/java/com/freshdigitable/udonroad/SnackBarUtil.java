/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.View;

import rx.functions.Action0;

/**
 * Created by akihit on 2016/07/14.
 */
public class SnackbarUtil {

  public static Snackbar create(@NonNull final View root, final String text) {
    return create(root, text, Snackbar.LENGTH_SHORT);
  }

  public static Snackbar create(@NonNull View root, String text, int length) {
    return Snackbar.make(root, text, length);
  }

  public static void show(@NonNull final View root, final String text) {
    create(root, text).show();
  }

  public static Action0 action(@NonNull final View root, final String text) {
    return new Action0() {
      @Override
      public void call() {
        show(root, text);
      }
    };
  }

  private SnackbarUtil() {
  }
}
