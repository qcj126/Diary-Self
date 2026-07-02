package diary.common.enums.exportenum;

import diary.common.exception.ParamIllegalException;
import lombok.Getter;

@Getter
public enum ExportEnum {
    EXCEL(1, "导出为Excel文件"),
    PDF(2, "导出为PDF文件"),
    IMAGE(3, "导出为Image文件");
    private final Integer code;
    private final String desc;
    ExportEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
