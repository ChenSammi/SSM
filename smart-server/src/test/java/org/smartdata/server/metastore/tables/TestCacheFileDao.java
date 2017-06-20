/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartdata.server.metastore.tables;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.smartdata.common.metastore.CachedFileStatus;
import org.smartdata.metrics.FileAccessEvent;
import org.smartdata.server.metastore.TestDaoUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestCacheFileDao extends TestDaoUtil {

  private CacheFileDao cacheFileDao;

  @Before
  public void initCacheFileDao() throws Exception {
    initDao();
    cacheFileDao = new CacheFileDao(druidPool.getDataSource());
  }

  @After
  public void closeCacheFileDao() throws Exception {
    closeDao();
    cacheFileDao = null;
  }

  @Test
  public void testUpdateCachedFiles() throws Exception {
    cacheFileDao.insert(80L,
        "testPath", 1000L, 2000L, 100);
    cacheFileDao.insert(new CachedFileStatus(90L,
        "testPath2", 2000L, 3000L, 200));
    Map<String, Long> pathToId = new HashMap<>();
    pathToId.put("testPath", 80L);
    pathToId.put("testPath2", 90L);
    pathToId.put("testPath3", 100L);
    List<FileAccessEvent> events = new ArrayList<>();
    events.add(new FileAccessEvent("testPath", 3000L));
    events.add(new FileAccessEvent("testPath", 4000L));
    events.add(new FileAccessEvent("testPath2", 4000L));
    events.add(new FileAccessEvent("testPath2", 5000L));
    events.add(new FileAccessEvent("testPath3", 8000L));
    events.add(new FileAccessEvent("testPath3", 9000L));
    cacheFileDao.update(pathToId, events);
    List<CachedFileStatus> statuses = cacheFileDao.getAll();
    Assert.assertTrue(statuses.size() == 2);
    Map<Long, CachedFileStatus> statusMap = new HashMap<>();
    for (CachedFileStatus status : statuses) {
      statusMap.put(status.getFid(), status);
    }
    Assert.assertTrue(statusMap.containsKey(80L));
    CachedFileStatus first = statusMap.get(80L);
    Assert.assertTrue(first.getLastAccessTime() == 4000L);
    Assert.assertTrue(first.getNumAccessed() == 102);
    Assert.assertTrue(statusMap.containsKey(90L));
    CachedFileStatus second = statusMap.get(90L);
    Assert.assertTrue(second.getLastAccessTime() == 5000L);
    Assert.assertTrue(second.getNumAccessed() == 202);
  }

  @Test
  public void testInsertDeleteCachedFiles() throws Exception {
    cacheFileDao
        .insert(80l,
            "testPath", 123456l, 234567l, 456);
    Assert.assertTrue(cacheFileDao.getById(
        80l).getFromTime() == 123456l);
    // Update record with 80l id
    cacheFileDao.update(80l,
        123455l, 460);
    Assert.assertTrue(cacheFileDao
                          .getAll().get(0)
                          .getLastAccessTime() == 123455l);
    CachedFileStatus[] cachedFileStatuses = new CachedFileStatus[] {
        new CachedFileStatus(321l, "testPath",
                                113334l, 222222l, 222)};
    cacheFileDao.insert(cachedFileStatuses);
    Assert.assertTrue(cacheFileDao.getById(321l)
                          .getNumAccessed() == 222);
    Assert.assertTrue(cacheFileDao.getAll().size() == 2);
    // Delete one record
    cacheFileDao.deleteById(321l);
    Assert.assertTrue(cacheFileDao.getAll().size() == 1);
    // Clear all records
    cacheFileDao.deleteAll();
    Assert.assertTrue(cacheFileDao.getAll().size() == 0);
  }

  @Test
  public void testGetCachedFileStatus() throws Exception {
    cacheFileDao.insert(6l, "testPath", 1490918400000l,
        234567l, 456);
    cacheFileDao.insert(19l, "testPath", 1490918400000l,
        234567l, 456);
    cacheFileDao.insert(23l, "testPath", 1490918400000l,
        234567l, 456);
    CachedFileStatus cachedFileStatus = cacheFileDao.getById(6);
    Assert.assertTrue(cachedFileStatus.getFromTime() == 1490918400000l);
    List<CachedFileStatus> cachedFileList = cacheFileDao.getAll();
    List<Long> fids = cacheFileDao.getFids();
    Assert.assertTrue(fids.size() == 3);
    Assert.assertTrue(cachedFileList.get(0).getFid() == 6);
    Assert.assertTrue(cachedFileList.get(1).getFid() == 19);
    Assert.assertTrue(cachedFileList.get(2).getFid() == 23);
  }
}
