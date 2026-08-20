package diary.common.enums.aienum;

import diary.common.exception.ParamIllegalException;

public enum AiFlagEnum {
    DIET,
    RECIPE,
    GOAL;
    public static void isTrueFlag(String flag) {
        for (AiFlagEnum aiFlagEnum : AiFlagEnum.values()) {
            if (aiFlagEnum.name().equalsIgnoreCase(flag)) return ;
        }
        throw new ParamIllegalException("非许可flag参数，请检查！");
    }
}
