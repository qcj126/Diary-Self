package diary.diaryleetcode;

public class 只出现一次的数字136 {
    public static void main(String[] args) {
        int[] nums = {2, 2, 1};
        System.out.println(singleNumber1(nums));
    }
    // 异或方式，相同数字异或结果为0，任何数字与0异或结果为本身   人工计算时转换为2进制再计算
    public static int singleNumber1(int[] nums) {
        int single = 0;
        for (int num : nums) {
            single ^= num;
        }
        return single;
    }
}
