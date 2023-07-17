package com.schoolShop.service.impl;

import ch.qos.logback.core.pattern.color.GreenCompositeConverter;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schoolShop.dto.Result;
import com.schoolShop.entity.SeckillVoucher;
import com.schoolShop.entity.VoucherOrder;
import com.schoolShop.mapper.VoucherOrderMapper;
import com.schoolShop.service.ISeckillVoucherService;
import com.schoolShop.service.IVoucherOrderService;
import com.schoolShop.utils.RedisIdWorker;
import com.schoolShop.utils.SimpleRedisLock;
import com.schoolShop.utils.UserHolder;
import com.sun.xml.internal.bind.v2.TODO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Executor;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

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
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

//    @PostConstruct
//    private void init() {
//        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandle());
//    }
//    private class VoucherOrderHandle implements Runnable {
//        String queneName ="stream.orders";
//        @SneakyThrows
//        @Override
//        public void run() {
//            while (true) {
//                try {
//                    //1、获取消息队列中的订单信息，XREADGRUOUP GRUOP g1 c1 COUNT 1 BLOCK 2000 STREAMS steams.order >
//                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
//                            Consumer.from("g1", "c1"),
//                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
//                            StreamOffset.create(queneName, ReadOffset.lastConsumed())
//                    );
//                    //2、判断消息是否存在
//                    if(list == null || list.isEmpty()) {
//                        continue;
//                    }
//                    //3、解析消息中的订单信息
//                    MapRecord<String, Object, Object> entries = list.get(0);
//                    Map<Object, Object> value = entries.getValue();
//                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
//                    //4、获取成功，可以下单
//                    handleVoucherOrder(voucherOrder);
//                    //5、ACK确认
//                    stringRedisTemplate.opsForStream().acknowledge(queneName,"g1", entries.getId());
//                } catch (Exception e) {
//                    log.error("订单异常");
//                    handlePendingList();
//                }
//
//
//            }
//
//
//            }
//
//        private void handlePendingList() throws InterruptedException {
//            while (true) {
//                try {
//                    //1、获取pendinglist中的订单信息，XREADGRUOUP GRUOP g1 c1 COUNT 1 BLOCK 2000 STREAMS steams.order >
//                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
//                            Consumer.from("g1", "c1"),
//                            StreamReadOptions.empty().count(1),
//                            StreamOffset.create(queneName, ReadOffset.from("0"))
//                    );
//                    //2、判断消息是否存在
//                    if(list == null || list.isEmpty()) {
//                        break;
//                    }
//                    //3、解析消息中的订单信息
//                    MapRecord<String, Object, Object> entries = list.get(0);
//                    Map<Object, Object> value = entries.getValue();
//                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
//                    //4、获取成功，可以下单
//                    handleVoucherOrder(voucherOrder);
//                    //5、ACK确认
//                    stringRedisTemplate.opsForStream().acknowledge(queneName,"g1", entries.getId());
//                } catch (Exception e) {
//                    log.error("订单异常");
//                    Thread.sleep(200);
//                }
//            }
//
//        }
//
//    }
//
//        private void handleVoucherOrder(VoucherOrder voucherOrder) {
//            Long userId = voucherOrder.getUserId();
//            RLock lock = redissonClient.getLock("lock:order:" + userId);
//            boolean isLocked = lock.tryLock();
//            if (!isLocked) {
//                log.error("不允许重复下单！");
//                return;
//            }
//            try {
//                // 该方法非主线程调用，代理对象需要在主线程中获取。
//                currentProxy.createVoucherOrder(voucherOrder);
//            } finally {
//                lock.unlock();
//            }
//        }


