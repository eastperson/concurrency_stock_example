package com.concurrency.stock.service;

import com.concurrency.stock.domain.Stock;
import com.concurrency.stock.facade.LettuceLockStockFacade;
import com.concurrency.stock.facade.NamedLockStockFacade;
import com.concurrency.stock.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LettuceLockStockServiceTest {

    @Autowired
    private LettuceLockStockFacade lettuceLockStockFacade;

    @Autowired
    private StockService stockService;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void before() {
        Stock stock = new Stock(1L, 100L);

        stockRepository.saveAndFlush(stock);
    }

    @AfterEach
    void after() {
        stockRepository.deleteAll();
    }

    @Test
    void stock_decrease() throws InterruptedException {
        stockService.decrease(1L, 1L);

        Stock stock = stockRepository.findById(1L).orElseThrow();

        assertThat(stock.getQuantity()).isEqualTo(99);
    }

    // 동시에 여러 요청이 들어오면 어떻게 될까?
    @Test
    void 동시에_100개_요청_멀티_쓰레드() throws InterruptedException {
        int threadCount = 100;

        // ExecutorService 는 비동기로 실행하는 작업을 단순화하게 도와주는 자바의 API
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        // 100개의 요청이 끝날때 까지 기다리게 도와주는
        // 다른 스레드에서 수행중인 작업이 끝날때까지 기다려주는 클래스
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    lettuceLockStockFacade.decrease(1L, 1L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Stock stock = stockRepository.findById(1L).orElseThrow();

        // [race condition] 발생
        // 둘 이상의 쓰레드가 공유된 자원에 access 할 수 있고 동시에 변경을 하려고 할 때 발생하는 문제이다.
        // 가령 5의 재고를 thread-1과 thread-2가 동시에 작업을 할 때 thread-1이 완료되지 않았을 때 thread-2가 select 쿼리를 날려 둘다 5인 재고 상태로 가져온다
        // 그 경우 thread-1이 1개의 재고를 빼고 저장을 하더라도 thread-2는 5인 상태로 작업을 하고 있으므로 또다시 1개의 재고를 빼고 작업을 하여
        // 최종 재고가 3이 아닌 4가로 저장이 되게 된다.
        // 하나의 스레드가 작업이 완료된 후에 다른 스레드가 접근할 수 있도록 변경해야한
        assertThat(stock.getQuantity()).isEqualTo(0);
    }
}