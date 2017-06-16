package com.example.optimistic_lock.service;

import com.example.optimistic_lock.entity.TBalance;
import com.example.optimistic_lock.entity.TBalanceExample;
import com.example.optimistic_lock.mapper.TBalanceMapper;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


/**
 * Created by ycwu on 2017/6/11.
 */
@Service
@Slf4j
public class BalanceService {

    public static final AtomicInteger checkCounter = new AtomicInteger(0);

    @Autowired
    private TBalanceMapper tBalanceMapper;


    @Transactional
    public void prepareBalance() {
        tBalanceMapper.deleteByExample(null);

        TBalance balance = new TBalance();
        balance.setBalance(100);
        balance.setId(1);
        balance.setVersion(1);
        tBalanceMapper.insert(balance);
    }

    @Transactional(readOnly = true)
    public TBalance getBalanceById(Integer id) {
        return tBalanceMapper.selectByPrimaryKey(id);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void addBalanceWithoutTransaction(int id, int amount) {
        addBalanceInternal(id, amount);
    }


    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void addBalanceWithReadCommitted(int id, int amount) {
        addBalanceInternal(id, amount);
    }

    /**
     * Isolation.DEFAULT of mysql is Isolation.REPEATABLE_READ
     * <p>
     * <b>will cause dead loop</b>
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void addBalanceWithRepeatableRead(int id, int amount) {
        addBalanceInternal(id, amount);
    }


    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void addBalanceInPessimisticMode(int id, int amount) {
        TBalance balance = tBalanceMapper.selectByPrimaryKey(id);
        balance.setBalance(balance.getBalance() + amount);
        tBalanceMapper.updateByPrimaryKeySelective(balance);
    }

    private void addBalanceInternal(int id, int amount) {
        TBalance balance;
        TBalance newBalance = new TBalance();
        TBalanceExample example = new TBalanceExample();
        do {
            balance = tBalanceMapper.selectByPrimaryKey(id);
            checkCounter.incrementAndGet();
            log.debug("{}", balance);

            example.clear();
            example.createCriteria().andIdEqualTo(balance.getId())
                .andVersionEqualTo(balance.getVersion());

            newBalance.setId(id);
            newBalance.setBalance(balance.getBalance() + amount);
            newBalance.setVersion(balance.getVersion() + 1);
        } while (tBalanceMapper.updateByExampleSelective(newBalance, example) == 0);
    }

    /////////////////////////////
    //
    // read balance: readonly transaction mode
    // update balance: in transactional mode, default is repeatable read
    // so it is 2 transactions in an operation, which will not bother each other.
    // but the performance is not good
    //
    /////////////////////////////

    public void addBalanceSeparatedSteps(int id, int amount) {
        TBalance balance;
        TBalance newBalance = new TBalance();
        TBalanceExample example = new TBalanceExample();
        do {
            balance = tBalanceMapper.selectByPrimaryKey(id);
            checkCounter.incrementAndGet();
            log.debug("{}", balance);

            example.clear();
            example.createCriteria().andIdEqualTo(balance.getId())
                .andVersionEqualTo(balance.getVersion());

            newBalance.setId(id);
            newBalance.setBalance(balance.getBalance() + amount);
            newBalance.setVersion(balance.getVersion() + 1);
        } while (updateBalanceByExample(newBalance, example) == 0);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int updateBalanceByExample(TBalance balance, TBalanceExample example) {
        return tBalanceMapper.updateByExampleSelective(balance, example);
    }

}
