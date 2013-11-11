package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.image.CharacterImage;
import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.Props;

import magick.ImageInfo;
import magick.MagickImage;

/**
 * Generate a sheet of all the kana for a single font.
 * TODO(eriq): Do other font related actions.
 */
public class GenerateFontSheet {
   public static void main(String[] args) throws Exception {
      if (args.length != 2) {
         System.out.println(
            "USAGE: java GenerateFontSheet <font name> <out file>");
         return;
      }

      // TODO(eriq): .properties file need unicode char to be written using hex codes.
      //  Get the kana in the config files properly.
      String hiragana = "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもや ゆ よらりるれろわ   をん";

      MagickImage[] images = CharacterImage.generateFontImages(
         hiragana, args[0]);

      MagickImage sheet = ImageUtils.gridCombine(images, 5);
      sheet.setFileName(args[1] + ".png");
      sheet.writeImage(new ImageInfo());
   }
}
