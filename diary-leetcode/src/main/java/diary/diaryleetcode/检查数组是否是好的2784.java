package diary.diaryleetcode;

import java.util.HashMap;

public class 检查数组是否是好的2784 {
    public static void main(String[] args) {
        int[] nums = {1,4,4,4,4};
        System.out.println(isGood(nums));
    }

    public static boolean isGood(int[] nums) {
        // 1ms 44.14MB
        HashMap<Integer, Integer> map = new HashMap<>();
        int temp = 0;
        int max = nums[0];
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] > max) max = nums[i];
        }
        if (max + 1 != nums.length) return false;
        for (int num = 0; num < nums.length; num++) {
            if (map.containsKey(nums[num]) && nums[num] != max) {
                return false;
            }
            if (nums[num] == max) {
                temp++;
            }
            if (temp > 2) return false;
            map.put(nums[num], num);
        }
        return true;
    }
}
/*
* 1  1
* 1  2  2
* 1  2  3  3
* 1  2  3  4  4
* */