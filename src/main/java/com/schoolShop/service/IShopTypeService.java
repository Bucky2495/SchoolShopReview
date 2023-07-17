package com.schoolShop.service;

import com.schoolShop.dto.Result;
import com.schoolShop.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ycy
 * @since 2019-3-13
 */
public interface IShopTypeService extends IService<ShopType> {


    List<ShopType> queryList();
}
