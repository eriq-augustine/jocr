package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.image.BubbleDetection;
import com.eriqaugustine.ocr.image.ImageTranslator;
import com.eriqaugustine.ocr.utils.FileUtils;

import magick.ImageInfo;
import magick.MagickImage;

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
         ImageInfo info = new ImageInfo(images[i]);
         MagickImage baseImage = new MagickImage(info);
         baseImage.setFileName(outDirectory + "/transTest-" + i + "-0-base.png");
         baseImage.writeImage(new ImageInfo());

         MagickImage bubbles = BubbleDetection.fillBubbles(baseImage);
         bubbles.setFileName(outDirectory + "/transTest-" + i + "-88-base.png");
         bubbles.writeImage(info);

         MagickImage transImage = translator.translate(baseImage);
         transImage.setFileName(outDirectory + "/transTest-" + i + "-99-trans.png");
         transImage.writeImage(new ImageInfo());
      }
   }
}
