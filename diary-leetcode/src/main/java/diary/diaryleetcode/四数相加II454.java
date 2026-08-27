package diary.diaryleetcode;

import java.util.HashMap;
import java.util.HashSet;

public class 四数相加II454 {
    public static void main(String[] args) {
        int[] nums1 = {-1,-1};
        int[] nums2 = {-1,1};
        int[] nums3 = {-1,1};
        int[] nums4 = {1,-1};
        System.out.println(fourSumCount(nums1, nums2, nums3, nums4));
        System.out.println(fourSumCount1(nums1, nums2, nums3, nums4));
    }

    // 152ms  46.36MB
    // api改为getOrDefault之后，性能提升不大  115ms 46.29MB
    private static int fourSumCount1(int[] nums1, int[] nums2, int[] nums3, int[] nums4) {
        int cnt = 0;   // -2  2个  0  2个
        HashMap<Integer, Integer> hashMap = new HashMap<>(nums1.length * 2);
        for (int value : nums1) {
            for (int k : nums2) {
                int sum = value + k;
                hashMap.put(sum, hashMap.getOrDefault(sum, 0) + 1);
            }
        }
        for (int k : nums3) {
            for (int i : nums4) { // 0  2个  2  1个
                cnt += hashMap.getOrDefault(-k - i, 0);
            }
        }

        return cnt;
    }

    // 暴力求解法  超时
    private static int fourSumCount(int[] nums1, int[] nums2, int[] nums3, int[] nums4) {
        int cnt = 0;
        for (int i = 0; i < nums1.length; i++) {
            for (int j = 0; j < nums2.length; j++) {
                for (int k = 0; k < nums3.length; k++) {
                    for (int l = 0; l < nums4.length; l++) {
                        if (nums1[i] + nums2[j] + nums3[k] + nums4[l] == 0) {
                            cnt++;
                        }
                    }
                }
            }
        }
        return cnt;
    }
}
