package diary.diaryleetcode;

import java.util.HashMap;


public class 多数元素169 {
    public static void main(String[] args) {
        int[] nums = {6,5,5};
        int majorityElement = majorityElement(nums);
        System.out.println(majorityElement);
    }

    public static int majorityElement(int[] nums) {
        // 解法1  1ms  54.98MB
        int can = 0;
        int vote = 0;
        for (int num : nums) {
            if (vote == 0) {
                can = num;
            }
            vote += num == can ? 1 : -1;
        }
        return can;
    }
}
