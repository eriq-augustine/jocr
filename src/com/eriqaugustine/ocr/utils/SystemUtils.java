package com.eriqaugustine.ocr.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintStream;
import java.util.Date;

public class SystemUtils {
   private static Logger logger = LogManager.getLogger(SystemUtils.class.getName());

   private static final int bytesToMB = 1024 * 1024;

   public static void memoryMark() {
      memoryMark("", System.err);
   }

   public static void memoryMark(String mark, PrintStream outStream) {
      outStream.println(memoryMarkString(mark));
   }

   public static String memoryMarkString(String mark) {
      String rtn = "";

      long time = System.currentTimeMillis();
      Runtime runtime = Runtime.getRuntime();

      long totalMemory = runtime.totalMemory() / bytesToMB;
      long freeMemory = runtime.freeMemory() / bytesToMB;

      rtn += "Memory Mark (" + mark + "): " + time + " (" + (new Date(time)) + ")\n";

      rtn += "   Used Memory: " + (totalMemory - freeMemory) + " MB\n";
      rtn += "   Free Memory: " + freeMemory + " MB\n";
      rtn += "   Available Memory: " + totalMemory + " MB\n";

      return rtn;
   }
}
