package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.classifier.CharacterClassifier;

import com.eriqaugustine.ocr.image.CharacterImage;
import com.eriqaugustine.ocr.image.TextImage;
import com.eriqaugustine.ocr.image.WrapImage;

import com.eriqaugustine.ocr.utils.FontUtils;
import com.eriqaugustine.ocr.utils.ImageUtils;

import com.eriqaugustine.ocr.utils.Props;

/**
 * The base to for a quick classifier spot check.
 * This will handle most of the setup, it just needs a constructed classifier.
 */
public abstract class KanjiClassifierTest {
   // Suggested training set.
   protected final String trainingCharacters;

   protected KanjiClassifierTest() {
      // trainingCharacters = Props.getString("KYOIKU_FIRST_GRADE");
      trainingCharacters = Props.getString("KYOIKU_FULL");
   }

   protected void classifierTest(CharacterClassifier classy) throws Exception {
      classifierTest(classy, false);
   }

   protected void classifierTest(CharacterClassifier classy, boolean verbose) throws Exception {
      FontUtils.registerLocalFonts();

      // Not exactly hiragana.
      String characters =
         "空車見山白男上日一" +
         "糸名立川赤女下月二" +
         "字正休花青子右火三" +
         "夕音天雨学人左水四" +
         "玉早気田校口大木五" +
         "文王本石先目中金六" +
         "力村犬貝生手小土七" +
         "円町虫林年耳入十八" +
         "右竹草森千足出百九";

      WrapImage baseImage = WrapImage.getImageFromFile("testImages/KyoikuKanji/FirstGrade.png");

      int count = 0;
      int hits = 0;

      WrapImage[][] gridTextImages = TextImage.gridBreakup(baseImage);
      for (int row = 0; row < gridTextImages.length; row++) {
         for (int col = 0; col < gridTextImages[row].length; col++) {
            WrapImage gridTextImage = gridTextImages[row][col];

            // System.out.println(ImageUtils.asciiImage(gridTextImage) + "\n-\n");

            String prediction = classy.classify(gridTextImage);

            if (verbose) {
               System.out.println(String.format("Classify (%d, %d)[%s]: {%s}",
                                                row, col,
                                                "" + characters.charAt(count),
                                                prediction));
            }

            if (prediction.equals("" + characters.charAt(count))) {
               hits++;
            }

            count++;
         }
      }

      System.err.println("Hits: " + hits + " / " + count + " (" + ((double)hits / count) + ")");
   }
}
