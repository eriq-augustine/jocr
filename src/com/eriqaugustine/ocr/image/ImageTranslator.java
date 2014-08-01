package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.classifier.CharacterClassifier;
import com.eriqaugustine.ocr.classifier.PDCClassifier;
import com.eriqaugustine.ocr.translate.Translator;
import com.eriqaugustine.ocr.utils.ImageUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Translate and replace the text in an image.
 */
public class ImageTranslator {
   private static Logger logger = LogManager.getLogger(ImageTranslator.class.getName());

   private static final String[] FONTS = new String[]{"Baekmuk Batang", "RyuminStd-Bold-KO"};

   private static final String ALPHABET = com.eriqaugustine.ocr.Test.HIRAGANA +
                                          com.eriqaugustine.ocr.Test.KATAKANA;

   private CharacterClassifier classy;
   private Translator trans;

   public ImageTranslator() throws Exception {
      this(FONTS);
   }

   public ImageTranslator(String[] fonts) throws Exception {
      String trainingAlphabet = "";
      for (int i = 0; i < fonts.length; i++) {
         trainingAlphabet += ALPHABET;
      }

      classy = new PDCClassifier(CharacterImage.generateFontImages(ALPHABET, fonts),
                                 trainingAlphabet, true, 1, fonts);
      trans = new Translator("ja", "en");
   }

   public WrapImage translate(WrapImage baseImage) {
      BubbleDetection.BubbleInfo[] bubbles = BubbleDetection.extractBubblesWithInfo(baseImage);

      for (BubbleDetection.BubbleInfo bubble : bubbles) {
         String text = "";

         BubbleText bubbleText = BubbleText.constructBubbleText(bubble.image);
         if (bubbleText == null) {
            // TODO(eriq): Add a metaword, or some indication of failure?
            continue;
         }

         for (BubbleText.TextSet textSet : bubbleText.getTextSets()) {
            for (WrapImage image : textSet.furiganaReplacementText) {
               text += classy.classify(image);
            }

            text += " ";
         }

         logger.debug(text.trim());

         String translation = trans.translate(text.trim());

         WrapImage transBubble = WrapImage.getStringImage(translation, false,
                                                          bubble.width, bubble.height);
         baseImage = ImageUtils.overlayImage(baseImage, transBubble,
                                             bubble.startRow, bubble.startCol);
      }

      return baseImage;
   }
}
