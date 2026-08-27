package diary.diaryleetcode;

import com.sun.source.tree.ReturnTree;

import java.util.*;

public class 三数之和15 {
    public static void main(String[] args) {
        int[] nums = {-5,1,-3,-1,-4,-2,4,-1,-1}; // 1,4,-5  2,3,-5  1,6,-7  2,5,-7 3,4,-7
        List<List<Integer>> lists = threeSum(nums);
        List<List<Integer>> lists2 = threeSum2(nums);
        List<List<Integer>> lists3 = threeSum3(nums);
        System.out.println(lists);
        System.out.println(lists2);
        System.out.println(lists3);
    }

    // 前后双指针解法
    // 43ms  58.16MB
    // 优化之后的版本：31ms  58.01MB
    public static List<List<Integer>> threeSum3(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> resultSet = new ArrayList<>();
        // 先固定一个桶，用剩余数字进行加操作
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] > 0) break;
            if (i > 0 && nums[i - 1] == nums[i]) continue;
            int target = -nums[i];
            int right = nums.length - 1;
            int left = i + 1;
            while (left < right) {
                // 每次循环判重，可优化为找到3元组后，再判重，留下的依旧是不重复元素
//                if (nums[left] == nums[left - 1] && left > i + 1) {
//                    left ++;
//                    continue;
//                }
//                if (right < nums.length - 1 && nums[right] == nums[right + 1]) {
//                    right --;
//                    continue;
//                }
                int add = nums[left] + nums[right];
                if (target == add) {
                    while(left < right && nums[left] == nums[left+1]) left++;
                    while(left < right && nums[right] == nums[right-1]) right--;
                    ArrayList<Integer> objects = new ArrayList<>(3);
                    objects.add(nums[i]);
                    objects.add(nums[left]);
                    objects.add(nums[right]);
                    resultSet.add(objects); // list中的数据是天然排序的
                    right --;
                    left ++;
                } else if (add < target) { // 说明left所在的数太小了，需要大一点的
                    left ++;
                } else { // 说明right所在的数太大了，需要小一点的
                    right --;
                }
            }
        }
        return resultSet;

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

        // 正确写法   383ms 59.91MB
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
