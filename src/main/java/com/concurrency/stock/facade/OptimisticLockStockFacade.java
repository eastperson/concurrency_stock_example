package com.concurrency.stock.facade;

import com.concurrency.stock.service.OptimisticLockStockService;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class OptimisticLockStockFacade {

    private OptimisticLockStockService optimisticLockStockService;

    public OptimisticLockStockFacade(OptimisticLockStockService optimisticLockStockService) {
        this.optimisticLockStockService = optimisticLockStockService;
    }

    public void decrease(Long id, Long quantity) throws InterruptedException {
        while(true) {
            try {
                optimisticLockStockService.decrease(id, quantity);
                break;
            } catch (Exception e) {
                Thread.sleep(1000);
            }
        }
    }
}
