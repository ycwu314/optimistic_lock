package com.example.optimistic_lock.service;

import com.example.optimistic_lock.OptimisticLockApplicationTests;
import com.example.optimistic_lock.entity.TBalance;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * Created by ycwu on 2017/6/11.
 */
@Slf4j
public class TestOptimiscLock extends OptimisticLockApplicationTests {

    @Autowired
    private BalanceService balanceService;

    static final int threadCount = 50;

    @Before
    public void init() {
        BalanceService.checkCounter.set(0);
        balanceService.prepareBalance();
        log.info("init done");
    }


    interface InvokeMethod {
        void process();
    }

    private void doTestInternal(InvokeMethod invokeMethod) {
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        ExecutorService executorService = Executors.newCachedThreadPool();
        IntStream.range(0, threadCount).forEach(i -> {
            executorService.execute(() -> {
                invokeMethod.process();
                countDownLatch.countDown();
            });
        });

        try {
            log.info("waiting thread work done...");
            countDownLatch.await();
            executorService.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TBalance balance = balanceService.getBalanceById(1);
        log.info("{}", balance);
        log.info("check counter={}", BalanceService.checkCounter);
        Assert.assertEquals(150, balance.getBalance().intValue());

    }

    @Test
    public void testAddBalanceWithoutTransaction() {
        doTestInternal(() -> {
            balanceService.addBalanceWithoutTransaction(1, 1);
        });
    }

    /**
     * mysql > 5.1, If you are using InnoDB tables and the transaction isolation level is READ COMMITTED or READ UNCOMMITTED, only row-based logging can be used.<br>
     * <a>https://dev.mysql.com/doc/refman/5.7/en/binary-log-setting.html</a>
     * the performance is generally good for the test
     */
    @Test
    public void testAddBalanceWithReadCommitted() {
        doTestInternal(() -> {
            balanceService.addBalanceWithReadCommitted(1, 1);
        });
    }

    /**
     * RR will cause problem.<br>
     * only the 1 thread successfully update 100->101.<br>
     * others will always read 100 as in repeatable read, which will end up in dead loop
     *
     * @throws InterruptedException
     */
    @Test
    public void testAddBalanceWithRepeatableRead() throws InterruptedException {
        Thread deadLoopThread = new Thread() {
            @Override
            public void run() {
                doTestInternal(() -> {
                    balanceService.addBalanceWithRepeatableRead(1, 1);
                });
            }
        };

        deadLoopThread.start();

        /**
         * TODO change Thread.sleep() to Awaitility  <a>https://github.com/awaitility/awaitility</a>
         */
        Thread.sleep(5000L);
        log.info("check counter={}, assume dead loop, interrupted thread", BalanceService.checkCounter.get());
        deadLoopThread.interrupt();

        Assert.assertEquals(101, balanceService.getBalanceById(1).getBalance().intValue());
    }

    @Test
    public void testAddBalanceWithRepeatableReadWithFutureTask() throws InterruptedException {
        FutureTask<Boolean> task = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                doTestInternal(() -> {
                    balanceService.addBalanceWithRepeatableRead(1, 1);
                });
                return true;
            }
        });

        new Thread(task).start();

        Exception ex = null;
        try {
            task.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            ex = e;
            e.printStackTrace();
        }

        log.info("check counter={},dead loop, interrupted thread", BalanceService.checkCounter.get());
        Assert.assertNotNull(ex);
        Assert.assertEquals(101, balanceService.getBalanceById(1).getBalance().intValue());
    }


    /**
     * read balance and update balance in TWO different transactions
     */
    @Test
    public void testAddBalanceSeparatedSteps() {
        doTestInternal(() -> {
            balanceService.addBalanceSeparatedSteps(1, 1);
        });
    }

}
