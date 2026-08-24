package diary.diaryleetcode;

import java.util.HashMap;

public class 存在重复元素219 {
    public static void main(String[] args) {
        int[] nums = {1,2,3,1,2,3};
        int k = 2;

        boolean result = containsNearbyDuplicate(nums, k);
        System.out.println(result);
    }

    private static boolean containsNearbyDuplicate(int[] nums, int k) {
        HashMap<Integer, Integer> map = new HashMap<>();

        for (int num = 0; num < nums.length; num++) {
            if (map.containsKey(nums[num])) {
                Integer index = map.get(nums[num]);
                if (num - index <= k) return true;
            }
            map.put(nums[num], num);
        }
        return false;
    }
}
