/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by akihit on 2016/06/15.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityInstTest {

  @Rule
  ActivityTestRule<MainActivity> rule = new ActivityTestRule<>(MainActivity.class);

  @Test
  public void tests() {
  }
}