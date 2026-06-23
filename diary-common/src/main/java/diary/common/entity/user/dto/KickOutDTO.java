package diary.common.entity.user.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class KickOutDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String username;
}
