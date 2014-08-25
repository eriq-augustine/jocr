package com.eriqaugustine.ocr.classifier;

import com.eriqaugustine.ocr.classifier.reduce.FeatureVectorReducer;
import com.eriqaugustine.ocr.image.CharacterImage;
import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.utils.ListUtils;
import com.eriqaugustine.ocr.utils.StringUtils;
import com.eriqaugustine.ocr.utils.MapUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A classifier specialized for ORCing characters.
 */
public abstract class CharacterClassifier
      extends VectorClassifier<WrapImage>
      implements OCRClassifier {
   private static Logger logger = LogManager.getLogger(CharacterClassifier.class.getName());

   public CharacterClassifier(int featureVectorLength) {
      super(featureVectorLength, " "); // A space is the default class.
   }

   public CharacterClassifier(int featureVectorLength, FeatureVectorReducer reduce) {
      super(featureVectorLength, " ", reduce); // A space is the default class.
   }

   protected boolean train(String characters,
                           String[] fonts,
                           Map<String, String> classifierAttributes) {
      String allCharacters = "";
      List<WrapImage> allImages = new ArrayList<WrapImage>();

      for (String font : fonts) {
         allCharacters += characters;
         ListUtils.append(allImages, CharacterImage.generateFontImages(characters, font));
      }

      return train(allImages, allCharacters, fonts, classifierAttributes);
   }

   /**
    * @inheritDoc
    * |characters| is 1-1 with characterImages.
    * |fonts| should be all the fonts used when generating |characterImages|.
    */
   protected boolean train(List<WrapImage> characterImages,
                           String characters,
                           String[] fonts,
                           Map<String, String> classifierAttributes) {
      return train(characterImages,
                   StringUtils.charSplit(characters),
                   MapUtils.inlinePut(classifierAttributes, "fonts", StringUtils.join(fonts, ", ")));
   }

   /**
    * @inheritDoc
    */
   protected boolean isEmpty(WrapImage image) {
      return image.isEmpty();
   }
}
