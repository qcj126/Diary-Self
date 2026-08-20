package diary.diaryleetcode;

import java.util.Arrays;

public class 有效的字母异位词242 {
    public static void main(String[] args) {
        String s = "aaa";
        String t = "bbb";

        System.out.println(isAnagram1(s, t));
        System.out.println(isAnagram2(s, t));
    }

    public static boolean isAnagram1(String s, String t) {
        // 解法1  3ms  45.93MB
        char[] sCharArray = s.toCharArray();
        Arrays.sort(sCharArray);

        char[] tCharArray = t.toCharArray();
        Arrays.sort(tCharArray);
        return Arrays.equals(sCharArray, tCharArray);
    }

    public static boolean isAnagram2(String s, String t) {
        // 解法2：数组桶  1ms  43.89MB
        if (s.length() != t.length()) return false;
        int[] table = new int[26];

        for (char c : s.toCharArray()) {
            table[c - 'a']++;
        }
        for (char cc : t.toCharArray()) {
            table[cc - 'a']--;
            if (table[cc - 'a'] < 0) return false;
        }
        return true;
    }
}
