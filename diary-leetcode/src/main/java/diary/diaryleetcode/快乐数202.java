package diary.diaryleetcode;

import java.util.HashSet;

public class 快乐数202 {
    public static void main(String[] args) {
        int n = 19;
        System.out.println(isHappy(n));
    }

    // 出现循环则永远到达不了1
    public static boolean isHappy(int n) {
        HashSet<Integer> set = new HashSet<>();
        int nextNum = 0;
        while (nextNum != 1) {
            nextNum = 0;
            while (n != 0) {
                nextNum += (n%10) * (n%10);
                n /= 10;
            }
            if (!set.add(nextNum)) return false;
            n = nextNum;
        }
        return true;
    }
}
