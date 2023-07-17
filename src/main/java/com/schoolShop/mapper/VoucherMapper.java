package com.schoolShop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.schoolShop.entity.Voucher;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author ycy
 * @since 2019-3-13
 */
public interface VoucherMapper extends BaseMapper<Voucher> {

    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
}
