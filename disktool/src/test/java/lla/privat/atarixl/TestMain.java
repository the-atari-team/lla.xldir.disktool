package lla.privat.atarixl;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMain {
  private final static Logger LOGGER = LoggerFactory.getLogger(TestMain.class);
  /*
   * HINT: Wenn dieser Test fehlschlägt, dann weil die class Main umbenannt oder
   * verschoben wurde Dann unbedingt auch die pom.xml anpassen
   * (maven-assembly-plugin artifact plugin)
   */
  @Test
  public void test() throws IOException {
    final Main app = new Main("src/test/resources/lla/privat/atarixl/turbobasic-on-dd.atr");

    Assert.assertEquals("lla.privat.atarixl.Main", app.getClass().getCanonicalName());
  }

  @Test
  public void testPwd() {
    LOGGER.info(getPwd().getAbsolutePath());
  }

  private File getPwd() {
    final File aFile = new File("target/test-classes", this.getClass().getName().replace('.', '/') + ".class");
    return aFile.getParentFile();
  }

}
