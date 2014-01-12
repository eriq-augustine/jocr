package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.image.CharacterImage;
import com.eriqaugustine.ocr.utils.FontUtils;
import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.Props;

import magick.ImageInfo;
import magick.MagickImage;

import java.io.File;

/**
 * Generate a sheet of all the kana for a single font.
 * TODO(eriq): Do other font related actions.
 */
public class GenerateFontSheet {
   public static void main(String[] args) throws Exception {
      if (args.length != 2) {
         System.out.println(
            "USAGE: java GenerateFontSheet <font name> <out file>\n" +
            "       java GenerateFontSheet - <base dir>");
         return;
      }

      FontUtils.registerLocalFonts();

      if (args[0].equals("-")) {
         String[] fontNames = FontUtils.getLocalFontNames();
         for (String fontName : fontNames) {
            writeFontSheet(fontName, args[1] + File.separator + fontName + ".png");
         }
      } else {
         writeFontSheet(args[0], args[1] + ".png");
      }
   }

   public static void writeFontSheet(String fontName, String outFile) throws Exception {
      System.out.println("Generating " + fontName + " to " + outFile + " ...");

      String kana = Props.getString("HIRAGANA") + "    " + Props.getString("KATAKANA");

      MagickImage[] images = CharacterImage.generateFontImages(kana, fontName);

      MagickImage sheet = ImageUtils.gridCombine(images, 5);
      sheet.setFileName(outFile);
      sheet.writeImage(new ImageInfo());
   }
}
