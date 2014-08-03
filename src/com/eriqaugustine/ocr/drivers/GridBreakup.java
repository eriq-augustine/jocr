package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.image.TextImage;
import com.eriqaugustine.ocr.utils.FontUtils;
import com.eriqaugustine.ocr.utils.ImageUtils;

/**
 * Breakup an image.
 */
public class GridBreakup {
   public static void main(String[] args) {
      FontUtils.registerLocalFonts();

      WrapImage baseImage = WrapImage.getImageFromFile("testImages/KyoikuKanji/Kyoiku_BW.png");

      WrapImage[][] gridTextImages = TextImage.gridBreakup(baseImage);
      for (int row = 0; row < gridTextImages.length; row++) {
         for (int col = 0; col < gridTextImages[row].length; col++) {
            WrapImage gridTextImage = gridTextImages[row][col];

            // TEST
            System.out.println(ImageUtils.asciiImage(gridTextImage) + "\n-\n");
         }
      }
   }
}
