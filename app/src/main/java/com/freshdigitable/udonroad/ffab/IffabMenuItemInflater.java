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
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.MenuInflater;

import com.freshdigitable.udonroad.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Created by akihit on 2017/03/13.
 */

class IffabMenuItemInflater {
  private static final String TAG = IffabMenuItemInflater.class.getSimpleName();
  private final Context context;
  private final MenuInflater inflater;

  IffabMenuItemInflater(Context context) {
    this.context = context;
    this.inflater = new MenuInflater(context);
  }

  void inflate(int menuRes, IffabMenu menu) {
    inflater.inflate(menuRes, menu);
    XmlResourceParser parser = null;
    try {
      parser = context.getResources().getLayout(menuRes);
      final AttributeSet attributeSet = Xml.asAttributeSet(parser);
      parseMenu(parser, attributeSet, menu);
    } catch (XmlPullParserException | IOException e) {
      e.printStackTrace();// XXX
    } finally {
      if (parser != null) {
        parser.close();
      }
    }
  }

  private void parseMenu(XmlResourceParser parser, AttributeSet attributeSet, IffabMenu menu)
      throws XmlPullParserException, IOException {
    int eventType = parser.getEventType();
    String tag;
    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_TAG) {
        tag = parser.getName();
        if ("menu".equals(tag)) {
          eventType = parser.next();
          break;
        }
      }
      eventType = parser.next();
    }

    while (eventType != XmlPullParser.END_DOCUMENT) {
      tag = parser.getName();
      if (eventType != XmlPullParser.START_TAG || !"item".equals(tag)) {
        eventType = parser.next();
        continue;
      }
      final int resourceId = findId(parser);
      if (resourceId > -1) {
        final TypedArray ta = context.obtainStyledAttributes(attributeSet, R.styleable.IndicatableFFAB);
        final ColorStateList colorStateList = ta.getColorStateList(
            R.styleable.IndicatableFFAB_toolbarIconColorState);
        menu.findItem(resourceId).setColorState(colorStateList);
        ta.recycle();
      }
      eventType = parser.next();
    }
  }

  private int findId(XmlPullParser parser) {
    final int attributeCount = parser.getAttributeCount();
    for (int i = 0; i < attributeCount; i++) {
      final String attributeName = parser.getAttributeName(i);
      if ("id".equals(attributeName)) {
        final String attributeValue = parser.getAttributeValue(i);
        Log.d(TAG, "findId: " + attributeValue);
        return Integer.parseInt(attributeValue.substring(1));
      }
    }
    return -1;
  }

}
