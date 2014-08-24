package com.eriqaugustine.ocr;

import com.eriqaugustine.ocr.image.BubbleDetection;
import com.eriqaugustine.ocr.image.CharacterImage;
import com.eriqaugustine.ocr.image.ImageTranslator;
import com.eriqaugustine.ocr.image.TextImage;
import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.math.BinaryConfusionMatrix;
import com.eriqaugustine.ocr.classifier.CharacterClassifier;
import com.eriqaugustine.ocr.classifier.PDCClassifier;
import com.eriqaugustine.ocr.pdc.PDCInfo;
import com.eriqaugustine.ocr.utils.FileUtils;
import com.eriqaugustine.ocr.utils.FontUtils;
import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.Props;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * A general testing driver.
 */
public class Test {
   public static final String HIRAGANA = "あいうえお" +
                                         "かがきぎくぐけげこご" +
                                         "さざしじすずせぜそぞ" +
                                         "ただちぢつづてでとど" +
                                         "なにぬねの" +
                                         "はばぱひびぴふぶぷへべぺほぼぽ" +
                                         "まみむめも" +
                                         "やゆよ" +
                                         "らりるれろ" +
                                         "わゐゑをん";

   public static final String KATAKANA = "アイウエオ" +
                                         "カガキギクグケゲコゴ" +
                                         "サザシジスズセゼソゾ" +
                                         "タダチヂツヅテデトド" +
                                         "ナニヌネノ" +
                                         "ハバパヒビピフブプヘベペホボポ" +
                                         "マミムメモ" +
                                         "ヤユヨ" +
                                         "ラリルレロ" +
                                         "ワヰヱヲン";

   public static void main(String[] args) throws Exception {
      //singleFontGenTest();
      //multiFontGenTest();
      //imageBreakdown();
      //densityComparisonTest();
      //gridBreakupTest();
      //translateTest();
      //volumeFillTest();
      //bubbleTrainingTest();
      //loggingTest();
   }

   public static void loggingTest() throws Exception {
      Logger logger = LogManager.getLogger();

      logger.debug("Test debug");
      logger.info("Test info");
      logger.warn("Test warn");
      logger.error("Test error");
      logger.fatal("Test fatal");

      System.out.println("Final");
   }

   public static void bubbleTrainingTest() throws Exception {
      String outDirectory = FileUtils.itterationDir("out", "bubbleTraining");

      FileUtils.BubbleTrainingSet training =
            FileUtils.loadBubbleTrainingSet("testImages/testSets/bubbleTrainingSet");

      BinaryConfusionMatrix matrix = new BinaryConfusionMatrix();

      int count = 0;
      for (File imgFile : training.trainingFiles) {
         WrapImage bubbles = BubbleDetection.bubbleFillTest(imgFile.getAbsolutePath(),
                                                            training,
                                                            matrix);

         bubbles.write(String.format("%s/%03d-bubbles.png", outDirectory, count));

         count++;
      }

      System.out.println(matrix.fullToString());
   }

   public static void volumeFillTest() throws Exception {
      String outDirectory = FileUtils.itterationDir("out", "volFill");

      // File baseDir = new File("testImages/testSets/youbatoVol1");
      File baseDir = new File("testImages/testSets/youbatoVol1_kana");
      File[] imageFiles = baseDir.listFiles();

      Arrays.sort(imageFiles);

      int count = 0;
      for (File imgFile : imageFiles) {
         WrapImage baseImage = WrapImage.getImageFromFile(imgFile.getAbsolutePath());

         /*
         baseImage.write(String.format("%s/%03d-bubbles.png", outDirectory, 0));
         */

         WrapImage bubbles = BubbleDetection.fillBubbles(baseImage);
         bubbles.write(String.format("%s/%03d-bubbles.png", outDirectory, count));

         count++;
      }
   }

