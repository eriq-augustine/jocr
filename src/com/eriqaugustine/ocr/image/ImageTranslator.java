package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.pdc.PDCClassifier;
import com.eriqaugustine.ocr.translate.Translator;
import com.eriqaugustine.ocr.utils.ImageUtils;

import magick.MagickImage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Translate and replace the text in an image.
 */
public class ImageTranslator {
   private static Logger logger = LogManager.getLogger(ImageTranslator.class.getName());

   private static final String[] FONTS = new String[]{"Baekmuk Batang", "RyuminStd-Bold-KO"};

   // TODO(eriq): Move the training set generation to a single location.
   private static final String ALPHABET = com.eriqaugustine.ocr.Test.HIRAGANA +
                                          com.eriqaugustine.ocr.Test.KATAKANA;

   private PDCClassifier classy;
   private Translator trans;

   public ImageTranslator() throws Exception {
      String trainingAlphabet = "";
      for (int i = 0; i < FONTS.length; i++) {
         trainingAlphabet += ALPHABET;
      }

      classy = new PDCClassifier(CharacterImage.generateFontImages(ALPHABET, FONTS),
                                 trainingAlphabet, true, 1);
      trans = new Translator("ja", "en");
   }

   public MagickImage translate(MagickImage baseImage) throws Exception {
      BubbleDetection.BubbleInfo[] bubbles = BubbleDetection.extractBubblesWithInfo(baseImage);

      for (BubbleDetection.BubbleInfo bubble : bubbles) {
         String text = "";

         List<MagickImage> characterImages = TextImage.characterBreakup(bubble.image);
         for (MagickImage image : characterImages) {
            text += classy.classify(image);
         }

         logger.debug(text.trim());

         String translation = trans.translate(text.trim());

         MagickImage transBubble = ImageUtils.generateString(translation, false,
                                                             bubble.width, bubble.height);
         baseImage = ImageUtils.overlayImage(baseImage, transBubble,
                                             bubble.startRow, bubble.startCol);
      }

      return baseImage;
   }
}
