package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.image.BubbleDetection;
import com.eriqaugustine.ocr.image.ImageTranslator;
import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.utils.FileUtils;

/**
 * Translate a single image (page).
 */
public class ImageTranslationTest {
   public static void main(String[] args) throws Exception {
      String outDirectory = FileUtils.itterationDir("out", "transTest");

      /*
      String[] images = new String[]{"testImages/page.png", "testImages/page2.jpg"};
      String[] images = new String[]{"../jocr-reference/manga/Initial D/Vol 35/raw/File0114.jpg"};
      */
      String[] images = new String[]{"testImages/testSets/youbatoVol1_kana/Yotsubato_v01_022.jpg"};

      String[] fonts = new String[]{"RyuminStd-Heavy-KO"};

      imageTranslateTest(images, fonts, outDirectory);
   }

   public static void imageTranslateTest(String[] images, String[] fonts, String outDirectory) throws Exception {
      ImageTranslator translator = new ImageTranslator(fonts);

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
