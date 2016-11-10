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

package com.freshdigitable.udonroad.module.realm;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import twitter4j.ExtendedMediaEntity;
import twitter4j.MediaEntity;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ExtendedMediaEntityRealmTest tests ExtendedMediaEntityRealm.
 *
 * Created by akihit on 2016/10/30.
 */
@RunWith(AndroidJUnit4.class)
public class ExtendedMediaEntityRealmTest {

  private ExtendedMediaEntity extendedMediaEntity;

  @Before
  public void setup() {
    extendedMediaEntity = mock(ExtendedMediaEntity.class);
    Map<Integer, MediaEntity.Size> sizes = new HashMap<>();
    sizes.put(0, createSize(150, 150, 101));
    sizes.put(1, createSize(680, 510, 100));
    sizes.put(2, createSize(1024, 768, 100));
    sizes.put(3, createSize(1024, 768, 100));
    when(extendedMediaEntity.getSizes()).thenReturn(sizes);
    when(extendedMediaEntity.getVideoVariants()).thenReturn(new ExtendedMediaEntity.Variant[0]);
    assertThat(sizes.size(), is(4));
  }

  @Test
  public void parseSizes() {
    final ExtendedMediaEntityRealm sut = new ExtendedMediaEntityRealm(extendedMediaEntity);
    final Map<Integer, MediaEntity.Size> actual = sut.getSizes();
    assertThatSizesIsSame(actual);
  }

  @Test
  public void parseSizesAfterFind() {
    // setup
    final RealmConfiguration conf = new RealmConfiguration.Builder()
        .deleteRealmIfMigrationNeeded()
        .build();
    final Realm realm = Realm.getInstance(conf);
    try {
      final ExtendedMediaEntityRealm sut = new ExtendedMediaEntityRealm(extendedMediaEntity);
      realm.executeTransaction(new Realm.Transaction() {
        @Override
        public void execute(Realm realm) {
          realm.insertOrUpdate(sut);
        }
      });
      // exec.
      final ExtendedMediaEntityRealm actual = realm.where(ExtendedMediaEntityRealm.class)
          .findFirst();
      final Map<Integer, MediaEntity.Size> actualSize = actual.getSizes();
      // assert
      assertThatSizesIsSame(actualSize);
    } finally {
      // tear down
      realm.executeTransaction(new Realm.Transaction() {
        @Override
        public void execute(Realm realm) {
          realm.deleteAll();
        }
      });
      realm.close();
      Realm.deleteRealm(conf);
    }
  }

  private void assertThatSizesIsSame(Map<Integer, MediaEntity.Size> actualSize) {
    Map<Integer, MediaEntity.Size> sizes = extendedMediaEntity.getSizes();
    assertThat(actualSize.size(), is(sizes.size()));
    for (Map.Entry<Integer, MediaEntity.Size> s : sizes.entrySet()) {
      final MediaEntity.Size as = actualSize.get(s.getKey());
      assertThat(as.getHeight(), is(s.getValue().getHeight()));
      assertThat(as.getWidth(), is(s.getValue().getWidth()));
      assertThat(as.getResize(), is(s.getValue().getResize()));
    }
  }

  private static MediaEntity.Size createSize(int h, int w, int resize) {
    final MediaEntity.Size mock = mock(MediaEntity.Size.class);
    when(mock.getHeight()).thenReturn(h);
    when(mock.getWidth()).thenReturn(w);
    when(mock.getResize()).thenReturn(resize);
    return mock;
  }
}
