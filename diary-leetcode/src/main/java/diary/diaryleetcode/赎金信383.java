package diary.diaryleetcode;

import java.util.Arrays;

public class 赎金信383 {
    public static void main(String[] args) {
        String ransomNote = "aa";
        String magazine = "aab";
        boolean trueth1 = canConstruct1(ransomNote, magazine);
        boolean trueth2 = canConstruct2(ransomNote, magazine);
        System.out.println(trueth1);
        System.out.println(trueth2);
    }

    public static boolean canConstruct1(String ransomNote, String magazine) {
        // magazine中的字符，仅使用一次，看能否拼凑出ransomNote
        // 1ms 45.43MB
        int[] ints = new int[26];
        char[] charArray = magazine.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            ints[charArray[i] - 97] ++;
        }

        char[] charArray2 = ransomNote.toCharArray();
        for (int i = 0; i < charArray2.length; i++) {
            if (ints[charArray2[i] - 97] > 0) {
                ints[charArray2[i] - 97] --;
            } else {
                return false;
            }
        }
        return true;
    }

    // 双指针解法
    public static boolean canConstruct2(String ransomNote, String magazine) {
        // magazine中的字符，仅使用一次，看能否拼凑出ransomNote
        int i = 0;
        int j = 0;

        char[] charArray1 = ransomNote.toCharArray();
        char[] charArray2 = magazine.toCharArray();
        Arrays.sort(charArray1);
        Arrays.sort(charArray2);

        while (i < charArray1.length && j < charArray2.length) {
            if (charArray1[i] == charArray2[j]) {
                i ++;
                j ++;
            } else {
                j ++;
            }
        }
        return i > charArray1.length - 1;
    }
}