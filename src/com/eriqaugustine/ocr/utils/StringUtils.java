package com.eriqaugustine.ocr.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utils for Strings.
 */
public class StringUtils {
   /**
    * Split a string into its individual characters (also as Strings).
    */
   public static List<String> charSplit(String str) {
      List<String> rtn = new ArrayList<String>(str.length());
      for (int i = 0; i < str.length(); i++) {
         rtn.add("" + str.charAt(i));
      }

      return rtn;
   }

   public static String[] charSplitArray(String str) {
      String[] rtn = new String[str.length()];
      for (int i = 0; i < str.length(); i++) {
         rtn[i] = "" + str.charAt(i);
      }

      return rtn;
   }

   public static String join(String[] strs) {
      return join(strs, ",");
   }

   public static String join(String[] strs, String  delim) {
      String rtn = "";

      for (String str : strs) {
         rtn += str + delim;
      }

      return rtn.replaceFirst(delim + "$", "");
   }
}
