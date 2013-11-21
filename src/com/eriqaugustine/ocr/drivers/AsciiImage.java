package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.utils.ImageUtils;

import magick.ImageInfo;
import magick.MagickImage;

/**
 * Convert an image into an ascii image and print it on stdout.
 */
public class AsciiImage {
   public static void main(String[] args) throws Exception {
      if (args.length != 1) {
         System.out.println("USAGE: java com.eriqaugustine.ocr.drivers.AsciiImage <image>");
         return;
      }

      ImageInfo info = new ImageInfo(args[0]);
      MagickImage baseImage = new MagickImage(info);
      System.out.println(ImageUtils.asciiImage(baseImage));
   }
}
