package diary.diaryleetcode;

public class 和为K的子数组560 {
    public static void main(String[] args) {
        int[] nums = {1,2,3,4,5,6,7,8,9};
        int k = 3; // 3,2  5
        System.out.println(subarraySum(nums, k));
    }

    // 一个数时：必须小于等于k，不然直接跳过
    // 多个数时：第一个数 + 第二个数：
    //   和小于k，则将第三个数相加，直到和等于k
    //   和大于k时，直接跳过
    // 暴力枚举法
    private static int subarraySum(int[] nums, int k) {
        int result = 0;
        for (int i = 0; i < nums.length; i++) {
            int sum = 0;
            for (int j = i; j < nums.length; j++) {
                sum += nums[j];  // 累加，不需要每次都重新算
                if (sum == k) {
                    result++;
                }
            }
        }
        return result;
    }
}
