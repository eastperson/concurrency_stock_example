package com.concurrency.stock.facade;

import com.concurrency.stock.repository.RedisLockRepository;
import com.concurrency.stock.service.StockService;
import org.springframework.stereotype.Component;

import javax.persistence.Id;

@Component
public class LettuceLockStockFacade {

    private RedisLockRepository redisLockRepository;

    private StockService stockService;

    public LettuceLockStockFacade(RedisLockRepository redisLockRepository, StockService stockService) {
        this.redisLockRepository = redisLockRepository;
        this.stockService = stockService;
    }

    public void decrease(Long key, Long quantity) throws InterruptedException {

        // lock 획득 시도, sleep 으로 레디스에 가는 부하를 줄여준다.
        while (!redisLockRepository.lock(key)) {
            Thread.sleep(100);
        }

        try {
            stockService.decrease(key, quantity);
        } finally {
            // 락 해제
            redisLockRepository.unlock(key);
        }
    }
}
