package diary.file.entity.convert.icon;

import diary.file.entity.dto.IconDTO;
import diary.file.entity.po.IconPO;

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
