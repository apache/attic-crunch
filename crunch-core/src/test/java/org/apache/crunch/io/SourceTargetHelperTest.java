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
package org.apache.crunch.io;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RawLocalFileSystem;
import org.junit.Test;

public class SourceTargetHelperTest {

  @Test
  public void testGetNonexistentPathSize() throws Exception {
    File tmp = File.createTempFile("pathsize", "");
    Path tmpPath = new Path(tmp.getAbsolutePath());
    tmp.delete();
    FileSystem fs = FileSystem.getLocal(new Configuration(false));
    assertEquals(-1L, SourceTargetHelper.getPathSize(fs, tmpPath));
  }

  @Test
  public void testGetNonExistentPathSize_NonExistantPath() throws IOException {
    FileSystem mockFs = new MockFileSystem();
    assertEquals(-1L, SourceTargetHelper.getPathSize(mockFs, new Path("does/not/exist")));
  }

  @Test
  public void testGetPathSize_NoRedundantListStatusCalls() throws IOException {
    final Path parent = new Path("parent");
    final Path childFile = new Path(parent, "childFile");
    final Path childDir = new Path(parent, "childDir");
    final Path grandchildFile = new Path(childDir, "grandchildFile");
    final FileStatus childFileStatus = new FileStatus(1, false, 0, 0, 0, childFile); // file, size = 1
    final FileStatus childDirStatus = new FileStatus(0, true, 0, 0, 0, childDir); // directory
    final FileStatus grandchildFileStatus = new FileStatus(2, false, 0, 0, 0, grandchildFile); // file, size = 2

    final FileSystem fs = mock(FileSystem.class);
    when(fs.globStatus(parent)).thenReturn(new FileStatus[] { childFileStatus, childDirStatus });
    when(fs.listStatus(childDir)).thenReturn(new FileStatus[] { grandchildFileStatus });

    assertEquals(3, SourceTargetHelper.getPathSize(fs, parent));
    verify(fs, times(1)).globStatus(parent);
    verify(fs, times(1)).listStatus(childDir);
    verify(fs, times(1)).globStatus(any(Path.class)); // Fails prior to CRUNCH-683
    verify(fs, times(1)).listStatus(any(Path.class));
  }

  /**
   * Mock FileSystem that returns null for {@link FileSystem#listStatus(Path)}.
   */
  private static class MockFileSystem extends LocalFileSystem {

    private static RawLocalFileSystem createConfiguredRawLocalFileSystem() {
      RawLocalFileSystem fs = new RawLocalFileSystem();
      fs.setConf(new Configuration(false));
      return fs;
    }

    private MockFileSystem() {
      super(createConfiguredRawLocalFileSystem());
    }

    @Override
    public FileStatus[] listStatus(Path f) throws IOException {
      return null;
    }
  }
}
