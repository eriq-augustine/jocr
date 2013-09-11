package com.eriqaugustine.ocr.pdc;

/**
 * An empty DC.
 * The numeric value for this DC would be [0, 0, 0, 0, 0, 0, 0, 0].
 */
public class EmptyDCFeature extends DCFeature {
   public EmptyDCFeature() {
      super();
   }

   public boolean empty() {
      return true;
   }
}
