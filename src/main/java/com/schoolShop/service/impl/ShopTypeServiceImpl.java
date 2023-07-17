package com.schoolShop.service.impl;

import cn.hutool.json.JSONUtil;
import com.schoolShop.dto.Result;
import com.schoolShop.entity.ShopType;
import com.schoolShop.mapper.ShopTypeMapper;
import com.schoolShop.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jodd.util.StringUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import static com.schoolShop.utils.RedisConstants.CACHE_SHOP_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author ycy
 * @since 2019-3-13
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ShopType> queryList() {
        //1、从Redis中查询商户的类型
        String key = CACHE_SHOP_KEY;
        String  shopTypes = stringRedisTemplate.opsForValue().get(key);
        //2、如果查询到了，直接返回商户的类型
        if(StringUtil.isNotBlank(shopTypes)) {
            List<ShopType> shopType = JSONUtil.toList(shopTypes,ShopType.class);
            return (shopType);
        }
        //3、如果查询不到，去数据库进行查找，找到之后写入cache，并返回
        List<ShopType> shopType= query().orderByAsc("sort").list();
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shopType));
        return shopType;
    }
}
