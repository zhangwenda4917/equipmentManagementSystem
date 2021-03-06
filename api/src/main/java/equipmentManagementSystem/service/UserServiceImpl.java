package equipmentManagementSystem.service;

import com.mengyunzhi.core.exception.ObjectNotFoundException;
import com.mengyunzhi.core.service.YunzhiService;
import equipmentManagementSystem.entity.User;
import equipmentManagementSystem.input.PUser;
import equipmentManagementSystem.input.VUser;
import equipmentManagementSystem.respority.DepartmentRepository;
import equipmentManagementSystem.respority.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.EntityNotFoundException;
import javax.xml.bind.ValidationException;

@Service
public class UserServiceImpl implements UserService{

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    public static final String DEFAULT_PASSWORD = "hebut";

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    private final YunzhiService<User> yunzhiService;

    public UserServiceImpl(UserRepository userRepository,
                           DepartmentRepository departmentRepository, YunzhiService<User> yunzhiService) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.yunzhiService = yunzhiService;
    }

    @Override
    public void add(User user) {
        user.setPassword(DEFAULT_PASSWORD);
        this.userRepository.save(user);
        if (user.getRole().equals(3L)) {
            user.getDepartment().setUser(user);
            this.departmentRepository.save(user.getDepartment());
        }
    }

    @Override
    public void delete(Long id) {
        this.userRepository.deleteById(id);
    }

    @Override
    public User getOneUnsavedUser() {
        return UserService.getOneUser();
    }

    @Override
    public User getCurrentLoginUser() {
        logger.debug("初始化用户");
        User user = null;

        logger.debug("获取用户认证信息");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        logger.debug("根据认证信息查询用户");
        if (authentication != null && authentication.isAuthenticated()) {
            user = userRepository.findByUsername(authentication.getName());
        }

        return user;
    }

    @Override
    public Page<User> quaryAll(String name, String jobNumber, Pageable pageable) {
        if (this.getCurrentLoginUser().getRole() != 3){
            return (Page<User>) this.userRepository.query(null, name, jobNumber, pageable, this.getCurrentLoginUser().getId());
        }
        else {
            return this.userRepository.query(this.getCurrentLoginUser().getDepartment(),name, jobNumber,
                    pageable, this.getCurrentLoginUser().getId());
        }
    }

    public User getOneSavedUser() {
        User user = this.getOneUnsavedUser();
        return this.userRepository.save(user);
    }

    @Override
    public User getUserById(Long id) {
        return this.userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("找不到相关角色"));
    }

    @Override
    public Page<User> getAll(Pageable pageable) {
        Long id = this.getCurrentLoginUser().getId();
        if (this.getCurrentLoginUser().getRole() != 3){
            return (Page<User>) this.userRepository.findAll(pageable);
        }
       else {
           return this.userRepository.getAllByDepartment(this.getCurrentLoginUser().getDepartment(),
                   pageable , this.getCurrentLoginUser().getId());
        }
    }


    @Override
    public Boolean isUsernameExist(String username) {
        logger.debug("根据新手机号查询用户");
        User user = this.userRepository.findByUsername(username);

        logger.debug("验证用户是否存在");
        if (user != null) {
            return true;
        } else {
            return user != null;
        }
    }


    @Override
    public User update(Long id, User user) {
        User oldUser = this.getUserById(id);
        oldUser.setUsername(user.getUsername());
        oldUser.setName(user.getName());
        oldUser.setSex(user.getSex());
        oldUser.setDepartment(user.getDepartment());
        oldUser.setJobNumber(user.getJobNumber());
        oldUser.setPhone(user.getPhone());
        oldUser.setRole(user.getRole());
        return this.userRepository.save(oldUser);
    }


    @Override
    public User findByUsername(String username) {
        return this.userRepository.findByUsername(username);
    }

    @Override
    public boolean checkPasswordIsRight(VUser vUser) {
        logger.debug("获取当前用户");
        User user = this.getCurrentLoginUser();

        logger.debug("比较密码是否正确");
        return user.verifyPassword(vUser.getPassword());
    }

    @Override
    public void updatePassword(VUser vUser) throws ValidationException {
        logger.debug("获取当前用户");
        User currentUser = this.getCurrentLoginUser();

        logger.debug("校验旧密码是否正确");
        if (!this.checkPasswordIsRight(vUser)) {
            throw new ValidationException("旧密码不正确");
        }

        logger.debug("更新密码");
        currentUser.setPassword(vUser.getNewPassword());
        this.userRepository.save(currentUser);
    }

    @Override
    public boolean verifyPassword() {
        logger.debug("获取当前用户");
        User currentUser = this.getCurrentLoginUser();

        logger.debug("验证密码是否是默认密码");
        return currentUser.verifyPassword(DEFAULT_PASSWORD);
    }

    @Override
    public void updatePhone(PUser pUser) throws ValidationException {
        logger.debug("获取当前用户");
        User currentUser = this.getCurrentLoginUser();

        logger.debug("验证密码是否匹配");

        logger.debug("校验旧密码是否正确");
        if (!currentUser.verifyPassword(pUser.getPassword())) {
            throw new ValidationException("旧密码不正确");
        }

        logger.debug("更新手机号(用户名)");
        currentUser.setUsername(pUser.getNewPhoneNumber());
        this.userRepository.save(currentUser);
    }


    @Override
    public Boolean verifyPhoneNumber(String phoneNumber) {
        logger.debug("获取当前用户");
        User currentUser = this.getCurrentLoginUser();

        logger.debug("验证新老手机号是否一致");
        return currentUser.getUsername().equals(phoneNumber);
    }

    @Override
    public Boolean existByPhone(String phoneNumber) {
        return this.userRepository.findByUsername(phoneNumber) != null;
    }


    /**
     * 重置密码
     *
     * @param id 用户id
     * @author pj
     */
    @Override
    public void resetPassword(Long id) {
        User user = this.userRepository.findById(id).orElseThrow(() -> new ObjectNotFoundException("实体未找到: " + id.toString()));
        user.setPassword(DEFAULT_PASSWORD);
        this.userRepository.save(user);
    }

    /**
     * 将上传的性别转换成Boolean 类型
     *
     * @param sex 上传的性别
     * @return 男 false 女 true
     */
    public Boolean formatSex(String sex) throws ValidationException {
        String man = "男";

        String woman = "女";
        if (man.equals(sex)) {
            return false;
        }
        if (woman.equals(sex)) {
            return true;
        }

        throw new ValidationException("上传的性别格式不正确");
    }
}
