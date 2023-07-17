package com.schoolShop.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schoolShop.dto.LoginFormDTO;
import com.schoolShop.dto.Result;
import com.schoolShop.dto.UserDTO;
import com.schoolShop.entity.User;
import com.schoolShop.mapper.UserMapper;
import com.schoolShop.service.IUserService;
import com.schoolShop.utils.RegexUtils;
import com.schoolShop.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.schoolShop.utils.RedisConstants.*;
import static com.schoolShop.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author ycy
 * @since 2019-3-13
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private  UserMapper userMapper;

    @Override
    public Result sendCode(String phone, HttpSession session)  {
        //1、校验手机号
        boolean phoneInvalid = RegexUtils.isPhoneInvalid(phone);
        //2、如果不符合返回错误信息
        if(phoneInvalid) {
            return Result.fail("手机格式错误");
        }
        //3、符合的话要生成验证码
        String code = RandomUtil.randomNumbers(6);
//        //4、将验证码保存到session
//        session.setAttribute("code",code);
        //将验证码存到Redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+ phone ,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //5、发送验证码给用户,实际要调用发短信的业务接口，此处为模拟
        log.debug("code=" + code);
        //6、返回成功信息
        return Result.ok();

    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();

        //1、校验手机号
        if(RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号出错");
        }
        //2、校验验证码
        //Session
//        Object code = session.getAttribute("code");
        String code1 = loginForm.getCode();
        //Redis：拿到存在redis中的验证码
        String code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);

        //3、不一致返回格式错误
        if(code1 == null || !(code1.equals(code))) {
            return Result.fail("验证码出错");
        }
        //4、一致，查询数据表中是否存在用户
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("phone",phone);
        User user = getOne(userQueryWrapper);
//         User user = query().eq("phone", phone).one();
        //5、如果不存在，创建用户并保存
        if(user == null) {
           user = createUserWithPhone(phone);

        }

        UserDTO userDTO = new UserDTO();
        userDTO.setIcon(user.getIcon());
        userDTO.setId(user.getId());
        userDTO.setNickName(user.getNickName());
//        //6、将用户写入session
//        session.setAttribute("user",userDTO);

        //Redis:保存用户
        //1、生成一个token
        String token = UUID.randomUUID().toString(true);
        log.debug(token);
        //2、将User对象转为HashMap存储
//        UserDTO userDTO1 = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String,String> UserMap = new HashMap<>();
        UserMap.put("id",userDTO.getId().toString());
        UserMap.put("icon",userDTO.getIcon().toString());
        UserMap.put("nickname",userDTO.getIcon().toString());
//        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO1,new HashMap<>(), CopyOptions.create().
//                setIgnoreNullValue(true).
//                setFieldValueEditor((fieldName,fieldValue)-> fieldValue.toString()));
        //3、存储
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,UserMap);
        //4、设置token有效期
        stringRedisTemplate.expire(tokenKey,30,TimeUnit.MINUTES);
        //5、返回token
        return Result.ok(token);


    }

    @Override
    public Result sign() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.写入Redis SETBIT key offset 1
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.获取本月截止今天为止的所有的签到记录，返回的是一个十进制的数字 BITFIELD sign:5:202203 GET u14 0
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if (result == null || result.isEmpty()) {
            // 没有任何签到结果
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return Result.ok(0);
        }
        // 6.循环遍历
        int count = 0;
        while (true) {
            // 6.1.让这个数字与1做与运算，得到数字的最后一个bit位  // 判断这个bit位是否为0
            if ((num & 1) == 0) {
                // 如果为0，说明未签到，结束
                break;
            }else {
                // 如果不为0，说明已签到，计数器+1
                count++;
            }
            // 把数字右移一位，抛弃最后一个bit位，继续下一个bit位
            num >>>= 1;
        }
        return Result.ok(count);
    }

    private User createUserWithPhone(String phone) {
        // 1.创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        // 2.保存用户
        save(user);
        return user;
    }
}
