package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.utils.FontUtils;
import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.ListUtils;
import com.eriqaugustine.ocr.utils.Props;

import java.util.ArrayList;
import java.util.List;

/**
 * General a single image that is the given text in the given font.
 */
public class GenerateFontSample {
   private static final int SAMPLE_WIDTH = 5000;
   private static final int SAMPLE_HEIGHT = 50;
   private static final String DEFAULT_OUT_FILE = "fontSample.png";

   public static void main(String[] args) {
      if (args.length < 2 || args.length > 3) {
         System.out.println("USAGE: java com.eriqaugustine.ocr.drivers.GenerateFontSample <font name> <text> [out file name]");
         System.exit(1);
      }

      FontUtils.registerLocalFonts();

      String outputFile = args.length == 3 ? args[2] : DEFAULT_OUT_FILE;
      WrapImage image = WrapImage.getStringImage(args[1], false, SAMPLE_WIDTH, SAMPLE_HEIGHT, args[0]);
      image.shrink().write(outputFile);
   }
}