//    private class VoucherOrderHandle implements Runnable {
//        @Override
//        public void run() {
//            while (true) {
//                try {
//                    //获取订单信息
//                    VoucherOrder voucherOrder = orderTasks.take();
//                    handleVoucherOrder(voucherOrder);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//            }
//
//        }
//
//        private void handleVoucherOrder(VoucherOrder voucherOrder) {
//            Long userId = voucherOrder.getUserId();
//            RLock lock = redissonClient.getLock("lock:order:" + userId);
//            boolean isLocked = lock.tryLock();
//            if (!isLocked) {
//                log.error("不允许重复下单！");
//                return;
//            }
//            try {
//                // 该方法非主线程调用，代理对象需要在主线程中获取。
//                currentProxy.createVoucherOrder(voucherOrder);
//            } finally {
//                lock.unlock();
//            }
//        }
//    }

    // 代理对象
    private IVoucherOrderService currentProxy;

    @Override
    public Result seckillVoucher(Long voucherId) {
        //1、获取用户
        Long userId = UserHolder.getUser().getId();
        //获取订单信息
        Long orderId = redisIdWorker.nextId("order");
        //1、执行Lua脚本，判断资格
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(),String.valueOf(orderId)
        );
        int r = result.intValue();
        //2、判断结果是否为0
        if (r != 0) {
            //2.2 不为0  没有下单资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        //2.1 为0,有购买资格，把下单信息保存到阻塞队列中。
//        //得到订单id
//        long orderId = redisIdWorker.nextId("order");
        //TODO 保存订单信息到阻塞队列
        VoucherOrder voucherOrder = new VoucherOrder();
        //订单id
        voucherOrder.setId(orderId);
        //用户id
        voucherOrder.setUserId(userId);
        //优惠券id
        voucherOrder.setVoucherId(voucherId);


        //3.返回订单id
        return Result.ok(orderId);
    }

    @Override
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        // 1. 一人一单
        Integer count = query()
                .eq("voucher_id", voucherOrder.getVoucherId())
                .eq("user_id", userId)
                .count();
        if (count > 0) {
            log.error("不可重复下单！");
            return;
        }

        // 2. 减扣库存
        boolean isAccomplished = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId()).gt("stock", 0)
                .update();
        if (!isAccomplished) {
            log.error("库存不足！");
            return;
        }

        // 3. 下单
        boolean isSaved = save(voucherOrder);
        if (!isSaved) {
            log.error("下单失败！");
            return;
        }
    }
}

//    @Transactional
//    public Result seckillVoucher(Long voucherId) {
//        //1、根据优惠券id来查询数据库
//        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
//        //2、判断秒杀是否开始
//        if(LocalDateTime.now().isBefore(seckillVoucher.getBeginTime())) {
//            return Result.fail("秒杀尚未开始。。。");
//        }
//        if(LocalDateTime.now().isAfter(seckillVoucher.getEndTime())) {
//            return Result.fail("秒杀已经结束。。。");
//        }
//        //3、判断库存是否充足
//        if(seckillVoucher.getStock()<1) {
//            return Result.fail("库存不足");
//        }
//        //4、扣除库存
//        boolean isSucess = seckillVoucherService.update()
//                .setSql("stock = stock -1")
//                .eq("voucher_id",voucherId).gt("stock",0)
//                .update();//在更改时要添加stock比较字段，CAS法。
//        if(!isSucess) {
//            return Result.fail("库存不足");
//        }
//       return createVoucherOrder(voucherId);
//
//    }
//    @Transactional
//    public Result createVoucherOrder(Long voucherId) {
//        //一人一单
//        //1、查询订单
//        Long userId = UserHolder.getUser().getId();
//        //创建锁对象
//        RLock redissonClientLock = redissonClient.getLock("lock:order:"+ userId);
//        boolean isLock = redissonClientLock.tryLock();
//        if(!isLock) {
//            return Result.ok("不允许重复下单");
//        }
//
//        try {
//            int count = query().eq("user_id",userId).eq("voucher_id", voucherId).count();
//            //2、判断是否存在
//            if(count>0) {
//                //重复下单
//                return Result.fail("重复下单！");
//            }
//            //5、创建订单
//            VoucherOrder voucherOrder = new VoucherOrder();
//            //订单id
//            long orderId = redisIdWorker.nextId("order");
//            //用户id
//            voucherOrder.setId(orderId);
//            voucherOrder.setUserId(userId);
//            //优惠券id
//            voucherOrder.setVoucherId(voucherId);
//            boolean isSaved = save(voucherOrder);
//            if(!isSaved) {
//                return Result.fail("下单失败");
//            }
//            return Result.ok(orderId);
//        } finally {
//                redissonClientLock.unlock();
//        }
//    }
//}
