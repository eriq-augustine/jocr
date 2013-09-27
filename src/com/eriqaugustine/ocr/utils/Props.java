package com.eriqaugustine.ocr.utils;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The Props is the place to go for properties and configurations.
 * Props will read any any specified files and hold them in a Map.
 * You cannot remove properties.
 * If there are properties in multiple files, the later properties will override the first.
 *
 * Advanced Apache Commons logging are used, so you can use the array grouping feature.
 *  Just give everything the same name, and they will be grouped as a List<String>.
 *
 * Properties are held only as Strings or List<String>.
 * However, you can request properties in specified formats.
 * If the conversion fails, you will get the default value.
 */
public class Props {
   private static Logger logger = LogManager.getLogger(Props.class.getName());

   /**
    * All the properties read in.
    */
   private static Map<String, PropType> props = new HashMap<String, PropType>();

   // TODO(eriq): This should really be done by the primary driver
   static {
      readFile("config/config.properties");
   }

   /**
    * Check to see if the key has an associated property.
    */
   public static boolean has(String key) {
      return props.containsKey(key);
   }

   /**
    * Get a String value.
    *
    * @param key The key.
    *
    * @return the value associated with key, or null if the key
    *  does not exist.
    */
   public static String getString(String key) {
      PropType prop = props.get(key);

      if (prop == null || !prop.isString()) {
         logger.warn("Tried to get a property that does not exist: {}", key);
         return null;
      }

      return ((StringProp)prop).data;
   }

   /**
    * Get a String value, or a default value if key is not in the properties.
    *
    * @param key The key.
    * @param defaultValue The value that will be returned if key does not exist.
    *
    * @return the value associated with key, or defaultValue.
    */
   public static String getString(String key, String defaultValue) {
      String val = getString(key);

      if (val == null) {
         return defaultValue;
      }

      return val;
   }

   /**
    * Get a int value, or a default value if key is not in the properties.
    *
    * @param key The key.
    * @param defaultValue The value that will be returned if key does not exist.
    *
    * @return the value associated with key, or defaultValue.
    */
   public static int getInt(String key, int defaultValue) {
      String val = getString(key);

      if (val == null) {
         return defaultValue;
      }

      return Integer.parseInt(val);
   }

   /**
    * Get a double value, or a default value if key is not in the properties.
    *
    * @param key The key.
    * @param defaultValue The value that will be returned if key does not exist.
    *
    * @return the value associated with key, or defaultValue.
    */
   public static double getDouble(String key, double defaultValue) {
      String val = getString(key);

      if (val == null) {
         return defaultValue;
      }

      return Double.parseDouble(val);
   }

   /**
    * Get a boolean value, or a default value if key is not in the properties.
    * WARNING: There is no check to see if the value in the store is "true" or "false".
    *  Anything that is not "true" evaluates to false.
    *
    * @param key The key.
    * @param defaultValue The value that will be returned if key does not exist.
    *
    * @return the value associated with key, or defaultValue.
    */
   public static boolean getBoolean(String key, boolean defaultValue) {
      String val = getString(key);

      if (val == null) {
         return defaultValue;
      }

      return Boolean.parseBoolean(val);
   }

   /**
    * Get a list of string values.
    * If the key does not exists, then an empty list will be
    *  returned.
    * If the key is not a list, then the value will be put in a list.
    *
    * @param key The key.
    *
    * @return the value associated with key, or an empty list.
    */
   public static List<String> getList(String key) {
      List<String> rtn = new ArrayList<String>();

      PropType prop = props.get(key);

      if (prop == null) {
         return rtn;
      }

      if (!prop.isArray()) {
         rtn.add(((StringProp)prop).data);
      } else {
         for (String strProp : ((ListProp)prop).data) {
            rtn.add(strProp);
         }
      }

      return rtn;
   }

   /**
    * Read a properties file and load all of the properties into Props.
    * Any properties conflicting with previous files will be overwritten.
    */
   public static boolean readFile(String fileName) {
      //Note: getKeys() returns the SET of all keys.
      // Therefore, lists will only be listed once.

      //Every key will be attempted to be lifted as an array.
      // If the array only has one element, it will be turned into a
      // single value.
      boolean failedConversion = false;

      try {
         PropertiesConfiguration propsFile = new PropertiesConfiguration(fileName);

         @SuppressWarnings("unchecked")
         Iterator<String> keys = propsFile.getKeys();
         while (keys.hasNext()) {
            String key = keys.next();

            try {
               String[] vals = propsFile.getStringArray(key);
               PropType prop;

               if (vals.length == 1) {
                  prop = new StringProp(vals[0]);
               } else {
                  List<String> propList = new ArrayList<String>();
                  for (String val : vals) {
                     propList.add(val);
                  }
                  prop = new ListProp(propList);
               }
               props.put(key, prop);
            } catch (Exception ex) {
               logger.error("Error lifting property, [{}] from file: {}.", key, fileName);
               failedConversion = true;
            }
         }
      } catch (Exception ex) {
            logger.fatal("Error getting properties from: {}.", fileName);
         return false;
      }

      return !failedConversion;
   }

   /**
    * The types of properties allowed.
    * This is either a String or List<String>.
    * Doing this avoids having to do weird/unsafe casting.
    */
   private static class PropType {
      /**
       * If it is not a String, it is an Array<String>.
       */
      private boolean isString;

      public PropType(boolean isString) {
         this.isString = isString;
      }

      public boolean isString() {
         return isString;
      }

      public boolean isArray() {
         return !isString;
      }
   }

   private static class StringProp extends PropType {
      public String data;

      public StringProp(String data) {
         super(true);
         this.data = data;
      }
   }

   private static class ListProp extends PropType {
      public List<String> data;

      public ListProp(List<String> data) {
         super(false);
         this.data = data;
      }
   }
}
