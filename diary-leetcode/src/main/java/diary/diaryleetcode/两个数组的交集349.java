package diary.diaryleetcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class 两个数组的交集349 {
    public static void main(String[] args) {
        int[] nums1 = {1,4,3,4,5,2,1};
        int[] nums2 = {4,5,6};
        int[] result = intersection1(nums1, nums2);
        int[] result2 = intersection2(nums1, nums2);
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

        for (int i = 0; i < nums1.length;) {
            for (int j = 0; j < nums2.length;) {
                if (nums1[i] == nums2[j]) {
                    hashSet.add(nums1[i]);
                    i++;
                    j++;
                }
                if (nums1[i] > nums2[j]) j++;
                if (nums1[i] < nums2[j]) i++;
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
}
