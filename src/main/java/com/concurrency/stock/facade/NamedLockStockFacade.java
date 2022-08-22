package com.concurrency.stock.facade;

import com.concurrency.stock.repository.LockRepository;
import com.concurrency.stock.service.StockService;
import org.springframework.stereotype.Component;

@Component
public class NamedLockStockFacade {

    private LockRepository lockRepository;
    private StockService stockService;

    public NamedLockStockFacade(LockRepository lockRepository, StockService stockService) {
        this.lockRepository = lockRepository;
        this.stockService = stockService;
    }

    public void decrease(Long id, Long quantity) {
        try {
            lockRepository.getLock(id.toString());
            stockService.decrease(id, quantity);
        } finally {
            lockRepository.releaseLock(id.toString());
        }
    }
}
