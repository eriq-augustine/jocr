package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.classifier.OCRClassifier;

import com.eriqaugustine.ocr.image.CharacterImage;
import com.eriqaugustine.ocr.image.TextImage;
import com.eriqaugustine.ocr.image.WrapImage;

import com.eriqaugustine.ocr.utils.FontUtils;
import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.SystemUtils;

import com.eriqaugustine.ocr.utils.Props;

/**
 * The base to for a quick classifier spot check.
 * This will handle most of the setup, it just needs a constructed classifier.
 */
public abstract class ClassifierTest {
   // Suggested training set.
   protected final String trainingCharacters;

   protected ClassifierTest() {
      trainingCharacters = Props.getString("HIRAGANA");
   }

   protected double classifierTest(OCRClassifier classy) throws Exception {
      return classifierTest(classy, false);
   }

   protected double classifierTest(OCRClassifier classy, boolean verbose) throws Exception {
      FontUtils.registerLocalFonts();

      // Not exactly hiragana.
      String characters = "あいうえおかきくけこさしすせそたちつてとなにぬねの" +
                          "はひふへほまみむめもやわゆんよらりるれろ";

      WrapImage baseImage = WrapImage.getImageFromFile("testImages/partHiragana.png");

      int count = 0;
      int hits = 0;

      WrapImage[][] gridTextImages = TextImage.gridBreakup(baseImage);

      SystemUtils.memoryMark("Test BEGIN", System.err);

      for (int row = 0; row < gridTextImages.length; row++) {
         for (int col = 0; col < gridTextImages[row].length; col++) {
            WrapImage gridTextImage = gridTextImages[row][col];

            // System.out.println(ImageUtils.asciiImage(gridTextImage) + "\n-\n");

            String prediction = classy.classify(gridTextImage);

            if (verbose) {
               System.out.print(String.format("Classify (%d, %d)[%s]: {%s}",
                                              row, col,
                                              "" + characters.charAt(count),
                                              prediction));

               if (!prediction.equals("" + characters.charAt(count))) {
                  System.out.println(" X");
               } else {
                  System.out.println();
               }
            }

            if (prediction.equals("" + characters.charAt(count))) {
               hits++;
            }

            count++;
         }
      }

      SystemUtils.memoryMark("Test END", System.err);

      double accuracy = (double)hits / count;
      System.err.println("Hits: " + hits + " / " + count + " (" + accuracy + ")");

      return accuracy;
   }
}
