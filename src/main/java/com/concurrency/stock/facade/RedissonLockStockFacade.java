package com.concurrency.stock.facade;

import com.concurrency.stock.service.StockService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.sql.Time;
import java.util.concurrent.TimeUnit;

@Component
public class RedissonLockStockFacade {

    private RedissonClient redissonClient;

    private StockService stockService;

    public RedissonLockStockFacade(RedissonClient redissonClient, StockService stockService) {
        this.redissonClient = redissonClient;
        this.stockService = stockService;
    }

    public void decrease(Long key, Long quantity) {
        // redis client를 통해 lock 객체를 가져온다.
        RLock lock = redissonClient.getLock(key.toString());
        try {

            // 몇초동안 lock 획득을 시도할 것인지 몇 초동안 점유할 것인지
            boolean available = lock.tryLock(10, 1, TimeUnit.SECONDS);

            // 락 획득 실패하면 로그남기고 return
            if (!available) {
                System.out.println("lock 획득 실패");
                return;
            }

            stockService.decrease(key, quantity);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}
