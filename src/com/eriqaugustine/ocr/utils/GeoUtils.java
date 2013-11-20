package com.eriqaugustine.ocr.utils;

import java.awt.Rectangle;

/**
 * Geometry related utils.
 */
public class GeoUtils {
   /**
    * Do |a| and |b| overlap in the horizontal dimension.
    * Ignores vertical dimension.
    */
   public static boolean hasHorizontalOverlap(Rectangle a, Rectangle b) {
      int left = Math.max(a.x, b.x);
      int right = Math.min(a.x + a.width, b.x + b.width);

      return right >= left;
   }
}
