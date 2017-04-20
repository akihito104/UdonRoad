package com.freshdigitable.udonroad.util;

import android.support.test.espresso.core.deps.guava.io.PatternFilenameFilter;

import io.realm.Realm;
import io.realm.RealmConfiguration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by akihit on 2017/04/20.
 */

public class StorageUtil {
  public static void initStorage() {
    for (String l : listStorage()) {
      final RealmConfiguration config = new RealmConfiguration.Builder()
          .name(l)
          .deleteRealmIfMigrationNeeded()
          .build();
      final int globalInstanceCount = Realm.getGlobalInstanceCount(config);
      if (globalInstanceCount > 0) {
        final Realm realm = Realm.getInstance(config);
        realm.executeTransaction(r -> r.deleteAll());
        realm.close();
      } else {
        final boolean b = Realm.deleteRealm(config);
        assertTrue(b);
      }
    }
  }

  private static String[] listStorage() {
    final Realm realm = Realm.getDefaultInstance();
    final String[] list = realm.getConfiguration().getRealmDirectory()
        .list(new PatternFilenameFilter("^.*\\.management$"));
    realm.close();
    for (int i = 0; i < list.length; i++) {
      list[i] = list[i].replace(".management", "");
    }
    return list;
  }

  public static void checkAllRealmInstanceCleared() {  // XXX
    for (String l : listStorage()) {
      checkRealmInstanceCount(l, 0);
    }
  }

  private static void checkRealmInstanceCount(String name, int count) {  // XXX
    final RealmConfiguration conf = new RealmConfiguration.Builder().name(name).build();
    assertThat("local instance count: " + name, Realm.getLocalInstanceCount(conf), is(count));
    assertThat("global instance count: " + name, Realm.getGlobalInstanceCount(conf), is(count));
  }

  private StorageUtil() {}
}
