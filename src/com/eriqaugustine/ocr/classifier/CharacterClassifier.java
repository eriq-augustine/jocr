package com.eriqaugustine.ocr.classifier;

import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.utils.StringUtils;
import com.eriqaugustine.ocr.utils.MapUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * A classifier specialized for ORCing characters.
 */
public abstract class CharacterClassifier extends VectorClassifier<WrapImage> {
   private static Logger logger = LogManager.getLogger(CharacterClassifier.class.getName());

   public CharacterClassifier(int featureVectorLength) {
      super(featureVectorLength, " "); // A space is the default class.
   }

   /**
    * @inheritDoc
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
