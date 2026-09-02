package diary.diaryleetcode;

import java.util.HashMap;
import java.util.HashSet;

public class 宝石与石头771 {
    public static void main(String[] args) {
        String jewels = "aA";
        String stones = "aAAbbbb";
        System.out.println(numJewelsInStones(jewels, stones));
    }
    public static int numJewelsInStones(String jewels, String stones) {
        HashMap<Character, Integer> map = new HashMap<>();
        // 存入石头类型以及石头数量
        for (char c : stones.toCharArray()) {
            map.put(c, map.getOrDefault(c, 0) + 1);
        }

        // 开始计数
        int count = 0;
        for (char c : jewels.toCharArray()) {
            count += map.getOrDefault(c, 0);
            map.remove(c);
        }
        return count;
    }

}
