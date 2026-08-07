package diary.common.convert.file;

import diary.common.entity.file.po.IconPO;
import diary.common.entity.file.vo.IconVO;

public class PoConvertToVo {
    public static IconVO convertToIconVO(IconPO po) {
        if (po == null) {
            return null;
        }
        IconVO iconVO = new IconVO();
        iconVO.setId(po.getId());
        iconVO.setIconName(po.getIconName());
        iconVO.setIconType(po.getIconType());
        iconVO.setIconPath(po.getIconPath());
        iconVO.setIconSize(po.getIconSize());
        iconVO.setIconPixel(po.getIconPixel());
        iconVO.setUserId(po.getUserId());
        iconVO.setCreateTime(po.getCreateTime());
        iconVO.setUpdateTime(po.getUpdateTime());
        return iconVO;
    }
}
