package lla.privat.atarixl;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ITFilesystemCheck {

  private int breakpoint;

  @After
  public void tearDown() {
    breakpoint = 1;
    Assert.assertEquals(1, breakpoint);
  }

  @Ignore
  @Test
  public void testCheckDosEd() throws IOException {
    FilesystemCheck fsck;

    fsck = new FilesystemCheck("src/test/resources/lla/privat/atarixl/dos-ed.atr");
    fsck.checkDirectoryEntries();
    Assert.assertEquals(0, fsck.getCountOfErrors());
    Assert.assertEquals(2, fsck.getCountOfWarnings());
  }


}
