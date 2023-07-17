package com.schoolShop.service;

import com.schoolShop.dto.Result;
import com.schoolShop.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ycy
 * @since 2019-3-13
 */
public interface IShopService extends IService<Shop> {

    Result queryById(Long id) throws InterruptedException;

    Result update(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
