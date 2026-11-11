package diary.file.entity.convert;

import diary.file.entity.po.IconPO;
import diary.file.entity.po.ImagePO;
import diary.file.entity.vo.IconVO;
import diary.file.entity.vo.ImageUploadStatusVO;
import diary.file.enums.FileMqOutboxStatusEnum;
import diary.file.enums.ImageStatusEnum;

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

    public static ImageUploadStatusVO convertToImageUploadStatusVO(ImagePO po) {
        ImageStatusEnum imageStatusEnum = ImageStatusEnum.fromCode(po.getStatus());
        return ImageUploadStatusVO.builder()
                .imageId(po.getId())
                .status(imageStatusEnum.name())
                .statusCode(po.getStatus())
                .terminal(imageStatusEnum.isTerminal())
                .success(imageStatusEnum.isSuccess())
                .description(imageStatusEnum.getDescription())
                .build();
    }
}
