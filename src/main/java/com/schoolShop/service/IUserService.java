package com.schoolShop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schoolShop.dto.LoginFormDTO;
import com.schoolShop.dto.Result;
import com.schoolShop.entity.User;

import javax.mail.MessagingException;
import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ycy
 * @since 2019-3-13
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session) throws MessagingException;

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result sign();

    Result signCount();

}
