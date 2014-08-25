package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.image.BubbleDetection;
import com.eriqaugustine.ocr.image.ImageTranslator;
import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.utils.FileUtils;

import com.eriqaugustine.ocr.classifier.OCRClassifier;
import com.eriqaugustine.ocr.classifier.PLOVEClassifier;
import com.eriqaugustine.ocr.classifier.reduce.FeatureVectorReducer;
import com.eriqaugustine.ocr.classifier.reduce.KLTReducer;

import com.eriqaugustine.ocr.plove.PLOVE;

import com.eriqaugustine.ocr.utils.Props;

/**
 * Translate a single image (page).
 */
public class ImageTranslationTest {
   public static void main(String[] args) throws Exception {
      String outDirectory = FileUtils.itterationDir("out", "transTest");

      String[] images = new String[]{"testImages/testSets/youbatoVol1_kana/Yotsubato_v01_022.jpg"};
      String[] fonts = Props.getList("CLASSIFIER_TRAINING_FONTS").toArray(new String[0]);
      String trainingCharacters = Props.getString("KYOIKU_FULL") + Props.getString("KANA_FULL");

      imageTranslateTest(images, fonts, trainingCharacters, outDirectory);
   }

   public static void imageTranslateTest(String[] images, String[] fonts,
                                         String trainingCharacters, String outDirectory) throws Exception {
      System.err.println("Begin");
      long time = System.currentTimeMillis();

      FeatureVectorReducer reduce = new KLTReducer(PLOVE.getNumberOfFeatures(), 650);
      OCRClassifier classy = new PLOVEClassifier(trainingCharacters, fonts, reduce);

      System.err.println("Training finished (" + (System.currentTimeMillis() - time) + ")");
      time = System.currentTimeMillis();

      ImageTranslator translator = new ImageTranslator(classy);

      System.err.println("Translation finished (" + (System.currentTimeMillis() - time) + ")");

      for (int i = 0; i < images.length; i++) {
         WrapImage baseImage = WrapImage.getImageFromFile(images[i]);
         baseImage.write(outDirectory + "/transTest-" + i + "-0-base.png");

         WrapImage bubbles = BubbleDetection.fillBubbles(baseImage);
         bubbles.write(outDirectory + "/transTest-" + i + "-88-base.png");

         WrapImage transImage = translator.translate(baseImage);
         transImage.write(outDirectory + "/transTest-" + i + "-99-trans.png");
      }
   }
}
