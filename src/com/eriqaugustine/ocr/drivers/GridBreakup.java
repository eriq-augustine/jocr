package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.image.TextImage;
import com.eriqaugustine.ocr.utils.FileUtils;
import com.eriqaugustine.ocr.utils.FontUtils;
import com.eriqaugustine.ocr.utils.ImageUtils;

/**
 * Breakup an image.
 */
public class GridBreakup {
   private static final String BASE_OUT_DIR = "out";
   private static final String OUT_PREFIX = "gridBreakup";

   public static void main(String[] args) {
      if (args.length < 1) {
         System.err.println("USAGE: java com.eriqaugustine.ocr.drivers.GridBreakup <image file>");
         return;
      }

      String outDir = FileUtils.itterationDir(BASE_OUT_DIR, OUT_PREFIX);
      System.out.println(outDir);

      FontUtils.registerLocalFonts();

      WrapImage baseImage = WrapImage.getImageFromFile(args[0]);

      WrapImage[][] gridTextImages = TextImage.gridBreakup(baseImage);
      for (int row = 0; row < gridTextImages.length; row++) {
         for (int col = 0; col < gridTextImages[row].length; col++) {
            WrapImage gridTextImage = gridTextImages[row][col];

            // System.out.println(ImageUtils.asciiImage(gridTextImage) + "\n-\n");

            String outFile = outDir + String.format("/%03d_%03d.png", row, col);
            gridTextImage.write(outFile);

            // Clear out the image after it is done.
            gridTextImage.clear();
         }
      }
   }
}
