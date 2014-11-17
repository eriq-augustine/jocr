package com.eriqaugustine.ocr.classifier;

import com.eriqaugustine.ocr.image.ImageText;
import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Translate and replace the text in an image.
 */
public class BasePreClassifier implements PreClassifier {
   /**
    * If a returned ImageText contains a string portion, then there is no need to classify it.
    * Some of the core work this will do is recognize periords and dashes
    * (without support, these will be cropped and re-expanded to the same thing, a black box).
    * Note that the output length will not necessarily be the same as the input length.
    */
   public List<ImageText> preClassify(List<WrapImage> characterImages) {
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
}
