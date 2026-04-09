package diary.dao.mapper.user;

import diary.dao.entity.user.po.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    User selectByEmail(String identify);

    User selectByPhone(String identify);

    User selectByUsername(String identify);

    int userRegister(User user);

    void updatePassword(String username, String encode);
}
