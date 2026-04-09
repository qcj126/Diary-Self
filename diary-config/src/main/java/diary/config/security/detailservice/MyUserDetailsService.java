package diary.config.security.detailservice;

import diary.config.security.detail.SecurityUserDetails;
import diary.dao.mapper.user.UserMapper;
import diary.dao.entity.user.po.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String identify) throws UsernameNotFoundException {
        User user = null;
        if (identify.contains("@")) {
            user = userMapper.selectByEmail(identify);
        } else if (identify.matches("^1[3-9]\\d{9}$")) {
            user = userMapper.selectByPhone(identify);
        } else {
            user = userMapper.selectByUsername(identify);
        }
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        user.setRoles(List.of("ROLE_USER"));
        return new SecurityUserDetails(user);
    }
}
