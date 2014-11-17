package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.utils.ListUtils;

import java.util.ArrayList;
import java.util.List;

/**
  * Hold the image and text for a specific character.
  * We want to use this intermediary container so we can do pre and post classification heuristics.
  * It is the caller's responsibility to check for null and sizes.
  */
public class ImageText {
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