   public static void translateTest() throws Exception {
      String alphabet = HIRAGANA + KATAKANA;

      // String[] fonts = new String[]{"IPAGothic", "RyuminStd-Bold-KO"};
      // String[] fonts = new String[]{"Baekmuk Batang", "RyuminStd-Bold-KO"};
      String[] fonts = new String[]{"IPAGothic", "RyuminStd-Bold-KO", "Baekmuk Batang"};
      String trainingAlphabet = "";
      for (int i = 0; i < fonts.length; i++) {
         trainingAlphabet += alphabet;
      }

      CharacterClassifier classy =
            new PDCClassifier(trainingAlphabet, true, 1, fonts);

      File baseDir = new File("training/kana");
      File[] testFiles = baseDir.listFiles();

      int hits = 0;
      for (int i = 0; i < testFiles.length; i++) {
         File testFile = testFiles[i];

         WrapImage image = WrapImage.getImageFromFile(testFile.getAbsolutePath());

         String actual = "" + testFile.getName().charAt(0);
         String prediction = classy.classify(image);

         System.out.println(String.format("Classify (%03d)[%s]: %s",
                                          i,
                                          actual,
                                          prediction));

         if (prediction.equals(actual)) {
            hits++;
         }
      }

      int count = testFiles.length;
      System.err.println("Hits: " + hits + " / " + count + " (" + ((double)hits / count) + ")");
   }

   public static void gridBreakupTest() {
      String alphabet = " " + HIRAGANA + KATAKANA;

      String outDirectory = FileUtils.itterationDir("out", "gridBreakup");

      // String filename = "testImages/partHiragana.png";
      // String filename = "testImages/2Text.png";
      String filename = "testImages/1Text.png";
      // String filename = "testImages/hiragana.png";

      WrapImage baseImage = WrapImage.getImageFromFile(filename);

      WrapImage[][] gridTextImages = TextImage.gridBreakup(baseImage);
      for (int row = 0; row < gridTextImages.length; row++) {
         for (int col = 0; col < gridTextImages[row].length; col++) {
            WrapImage gridTextImage = gridTextImages[row][col];

            gridTextImage.write(String.format("%s/block-%02d-%02d.png", outDirectory, row, col));

            /*
            String prediction = classy.classify(gridTextImage);
            System.err.println(String.format("(%d, %d): %s", row, col, prediction));
            */
         }
      }
   }

   public static void densityComparisonTest() {
      String alphabet = HIRAGANA + KATAKANA;

      WrapImage baseImage = WrapImage.getImageFromFile("testImages/2Text.png");

      double[][][] fontDensityMaps =
         CharacterImage.getFontDensityMaps(alphabet, 4, 4);

      WrapImage[][] gridTextImages = TextImage.gridBreakup(baseImage);
      for (int row = 0; row < gridTextImages.length; row++) {
         for (int col = 0; col < gridTextImages[row].length; col++) {
            WrapImage gridTextImage = gridTextImages[row][col];

            double[][] densityMap = CharacterImage.getDensityMap(gridTextImage, 4, 4);

            if (densityMap == null) {
               System.out.println(String.format("(%d, %d) -> <space>", row, col));
            } else {
               int guess = bestMatch(fontDensityMaps, densityMap);
               System.out.println(String.format("(%d, %d) -> %c",
                                                row, col, alphabet.charAt(guess)));
            }
         }
      }
   }

   private static int bestMatch(double[][][] haystack, double[][] needle) {
      int bestGuess = 0;
      double bestDistance = Double.MAX_VALUE;

      for (int i = 0; i < haystack.length; i++) {
         double dist = CharacterImage.densityMapDistance(haystack[i], needle);

         if (dist < bestDistance) {
            bestGuess = i;
            bestDistance = dist;
         }
      }

      return bestGuess;
   }

   public static void singleFontGenTest() throws Exception {
      String outDirectory = FileUtils.itterationDir("out", "fontGen");

      WrapImage image = null;

      int current = (int)'あ';
      int count = 0;
      do {
         // System.err.println(count + " -- " + (char)current + " (" + current + ")");

         image = WrapImage.getCharacterImage((char)current, true);
         image.write(String.format("%s/char-%03d-%c.png",
                                   outDirectory,
                                   count,
                                   (char)current));

         count++;
         current++;
      } while ((char)(current - 1) != 'ん');
   }

   public static void multiFontGenTest() {
      String outDirectory = FileUtils.itterationDir("out", "multiFontGen");
      String alphabet = HIRAGANA;

      WrapImage image = null;

      //TEST
      alphabet = "タ";

      for (String fontFamily : FontUtils.FONTS) {
         for (int i = 0; i < alphabet.length(); i++) {
            // System.err.println(count + " -- " + (char)current + " (" + current + ")");
            char character = alphabet.charAt(i);

            image = WrapImage.getCharacterImage(character, true,
                                                Props.getInt("DEFAULT_FONT_SIZE"),
                                                fontFamily);
            image.write(String.format("%s/char-%s-%03d-%c.png",
                                      outDirectory,
                                      fontFamily,
                                      i,
                                      character));
         }
      }
   }
}
