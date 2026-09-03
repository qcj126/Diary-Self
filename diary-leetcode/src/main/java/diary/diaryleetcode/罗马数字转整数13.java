package diary.diaryleetcode;

import java.util.HashMap;

public class 罗马数字转整数13 {
    public static void main(String[] args) {
        String s = "MCMXCIV";
        System.out.println(romanToInt1(s));
        System.out.println(romanToInt2(s));
        System.out.println(romanToInt3(s));
    }

    // 第一版
    public static int romanToInt1(String s) {
        HashMap<Character, Integer> map = new HashMap<>();
        map.put('I', 1);
        map.put('V', 5);
        map.put('X', 10);
        map.put('L', 50);
        map.put('C', 100);
        map.put('D', 500);
        map.put('M', 1000);
        int result = 0;
        if (s.length() == 1) {
            return map.get(s.charAt(0));
        }
        for (int i = 0; i < s.length(); i++) {
            // 左边比右边小，这是减法
            if (i < s.length() - 1) {
                if (map.get(s.charAt(i)) < map.get(s.charAt(i + 1))) {
                    result += map.get(s.charAt(i + 1)) - map.get(s.charAt(i));
                } else {
                    result += map.get(s.charAt(i + 1)) + map.get(s.charAt(i));
                }
                if (i > 0) {
                    result -= map.get(s.charAt(i));
                }
            }
        }
        return result;
    }

    // 第二版
    public static int romanToInt2(String s) {
        HashMap<Character, Integer> map = new HashMap<>();
        map.put('I', 1);
        map.put('V', 5);
        map.put('X', 10);
        map.put('L', 50);
        map.put('C', 100);
        map.put('D', 500);
        map.put('M', 1000);

        if (s.length() == 1) {
            return map.get(s.charAt(0));
        }

        int result = 0;
        if (map.get(s.charAt(0)) < map.get(s.charAt(1))) {
            result = map.get(s.charAt(1)) - map.get(s.charAt(0));
        } else {
            result = map.get(s.charAt(1)) + map.get(s.charAt(0));
        }

        for (int i = 2; i < s.length(); i++) {
            // 左边比右边小，这是减法
            if (map.get(s.charAt(i)) > map.get(s.charAt(i - 1))) {
                result += map.get(s.charAt(i)) - 2 * map.get(s.charAt(i - 1));
            } else {
                result += map.get(s.charAt(i));
            }
        }
        return result;
    }

    // 第三版
    public static int romanToInt3(String s) {
        int sum=0;
        for (int i=0;i<s.length();i++){
            int t=gv(s.charAt(i));
            if (i+1<s.length() && t<gv(s.charAt(i+1))){
                sum-=t;
            }
            else{
                sum+=t;
            }
        }
        return sum;
    }
    public static int gv(char c){
        switch(c){
            case 'I':return 1;
            case 'V':return 5;
            case 'X':return 10;
            case 'L':return 50;
            case 'C':return 100;
            case 'D':return 500;
            case 'M':return 1000;
        }
        return 0;
    }
}
