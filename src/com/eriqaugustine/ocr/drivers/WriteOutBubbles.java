package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.image.BubbleDetection;
import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.utils.FileUtils;
import com.eriqaugustine.ocr.utils.Props;

/**
 * Detect the bubbles in the given files (args) and write wach bubbles to seperate file.
 */
public class WriteOutBubbles {
   public static void main(String[] args) throws Exception {
      String outDirectory = FileUtils.itterationDir(Props.getString("OUT_DIR"), "bubbleExtract");
      System.out.println(outDirectory);

      for (String filename : args) {
         extractWriteImage(outDirectory, filename);
      }
   }

   public static void extractWriteImage(String outDir, String path) throws Exception {
      String basename = FileUtils.getBasename(path, false /* no extension */);

      WrapImage image = WrapImage.getImageFromFile(path);

      WrapImage[] bubbles = BubbleDetection.extractBubbles(image);

      for (int i = 0; i < bubbles.length; i++) {
         bubbles[i].write(String.format("%s/%s-%02d.png", outDir, basename, i));
      }
   }
}
