package com.eriqaugustine.ocr.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Point;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Just some general utils for working with files.
 */
public class FileUtils {
   private static Logger logger = LogManager.getLogger(FileUtils.class.getName());

   private static final String BUBBLE_TRAINING_IMAGES_DIR = "images";
   private static final String BUBBLE_TRAINING_DATA_FILE = "bubbles.txt";

   public static String getBasename(String path) {
      return getBasename(path, true);
   }

   /**
    * Get the basename for a path.
    */
   public static String getBasename(String path, boolean includeExtension) {
      if (includeExtension) {
         return new File(path).getName();
      }

      return new File(path).getName().replaceFirst("\\.[^\\.]*$", "");
   }

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

   public static BubbleTrainingSet loadBubbleTrainingSet(String baseDir) {
      File base = new File(baseDir + File.separator + BUBBLE_TRAINING_IMAGES_DIR);
      assert(base.exists() && base.isDirectory());

      File[] imageFiles = base.listFiles();
      Arrays.sort(imageFiles);
      BubbleTrainingSet training = new BubbleTrainingSet(imageFiles);

      File trainingDataFile = new File(baseDir + File.separator + BUBBLE_TRAINING_DATA_FILE);
      assert(trainingDataFile.exists() && trainingDataFile.isFile());

      try {
         Scanner fileScanner = new Scanner(trainingDataFile);
         // Eat the header line.
         fileScanner.nextLine();

         while (fileScanner.hasNextLine()) {
            String[] attributes = fileScanner.nextLine().trim().split(";");
            training.addBubble(attributes[0],
                               new Point[]{new Point(Integer.parseInt(attributes[1]),
                                                     Integer.parseInt(attributes[2])),
                                           new Point(Integer.parseInt(attributes[3]),
                                                     Integer.parseInt(attributes[4]))});
         }
      } catch (Exception ex) {
         logger.fatal("Bubble training directory file is not formatted correctly.", ex);
      }

      return training;
   }

   /**
    * Just a simple wrapper for information about the image files to test
    * and the bubble bounding boxes.
    */
   public static class BubbleTrainingSet {
      public List<File> trainingFiles;

      /**
       * The bounding boxes.
       * {filename : [[Upper Left, Lower Right], ...]}
       */
      public Map<String, List<Point[]>> trainingBubbles;

      public BubbleTrainingSet(File[] files) {
         trainingFiles = Arrays.asList(files);
         trainingBubbles = new HashMap<String, List<Point[]>>();
      }

      public void addBubble(String filename, Point[] bounds) {
         if (!trainingBubbles.containsKey(filename)) {
            trainingBubbles.put(filename, new ArrayList<Point[]>());
         }

         trainingBubbles.get(filename).add(bounds);
      }
   }
}
