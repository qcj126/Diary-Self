package diary.diaryleetcode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class 存在重复元素217 {
    public static void main(String[] args) {
        int[] nums = {1,2,3,1};
        System.out.println(containsDuplicate1(nums));
        System.out.println(containsDuplicate2(nums));
        System.out.println(containsDuplicate3(nums));
    }

    // hash表
    public static boolean containsDuplicate1(int[] nums) {
        HashMap<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            if (map.containsKey(nums[i])) {
                return true;
            } else {
                map.put(nums[i], i);
            }
        }
        return false;
    }

    // 排序
    public static boolean containsDuplicate2(int[] nums) {
//        Arrays.sort(nums);
//        for (int i = 0; i < nums.length - 1; i++) {
//            if (nums[i] == nums[i + 1]) return true;
//        }
        return false;
    }

    // hashset
    public static boolean containsDuplicate3(int[] nums) {
        HashSet<Integer> set = new HashSet<>(10000);
        for (int i = 0; i < nums.length; i++) {
            if (!set.add(nums[i])) return true;
        }
        return false;
    }
}
