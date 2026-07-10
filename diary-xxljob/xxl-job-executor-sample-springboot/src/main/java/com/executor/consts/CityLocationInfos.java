package com.executor.consts;

/**
 * 重庆地区城市编码枚举
 */
public enum CityLocationInfos {

    CHONG_QING(101040100, "重庆"),
    YONG_CHUAN(101040200, "永川"),
    HE_CHUAN(101040300, "合川"),
    NAN_CHUAN(101040400, "南川"),
    JIANG_JIN(101040500, "江津"),
    BEI_BEI(101040800, "北碚"),
    BA_NAN(101040900, "巴南"),
    CHANG_SHOU(101041000, "长寿"),
    QIAN_JIANG(101041100, "黔江"),
    YU_ZHONG(101041200, "渝中"),
    WAN_ZHOU(101041300, "万州"),
    FU_LING(101041400, "涪陵"),
    CHENG_KOU(101041600, "城口"),
    YUN_YANG(101041700, "云阳"),
    WU_XI(101041800, "巫溪"),
    FENG_JIE(101041900, "奉节"),
    WU_SHAN(101042000, "巫山"),
    TONG_NAN(101042100, "潼南"),
    DIAN_JIANG(101042200, "垫江"),
    LIANG_PING(101042300, "梁平"),
    ZHONG_XIAN(101042400, "忠县"),
    SHI_ZHU(101042500, "石柱"),
    DA_ZU(101042600, "大足"),
    RONG_CHANG(101042700, "荣昌"),
    TONG_LIANG(101042800, "铜梁"),
    BI_SHAN(101042900, "璧山"),
    FENG_DU(101043000, "丰都"),
    WU_LONG(101043100, "武隆"),
    PENG_SHUI(101043200, "彭水"),
    QI_JIANG(101043300, "綦江"),
    YOU_YANG(101043400, "酉阳"),
    DA_DU_KOU(101043500, "大渡口"),
    XIU_SHAN(101043600, "秀山"),
    SHA_PING_BA(101043800, "沙坪坝"),
    JIU_LONG_PO(101043900, "九龙坡"),
    NAN_AN(101044000, "南岸"),
    KAI_ZHOU(101044100, "开州"),
    LIANG_JIANG_XIN_QU(101044200, "两江新区");

    private final int code;
    private final String name;

    CityLocationInfos(int code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据城市名称查找枚举
     */
    public static Integer fromName(String name) {
        for (CityLocationInfos city : values()) {
            if (city.name.equals(name)) {
                return city.code;
            }
        }
        return null;
    }
}
