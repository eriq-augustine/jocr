package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.utils.ImageUtils;

import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
  * A container for a group of text that appears in an image.
  */
public class TextSet {
   /**
     * The entire set image (hopefully, the kanji and its covering furigana in the same image).
     */
   public final WrapImage baseImage;

   /**
     * The set without any furigana.
     */
   public final List<WrapImage> noFuriganaText;

   /**
     * The text with any furigana replacing the kanji it covers.
     */
   public final List<WrapImage> furiganaReplacementText;

   /**
     * Keep the original params to make swapImage() easier.
     */
   private List<Rectangle> origFullText;
   private Map<Rectangle, List<Rectangle>> origFuriganaMapping;

   public TextSet(WrapImage image,
                  List<Rectangle> fullText,
                  Map<Rectangle, List<Rectangle>> furiganaMapping) {
      baseImage = image.copy();

      // Keep the original information.
      origFullText = new ArrayList<Rectangle>(fullText);
      origFuriganaMapping = new HashMap<Rectangle, List<Rectangle>>(furiganaMapping);

      noFuriganaText = new ArrayList<WrapImage>();
      for (Rectangle rect : fullText) {
         noFuriganaText.add(baseImage.crop(rect));
      }

      furiganaReplacementText = new ArrayList<WrapImage>();
      for (Rectangle rect : fullText) {
         if (furiganaMapping.containsKey(rect)) {
            for (Rectangle furiRect : furiganaMapping.get(rect)) {
               furiganaReplacementText.add(baseImage.crop(furiRect));
            }
         } else {
            furiganaReplacementText.add(baseImage.crop(rect));
         }
      }
   }

   /**
     * Get a new TextSet that has |newImage| as the base.
     */
   public TextSet swapImage(WrapImage newImage) {
      return new TextSet(newImage, origFullText, origFuriganaMapping);
   }

   /**
    * Get a single image that has all the characters in a single line.
    * This is useful for debugging purposes.
    */
   public WrapImage toImage(boolean replaceFurigana) {
      List<WrapImage> images = replaceFurigana ? furiganaReplacementText : noFuriganaText;
      return ImageUtils.concatImages(images);
   }
}
