package com.eriqaugustine.ocr.image;

import static com.eriqaugustine.ocr.image.WrapImage.Pixel;
import com.eriqaugustine.ocr.utils.ColorUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * The interface to detecting callouts (speech bubbles).
 */
public abstract class BubbleDetector {
   /**
    * Get the raw blobs that represent the bubbles.
    */
   public abstract List<Blob> getBubbles(WrapImage image);

   public abstract BubbleInfo[] extractBubblesWithInfo(WrapImage image);

   /**
    * Extract the pixels for each bubble and convert them to an image.
    */
   public WrapImage[] extractBubbles(WrapImage image) {
      BubbleInfo[] bubbles = extractBubblesWithInfo(image);
      WrapImage[] images = new WrapImage[bubbles.length];
      for (int i = 0; i < bubbles.length; i++) {
         images[i] = bubbles[i].image;
      }
      return images;
   }

   public WrapImage colorBubbles(WrapImage image) {
      List<Blob> bubbles = getBubbles(image);
      return colorBubbles(image, bubbles);
   }

   public WrapImage colorBubbles(WrapImage image, BubbleInfo[] bubbleInfos) {
      List<Blob> blobs = new ArrayList<Blob>(bubbleInfos.length);
      for (BubbleInfo info : bubbleInfos) {
         blobs.add(info.blob);
      }

      return colorBubbles(image, blobs);
   }

   /**
    * Color all the text bubbles in a copy of |image|.
    * It is assumed that |image| has been edged, and that it has
    *  only two colors, true black and true white.
    * White pixels are edges.
    */
   public WrapImage colorBubbles(WrapImage image, List<Blob> bubbles) {
      Pixel[] pixels = image.getPixels();

      // Fill the blobs.
      fillBlobs(pixels, bubbles, null);

      WrapImage newImage = WrapImage.getImageFromPixels(pixels, image.width(), image.height());

      return newImage;
   }

   /**
    * Modify |pixels| to fill in all the blobs as red.
    */
   private void fillBlobs(Pixel[] pixels, List<Blob> blobs) {
      fillBlobs(pixels, blobs, new Color(255, 0, 0));
   }

   /**
    * If |color| is null, then pick a different colot every time.
    */
   private void fillBlobs(Pixel[] pixels, List<Blob> blobs, Color color) {
      for (Blob blob : blobs) {
         Color activeColor = color != null ? color : ColorUtils.nextColor();

         for (Integer index : blob.getPoints()) {
            int pixelIndex = index.intValue();

            // Mark the blobs as red.
            pixels[pixelIndex] = new Pixel(activeColor);
         }
      }
   }
}
