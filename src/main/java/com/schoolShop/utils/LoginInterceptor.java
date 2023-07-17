package com.schoolShop.utils;

import cn.hutool.core.bean.BeanUtil;
import com.schoolShop.dto.UserDTO;
import jodd.util.StringUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//       //Session:
//        //1、获得session
//        HttpSession session = request.getSession();
//        //2、获取session中的用户
//        Object user = session.getAttribute("user");
//        //3、判断用户是否存在, //4、不存在拦截
//        if(user == null) {
//            response.setStatus(401);
//            return false;
//        }
//        //5、存在，保存用户信息保存在TreadLocal
//        UserHolder.saveUser((UserDTO)user);
//        return true;
//        //Redis:
//        //1、获取请求头中的token
//        String token = request.getHeader("authorization");
//        if (StringUtil.isBlank(token)) {
//            response.setStatus(401);
//            return false;
//        }
//        //2、根据token去获取redis中的用户信息
//        String key = RedisConstants.LOGIN_USER_KEY + token;
//        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
//        //3、判断用户是否存在
//        if (userMap.isEmpty()) {
//            response.setStatus(401);
//            return false;
//        }
//        //4、将查询到的对象转化为UserDTO
//        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
//
//        //5、将用户信息保存在TreadLocal中
//        UserHolder.saveUser(userDTO);
//
//        //6、刷新token时间
//        stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
//
//        //7、放行
//        return true;

        //版本二：第二个拦截器只做报存用户到threadLocal的作用
        //1、判断threadlocal中是否有用户
        if(UserHolder.getUser() == null) {
            response.setStatus(401);//设置用户未登录状态码
            return false;
        }
        //2、有用户数据的话放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();//清空Treadlocal防止内存泄露
    }
}
