package diary.file.service;

import diary.common.entity.file.dto.IconAddDTO;
import diary.common.entity.file.dto.IconDeleteDTO;
import diary.common.entity.file.dto.IconQueryDTO;
import diary.common.entity.file.dto.IconUpdateDTO;
import diary.common.entity.file.vo.IconVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IconService {
    IconVO addIcon(MultipartFile file, IconAddDTO iconAddDTO);

    List<IconVO> queryIcons(IconQueryDTO queryDTO);

    Boolean updateIcon(MultipartFile file, IconUpdateDTO iconUpdateDTO);

    Boolean deleteIcon(IconDeleteDTO deleteDTO);
}
