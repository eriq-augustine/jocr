package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.classifier.CharacterClassifier;
import com.eriqaugustine.ocr.classifier.PDCClassifier;

import com.eriqaugustine.ocr.image.CharacterImage;
import com.eriqaugustine.ocr.image.TextImage;
import com.eriqaugustine.ocr.image.WrapImage;

import com.eriqaugustine.ocr.utils.FontUtils;
import com.eriqaugustine.ocr.utils.ImageUtils;

import com.eriqaugustine.ocr.utils.Props;

/**
 * A test to see how PDC is doing on a simple test set.
 */
public class PDCTest {
   public static void main(String[] args) throws Exception {
      pdcTest();
   }

   public static void pdcTest() throws Exception {
      FontUtils.registerLocalFonts();

      String alphabet = Props.getString("HIRAGANA");

      CharacterClassifier classy = new PDCClassifier(CharacterImage.generateFontImages(alphabet),
                                                     //  alphabet, false, 1);
                                                     alphabet, true, 1,
                                                     new String[]{Props.getString("DEFAULT_FONT_FAMILY")});

      // Not exactly hiragana.
      String characters = "あいうえおかきくけこさしすせそたちつてとなにぬねの" +
                          "はひふへほまみむめもやわゆんよらりるれろ";

      WrapImage baseImage = WrapImage.getImageFromFile("testImages/partHiragana.png");

      int count = 0;
      int hits = 0;

      WrapImage[][] gridTextImages = TextImage.gridBreakup(baseImage);
      for (int row = 0; row < gridTextImages.length; row++) {
         for (int col = 0; col < gridTextImages[row].length; col++) {
            WrapImage gridTextImage = gridTextImages[row][col];

            // System.out.println(ImageUtils.asciiImage(gridTextImage) + "\n-\n");

            String prediction = classy.classify(gridTextImage);
            System.out.println(String.format("Classify (%d, %d)[%s]: {%s}",
                                             row, col,
                                             "" + characters.charAt(count),
                                             prediction));

            if (prediction.equals("" + characters.charAt(count))) {
               hits++;
            }

            count++;
         }
      }

      System.err.println("Hits: " + hits + " / " + count + " (" + ((double)hits / count) + ")");
   }
}
