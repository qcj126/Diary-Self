package diary.diaryleetcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class 两个数组的交集349 {
    public static void main(String[] args) {
        int[] nums2 = {1,4,3,4,5,2,1};
        int[] nums1 = {4,5,6};
        int[] result = intersection1(nums1, nums2);
        int[] result2 = intersection2(nums1, nums2);
        int[] result3 = intersection3(nums1, nums2);
    }

    public static int[] intersection1(int[] nums1, int[] nums2) {
        // 3ms 44.55MB
        HashSet<Integer> hashSet1 = new HashSet<>();
        HashSet<Integer> hashSet2 = new HashSet<>();
        HashSet<Integer> hashSet3 = new HashSet<>();
        for (int i = 0; i < nums1.length; i++) {
            hashSet1.add(nums1[i]);
        }
        for (int i = 0; i < nums2.length; i++) {
            hashSet2.add(nums2[i]);
        }

        for (Integer i : hashSet1) {
            if (hashSet2.contains(i)) {
                hashSet3.add(i);
                hashSet2.remove(i);
            }
        }
        int[] ints = new int[hashSet3.size()];
        int i = 0;
        for (Integer integer : hashSet3) {
            ints[i] = integer;
            i++;
        }
        return ints;
    }

    public static int[] intersection2(int[] nums1, int[] nums2) {
        // 2ms 44.18MB
        HashSet<Integer> hashSet1 = new HashSet<>();
        HashSet<Integer> hashSet3 = new HashSet<>();
        for (int i = 0; i < nums1.length; i++) {
            hashSet1.add(nums1[i]);
        }

        for (Integer i : nums2) {
            if (hashSet1.contains(i)) {
                hashSet3.add(i);
            }
        }
        int[] ints = new int[hashSet3.size()];
        int i = 0;
        for (Integer integer : hashSet3) {
            ints[i] = integer;
            i++;
        }
        return ints;
    }

    // 双指针解法
    public static int[] intersection3(int[] nums1, int[] nums2) {
        HashSet<Integer> hashSet = new HashSet<>();

        Arrays.sort(nums1);
        Arrays.sort(nums2);

        for (int i = 0, j = 0; i < nums1.length && j < nums2.length;) {
            if (nums1[i] == nums2[j]) {
                hashSet.add(nums1[i]);
                i++;
                j++;
            } else {
                if (nums1[i] > nums2[j]) {
                    j++;
                }
                if (j == nums2.length) break;
                if (nums1[i] < nums2[j]) {
                    i++;
                }
            }
        }
        int[] ints = new int[hashSet.size()];
        int i = 0;
        for (Integer integer : hashSet) {
            ints[i] = integer;
            i++;
        }
        return ints;
    }

    // 两个数组的交集 II
    public int[] intersect4(int[] nums1, int[] nums2) {
        // 10ms 43.89MB
        ArrayList<Integer> hashSet1 = new ArrayList<>();
        ArrayList<Integer> hashSet3 = new ArrayList<>();
        for (int i = 0; i < nums1.length; i++) {
            hashSet1.add(nums1[i]);
        }

        for (Integer i : nums2) {
            if (hashSet1.contains(i)) {
                hashSet3.add(i);
                hashSet1.remove(i);
            }
        }
        int[] ints = new int[hashSet3.size()];
        int i = 0;
        for (Integer integer : hashSet3) {
            ints[i] = integer;
            i++;
        }
        return ints;
    }

    // 两个数组的交集 II
    public int[] intersect5(int[] nums1, int[] nums2) {
        // 3ms 44.43MB
        // 第一个是小集合
        if (nums1.length > nums2.length) {
            return intersect5(nums2, nums1);
        }
        int[] ints = new int[nums1.length];
        HashMap<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < nums1.length; i++) {
            map.put(nums1[i], map.getOrDefault(nums1[i], 0) + 1);
        }
        int index = 0;
        for (int i = 0; i < nums2.length; i++) {
            if (map.containsKey(nums2[i])) {
                if (map.get(nums2[i]) > 0) {
                    map.put(nums2[i], map.get(nums2[i]) - 1);
                    ints[index ++] = nums2[i];
                } else {
                    map.remove(nums2[i]);
                }
            }
        }
        return Arrays.copyOfRange(ints, 0, index);
    }

    // 两个数组的交集 II
    public int[] intersect6(int[] nums1, int[] nums2) {
        // 3ms 44.61MB
        // 第一个是小集合
        if (nums1.length > nums2.length) {
            return intersect6(nums2, nums1);
        }
        HashMap<Integer, Integer> map = new HashMap<>();
        HashMap<Integer, Integer> resultMap = new HashMap<>();
        for (int i = 0; i < nums1.length; i++) {
            map.put(nums1[i], map.getOrDefault(nums1[i], 0) + 1);
        }
        int index = 0;
        for (int i = 0; i < nums2.length; i++) {
            if (map.containsKey(nums2[i])) {
                if (map.get(nums2[i]) > 0) {
                    map.put(nums2[i], map.get(nums2[i]) - 1);
                    resultMap.put(index ++, nums2[i]);
                } else {
                    map.remove(nums2[i]);
                }
            }
        }
        int[] ints = new int[resultMap.size()];
        for (int x = 0; x < resultMap.size(); x++) {
            ints[x] = resultMap.get(x);
        }
        return ints;
    }

    // 两数组交集 II 数组作为hash表的方式     与投票选举类似
    public int[] intersect7(int[] nums1, int[] nums2) {
        // 0ms 44.16MB
        int[] count = new int[1001];
        for (int i : nums1) {
            count[i] ++;
        }
        int length = nums1.length <= nums2.length ? nums1.length : nums2.length;
        int[] tempArr = new int[length];
        int index = 0;
        for (int i : nums2) {
            if (count[i] > 0) {
                tempArr[index ++] = i;
                count[i] --;
            }
        }
        return Arrays.copyOfRange(tempArr, 0, index);
    }
}
