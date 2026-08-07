package diary.common.convert.file;

import diary.common.entity.file.dto.IconDTO;
import diary.common.entity.file.po.IconPO;

public class DtoConvertToPo {
    public static IconPO iconDtoConvertToPO(IconDTO dto) {
        IconPO iconPO = new IconPO();
        iconPO.setId(dto.getId());
        iconPO.setIconName(dto.getIconName());
        iconPO.setIconType(dto.getIconType());
        iconPO.setIconPath(dto.getIconPath());
        iconPO.setIconSize(dto.getIconSize());
        iconPO.setIconPixel(dto.getIconPixel());
        iconPO.setUserId(dto.getUserId());
        iconPO.setCreateTime(dto.getCreateTime());
        iconPO.setUpdateTime(dto.getUpdateTime());
        return iconPO;
    }
}
