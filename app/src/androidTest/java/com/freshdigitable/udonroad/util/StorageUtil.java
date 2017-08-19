package com.freshdigitable.udonroad.util;

import android.support.test.espresso.core.deps.guava.io.PatternFilenameFilter;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;

import static org.hamcrest.Matchers.is;
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
    final RealmConfiguration conf = new RealmConfiguration.Builder().build();
    final String[] list = conf.getRealmDirectory()
        .list(new PatternFilenameFilter("^.*\\.management$"));
    for (int i = 0; i < list.length; i++) {
      list[i] = list[i].replace(".management", "");
    }
    return list;
  }

  public static void checkAllRealmInstanceCleared() {  // XXX
    final ArrayList<InstanceCount> realms = new ArrayList<>();
    for (String l : listStorage()) {
      final InstanceCount ic = checkRealmInstanceCount(l);
      realms.add(ic);
    }
    final String countResult = parseCountResult(realms);
    for (InstanceCount r : realms) {
      assertThat(countResult, r.localCount, is(0));
      assertThat(countResult, r.globalCount, is(0));
    }
  }

  private static String parseCountResult(List<InstanceCount> instanceCounts) {
    final StringBuilder stringBuilder = new StringBuilder();
    for (InstanceCount ic : instanceCounts) {
      stringBuilder.append(ic.toString()).append("\n");
    }
    return stringBuilder.toString();
  }

  private static InstanceCount checkRealmInstanceCount(String name) {  // XXX
    final RealmConfiguration conf = new RealmConfiguration.Builder().name(name).build();
    return new InstanceCount(name, Realm.getLocalInstanceCount(conf), Realm.getGlobalInstanceCount(conf));
  }

  private static class InstanceCount {
    private final String name;
    private final int localCount;
    private final int globalCount;

    InstanceCount(String name, int localCount, int globalCount) {
      this.name = name;
      this.localCount = localCount;
      this.globalCount = globalCount;
    }

    @Override
    public String toString() {
      return "name: " + name + ", local: " + localCount + ", global: " + globalCount;
    }
  }

  private StorageUtil() {}
}
