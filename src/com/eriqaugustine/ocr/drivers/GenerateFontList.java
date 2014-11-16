package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.utils.FontUtils;
import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.ListUtils;
import com.eriqaugustine.ocr.utils.Props;

import java.util.ArrayList;
import java.util.List;

/**
 * General a single image that has one line for each font listed.
 */
public class GenerateFontList {
   private static final String SAMPLE_CHARACTERS = "あいうえおかきくけこ。アイウエオカキクケコ。日一大年中会人本月。‘”?!";

   private static final int SAMPLE_WIDTH = 5000;
   private static final int SAMPLE_HEIGHT = 50;

   public static void main(String[] args) {
      FontUtils.registerLocalFonts();

      List<String> fonts = new ArrayList<String>();

      if (args.length == 0) {
         // ListUtils.append(fonts, Props.getList("CLASSIFIER_TRAINING_FONTS"));
         ListUtils.append(fonts, FontUtils.getLocalFontNames());
      } else {
         ListUtils.append(fonts, args);
      }

      WrapImage baseImage = WrapImage.getBlankImage(SAMPLE_WIDTH, SAMPLE_HEIGHT * fonts.size());

      int maxFontNameLength = 0;
      for (String font : fonts) {
         if (font.length() > maxFontNameLength) {
            maxFontNameLength = font.length();
         }
      }
      maxFontNameLength += 5;

      int labelWidth = 10 * maxFontNameLength;
      int textWidth = SAMPLE_WIDTH - labelWidth;

      // Get the stips for each font.
      for (int i = 0; i < fonts.size(); i++) {
         String label = String.format("%-" + maxFontNameLength + "s:", fonts.get(i));
         WrapImage labelImage = WrapImage.getStringImage(label, false, labelWidth, SAMPLE_HEIGHT);

         WrapImage textImage = WrapImage.getStringImage(SAMPLE_CHARACTERS, false, textWidth, SAMPLE_HEIGHT, fonts.get(i));

         baseImage = ImageUtils.overlayImage(baseImage, labelImage, SAMPLE_HEIGHT * i, 0);
         baseImage = ImageUtils.overlayImage(baseImage, textImage, SAMPLE_HEIGHT * i, labelWidth);
      }

      baseImage.shrink().write("fontList.png");
   }
}
