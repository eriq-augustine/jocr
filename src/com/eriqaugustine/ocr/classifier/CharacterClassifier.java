package com.eriqaugustine.ocr.pdc;

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

   public CharacterClassifier(List<WrapImage> characterImages,
                              String characters,
                              String[] fonts,
                              int featureVectorLength,
                              Map<String, String> classifierAttributes) throws Exception {
      super(characterImages,
            StringUtils.charSplit(characters),
            featureVectorLength,
            " ", // A space is the default class.
            MapUtils.inlinePut(classifierAttributes, "fonts", StringUtils.join(fonts, ", ")));
   }

   /**
    * @inheritDoc
    */
   protected boolean isEmpty(WrapImage image) {
      return image.isEmpty();
   }
}
