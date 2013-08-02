package com.eriqaugustine.ocr;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Just some general utils.
 *
 * TODO(eriq): This assumes that all paths are on linux machines.
 *  Fix paths to be cross-platform.
 */
public class Util {
   /**
    * Create the "next" directory and return the path.
    * This is meant for when a series of tests are run.
    * Each call will yield a new unique directory.
    */
   public static String itterationDir(String baseDir, final String prefix) {
      File base = new File(baseDir);

      assert(base.exists());
      assert(base.isDirectory());

      String[] filenames = base.list(new FilenameFilter() {
         public boolean accept(File dir, String name) {
            File tempFile = new File(dir, name);
            return name.startsWith(prefix) && tempFile.isDirectory();
         }
      });

      int max = -1;
      for (String filename : filenames) {
         int val = Integer.parseInt(filename.substring(prefix.length() + 1));

         if (val > max) {
            max = val;
         }
      }

      File child = new File(base, String.format("%s-%03d", prefix, max + 1));
      child.mkdir();

      return child.getAbsolutePath();
   }

   public static String itterationDir(String baseDir) {
      return itterationDir(baseDir, "");
   }
}
