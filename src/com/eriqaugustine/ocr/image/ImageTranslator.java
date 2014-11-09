package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.classifier.OCRClassifier;
import com.eriqaugustine.ocr.classifier.PDCClassifier;
import com.eriqaugustine.ocr.translate.Translator;
import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.ListUtils;
import com.eriqaugustine.ocr.utils.Props;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Translate and replace the text in an image.
 */
public class ImageTranslator {
   private static Logger logger = LogManager.getLogger(ImageTranslator.class.getName());

   private static final String[] FONTS = new String[]{"Baekmuk Batang", "RyuminStd-Bold-KO"};

   private static final String ALPHABET = Props.getString("KANA_FULL");

   private OCRClassifier classy;
   private Translator trans;

   public ImageTranslator() throws Exception {
      this(FONTS);
   }

   public ImageTranslator(OCRClassifier classy) throws Exception {
      this.classy = classy;
      trans = new Translator("ja", "en");
   }

   public ImageTranslator(String[] fonts) throws Exception {
      String trainingAlphabet = "";
      for (int i = 0; i < fonts.length; i++) {
         trainingAlphabet += ALPHABET;
      }

      classy = new PDCClassifier(trainingAlphabet, true, 1, fonts);
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
            List<ImageText> imageTexts = preClassifyCleanup(textSet.furiganaReplacementText);
            String currentSetText = "";

            for (ImageText imageText : imageTexts) {
               if (imageText.text == null) {
                  for (WrapImage image : imageText.images) {
                     currentSetText += classy.classify(image);
                  }
               } else {
                  currentSetText += imageText.text;
               }
            }

            currentSetText = postClassifyCleanup(currentSetText);

            text += currentSetText + " ";
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

   /**
    * Apply some heuristics to cleanup the images before passing them on to be classified.
    * If a returned ImageText contains a string portion, then there is no need to classify it.
    * Some of the core work this will do is recognize periords and dashes
    * (without support, these will be cropped and re-expanded to the same thing, a black box).
    * Note that the output length will not necessarily be the same as the input length.
    */
   private List<ImageText> preClassifyCleanup(List<WrapImage> characterImages) {
      List<ImageText> rtn = new ArrayList<ImageText>();

      // Initialize and make a first pass at heuristic classification.
      for (WrapImage image : characterImages) {
         ImageText imageText = new ImageText(image);

         String text = heuristicClassify(image);
         if (text != null) {
            imageText.text = text;
         }

         rtn.add(imageText);
      }

      // Look for three periods and replace with an ellipsis.
      // Make sure to make multiple passes.
      boolean done = false;
      while (!done) {
         done = true;

         for (int i = 0; i < rtn.size() - 2; i++) {
            if (rtn.get(i).textEquals(".") && rtn.get(i + 1).textEquals(".") && rtn.get(i + 2).textEquals(".")) {
               List<WrapImage> images = new ArrayList<WrapImage>();
               images.addAll(rtn.get(i).images);
               images.addAll(rtn.get(i + 1).images);
               images.addAll(rtn.get(i + 2).images);

               ImageText imageText = new ImageText(images);
               imageText.text = "…";

               // Put in the elipses.
               rtn.set(i, imageText);

               // Remove the other periods.
               rtn.remove(i + 2);
               rtn.remove(i + 1);

               done = false;
            }
         }
      }

      // Look for a vertical bar followed by a period and replace with a bang.
      done = false;
      while (!done) {
         done = true;

         for (int i = 0; i < rtn.size() - 1; i++) {
            if (rtn.get(i).textEquals("|") && rtn.get(i + 1).textEquals(".")) {
               List<WrapImage> images = new ArrayList<WrapImage>();
               images.addAll(rtn.get(i).images);
               images.addAll(rtn.get(i + 1).images);

               ImageText imageText = new ImageText(images);
               imageText.text = "!";

               // Put in the bang.
               rtn.set(i, imageText);

               // Remove the period.
               rtn.remove(i + 1);

               done = false;
            }
         }
      }

      return rtn;
   }

   /**
     * Try to classify simple things like periods and lines.
     * Will return null if no classification.
     */
   private String heuristicClassify(WrapImage image) {
      String rtn = null;

      double density = ImageUtils.density(image, 200);
      double widthToHeightRatio = (double)image.width() / (double)image.height();

      // Check for periods.
      if (density > 0.75 && widthToHeightRatio >= 0.8 && widthToHeightRatio <= 1.2) {
         rtn = ".";
      // Check for dashes.
      } else if (density > 0.60 && widthToHeightRatio >= 3.0) {
         rtn = "-";
      // Check for vertical bars.
      } else if (density > 0.60 && widthToHeightRatio <= 0.33) {
         rtn = "|";
      }

      /*
      System.err.println("Density: " + density + ", ratio: " + widthToHeightRatio + ", replace: " + rtn);
      System.err.println("Width: " + image.width() + ", Height: " + image.height());
      System.err.println("------");
      if (rtn != null) {
         System.err.println(ImageUtils.asciiImage(image));
      } else {
         System.err.println("No Replacement");
      }
      System.err.println("------");
      */

      return rtn;
   }

   /**
    * Apply some heuristics after the text has been classified.
    * This is a great place to cleanup very common misclassifications.
    * This should not do super serious work like spelling correction.
    * One common thing to to is combine characters like "ワ" and "." into "?".
    */
   private String postClassifyCleanup(String input) {
      // System.err.println("Pre : " + input);

      // "ハ‥" -> "‼"
      // Note that the second character is a double ellipsis (some fonts show it strangely).
      input = input.replaceAll("ハ‥", "‼");

      // Replace double ellipsis with standard ellipsis.
      input = input.replaceAll("‥", "…");

      // Quote
      input = input.replaceAll("ワ\\s*\\.", "?");
      input = input.replaceAll("つ\\s*\\.", "?");
      input = input.replaceAll("っ\\s*\\.", "?");

      // Bang
      input = input.replaceAll("\\|\\s*\\.", "!");
      input = input.replaceAll("/\\s*\\.", "!");

      // Ellipsis
      input = input.replaceAll("\\.\\s*\\.\\s*\\.", "…");

      // "一ん" -> "え"
      input = input.replaceAll("-ん", "え");

      // "‼" -> "!"
      // For consistency, lets just use single bangs.
      input = input.replaceAll("‼", "!");
      input = input.replaceAll("!(\\s*!)*", "!");

      // Change any remaining periods to japanese periods.
      input = input.replaceAll("\\.", "。");

      // System.err.println("Post: " + input);

      return input;
   }

   /**
    * Hold the image and text for a specific character.
    * We want to use this intermediary container so we can do pre and post classification heuristics.
    * It is the caller's responsibility to check for null and sizes.
    */
   private class ImageText {
      public List<WrapImage> images;
      public String text;

      public ImageText() {
         images = new ArrayList<WrapImage>();
         text = null;
      }

      public ImageText(WrapImage image) {
         this();
         images.add(image);
      }

      public ImageText(List<WrapImage> images) {
         this();
         this.images.addAll(images);
      }

      public ImageText(WrapImage[] images) {
         this();
         ListUtils.append(this.images, images);
      }

      /**
       * Use this for a quick null and equality check.
       */
      public boolean textEquals(String other) {
         return text != null && text.equals(other);
      }
   }
}
