package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.utils.ImageUtils;

/**
 * Convert an image into an ascii image and print it on stdout.
 */
public class AsciiImage {
   public static void main(String[] args) throws Exception {
      if (args.length != 1) {
         System.out.println("USAGE: java com.eriqaugustine.ocr.drivers.AsciiImage <image>");
         return;
      }

      WrapImage image = WrapImage.getImageFromFile(args[0]);
      System.out.println(ImageUtils.asciiImage(image));
   }
}
