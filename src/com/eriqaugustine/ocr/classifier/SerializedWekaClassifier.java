package com.eriqaugustine.ocr.classifier;

import com.eriqaugustine.ocr.utils.Props;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import weka.classifiers.Classifier;
import weka.core.Instances;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles serializing and caching for weka classifiers.
 */
public class SerializedWekaClassifier {
   private static Logger logger = LogManager.getLogger(SerializedWekaClassifier.class.getName());

   private static final String CLASSIFIER_CACHE_PREFIX = "classifier";
   private static final String CLASSIFIER_CACHE_FILE_NAME = "classifier";
   private static final String CLASSIFIER_INFO_FILE_NAME = "info.txt";

   /**
    * Static access only.
    */
   private SerializedWekaClassifier() {
   }

   public static Classifier fetchClassifier(Class<? extends Classifier> type,
                                            Instances trainingSet) {
      return fetchClassifier(type, trainingSet, true, new HashMap<String, String>(), "");
   }

   /**
    * The uniqueness of a classifier is determined by the classifier's type, the |attributes|,
    * and the training set.
    */
   public static Classifier fetchClassifier(Class<? extends Classifier> type, Instances trainingSet,
                                            boolean cache,
                                            Map<String, String> attributes, String notes) {
      // Boring.
      if (!cache) {
         return makeClassifier(type, trainingSet);
      }

      String id = cacheKey(type, attributes, trainingSet);
      String cacheDirPath = String.format("%s%s%s_%s",
                                          Props.getString("CACHE_DIR"),
                                          File.separator,
                                          CLASSIFIER_CACHE_PREFIX,
                                          id);
      File cacheDir = new File(cacheDirPath);

      Classifier classy = fetchCache(cacheDir);

      if (classy == null) {
         classy = makeClassifier(type, trainingSet);

         if (classy != null) {
            // Cache this classifier.
            putCache(cacheDir, classy, attributes, notes);
         }
      }

      return classy;
   }

   private static Classifier makeClassifier(Class<? extends Classifier> type,
                                            Instances trainingSet) {
      try {
         Classifier classy = (Classifier)type.newInstance();
         classy.buildClassifier(trainingSet);
         return classy;
      } catch (Exception ex) {
         logger.error("Unable to create classifier.", ex);
         return null;
      }
   }

   /**
    * Generate the unique key for the cache.
    */
   private static String cacheKey(Class<? extends Classifier> classifierType,
                                  Map<String, String> attributes,
                                  Instances trainingSet) {
      try {
         ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

         // Add the full name of the classifier.
         byte[] nameBytes = classifierType.getName().getBytes();
         byteStream.write(nameBytes, 0, nameBytes.length);

         // Now the attributes.
         byte[] attributeBytes = attributes.toString().getBytes();
         byteStream.write(attributeBytes, 0, attributeBytes.length);

         // Ensure that the training set is used as part of the key.
         ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
         objectStream.writeObject(trainingSet);
         objectStream.close();

         String hex = DigestUtils.sha1Hex(byteStream.toByteArray());

         // Include the shortname of the classifier in the id (for readability).
         return String.format("%s-%s", classifierType.getSimpleName(), hex);
      } catch (Exception ex) {
         return null;
      }
   }

   /**
    * Write out the extra information about the classifier.
    * This info if mainly for human readability.
    * Purposefully let this throw (will make it more elegant upstream).
    */
   private static void writeInfo(File classifierCacheDir,
                                 Classifier classy,
                                 Map<String, String> attributes, String notes) throws Exception {
      File infoFile = new File(classifierCacheDir, CLASSIFIER_INFO_FILE_NAME);
      BufferedWriter writer = new BufferedWriter(new FileWriter(infoFile));

      writer.write(String.format("Classifier: %s (%s)\n",
                                 classy.getClass().getSimpleName(),
                                 classy.getClass().getName()));
      writer.write("Date Created: " + new Date() + "\n");
      writer.write("Notes:\n   " + notes + "\n");

      writer.write("Attributes:\n");
      for (Map.Entry<String, String> attribute : attributes.entrySet()) {
         writer.write(String.format("   %s -- %s\n",
                                    attribute.getKey(),
                                    attribute.getValue()));
      }

      writer.close();
   }

   private static Classifier fetchCache(File classifierCacheDir) {
      try {
         if (!classifierCacheDir.exists()) {
            return null;
         } else if (!classifierCacheDir.isDirectory()) {
            logger.warn("A classifier cache was not a dir.");
            classifierCacheDir.delete();
         }

         File classifierCacheFile = new File(classifierCacheDir, CLASSIFIER_CACHE_FILE_NAME);

         if (!classifierCacheFile.exists() || !classifierCacheFile.isFile()) {
            logger.warn("A classifier cache is improperly formatted.");
            return null;
         }

         FileInputStream fileStream = new FileInputStream(new File(classifierCacheDir,
                                                                   CLASSIFIER_CACHE_FILE_NAME));
         ObjectInputStream objectStream = new ObjectInputStream(fileStream);
         Classifier classy = (Classifier)objectStream.readObject();
         objectStream.close();

         return classy;
      } catch (Exception ex) {
         logger.error("Error fetching classifier cache.", ex);
         return null;
      }
   }

   private static boolean putCache(File classifierCacheDir, Classifier classy,
                                   Map<String, String> attributes, String notes) {
      try {
         if (classifierCacheDir.exists()) {
            classifierCacheDir.delete();
         }

         classifierCacheDir.mkdir();

         writeInfo(classifierCacheDir, classy, attributes, notes);
         FileOutputStream fileStream = new FileOutputStream(new File(classifierCacheDir,
                                                            CLASSIFIER_CACHE_FILE_NAME));
         ObjectOutputStream objectStream = new ObjectOutputStream(fileStream);
         objectStream.writeObject(classy);
         objectStream.close();
         return true;
      } catch (Exception ex) {
         logger.error("Error creating classifier cache.", ex);
         classifierCacheDir.delete();
         return false;
      }
   }
}
