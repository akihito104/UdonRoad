package com.freshdigitable.udonroad.util;

import android.support.test.InstrumentationRegistry;

import com.freshdigitable.udonroad.MockAppComponent;
import com.freshdigitable.udonroad.MockMainApplication;

/**
 * Created by akihit on 2017/04/20.
 */

public class TestInjectionUtil {
  public static MockAppComponent getComponent() {
    MockMainApplication app
        = (MockMainApplication) InstrumentationRegistry.getTargetContext().getApplicationContext();
    return (MockAppComponent) app.getAppComponent();
  }
}
