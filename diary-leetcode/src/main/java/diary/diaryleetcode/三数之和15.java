package diary.diaryleetcode;

import com.sun.source.tree.ReturnTree;

import java.util.*;

public class 三数之和15 {
    public static void main(String[] args) {
        int[] nums = {-100,-70,-60,110,120,130,160}; // 1,4,-5  2,3,-5  1,6,-7  2,5,-7 3,4,-7
        List<List<Integer>> lists = threeSum(nums);
        List<List<Integer>> lists2 = threeSum2(nums);
        System.out.println(lists);
        System.out.println(lists2);
    }
    // 暴力求解法超时
    public static List<List<Integer>> threeSum(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        for (int i = 0; i < nums.length - 2; i++) {
            for (int j = i + 1; j < nums.length - 1; j++) {
                for (int k = j + 1; k < nums.length; k++) {
                    if (nums[i] + nums[j] + nums[k] == 0) {
                        ArrayList<Integer> list = new ArrayList<>();
                        list.add(nums[i]);
                        list.add(nums[j]);
                        list.add(nums[k]);
                        list.sort(Integer::compareTo);
                        if (!result.contains(list)) result.add(list);
                    }
                }
            }
        }
        return result;
    }

    // 哈希求解
    public static List<List<Integer>> threeSum2(int[] nums) {
        // 错误写法
//        HashMap<Integer, Integer> stableSet = new HashMap<>();
//        List<List<Integer>> result = new ArrayList<>();
//        for (int i = 0; i < nums.length - 1; i++) {
//            for (int j = i + 1; j < nums.length; j++) {
//                if (stableSet.containsKey(-(nums[i] + nums[j]))) {
//                    ArrayList<Integer> list = new ArrayList<>();
//                    list.add(nums[i]);
//                    list.add(nums[j]);
//                    list.add(-(nums[i] + nums[j]));
//                    list.sort(Integer::compareTo);
//
//                    if (!result.contains(list)) result.add(list);
//                } else {
//                    stableSet.put(nums[i], stableSet.getOrDefault(nums[i], 0) + 1);
//                }
//            }
//        }
//        return result;

        // 正确写法   594ms 59.63MB
        // 1. 先排序，方便去重
        Arrays.sort(nums);
        Set<List<Integer>> resultSet = new HashSet<>();

        for (int i = 0; i < nums.length - 2; i++) {
            // 剪枝：如果 nums[i] > 0，后面不可能有三数和为 0
            if (nums[i] > 0) break;
            // 跳过重复的 i（可选优化）
            if (i > 0 && nums[i] == nums[i - 1]) continue;

            int target = -nums[i];
            Set<Integer> seen = new HashSet<>();  // 存已经遍历过的 nums[j]

            for (int j = i + 1; j < nums.length; j++) {
                int complement = target - nums[j];  // 需要找的数

                if (seen.contains(complement)) {
                    // 找到三元组
                    List<Integer> triplet = Arrays.asList(nums[i], complement, nums[j]);
                    // 因为 nums 已排序，triplet 自动有序
                    resultSet.add(triplet);
                }

                seen.add(nums[j]);  // 把当前数加入已遍历集合
            }
        }
        return new ArrayList<>(resultSet);
    }
}
