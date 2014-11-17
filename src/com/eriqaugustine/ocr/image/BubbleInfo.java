package com.eriqaugustine.ocr.image;

/**
  * A container for information about bubbles.
  */
public class BubbleInfo {
   public final int startRow;
   public final int startCol;
   public final int width;
   public final int height;
   public final WrapImage image;
   public final Blob blob;

   public BubbleInfo(int startRow, int startCol, int width, int height, WrapImage image, Blob blob) {
      this.startRow = startRow;
      this.startCol = startCol;
      this.width = width;
      this.height = height;
      this.image = image;
      this.blob = blob;
   }
}
