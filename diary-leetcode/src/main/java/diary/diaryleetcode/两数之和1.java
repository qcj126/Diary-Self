package diary.diaryleetcode;

import java.util.HashMap;

public class 两数之和1 {
    public static void main(String[] args) {
        int[] result = twoSum(new int[]{3,2,4}, 6);
        if (result != null) {
            System.out.println(result[0] + " " + result[1]);
        }
    }

    public static int[] twoSum(int[] nums, int target) {
        HashMap<Integer, Integer> map = new HashMap<>();

        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (map.containsKey(complement)) {
                Integer i1 = map.get(complement);
                return new int[] {i, i1};
            }
            map.put(nums[i], i);
        }
        return null;
    }
}
