package com.example.optimistic_lock.mapper;

import com.example.optimistic_lock.entity.TBalance;
import com.example.optimistic_lock.entity.TBalanceExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TBalanceMapper {
    long countByExample(TBalanceExample example);

    int deleteByExample(TBalanceExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(TBalance record);

    int insertSelective(TBalance record);

    List<TBalance> selectByExample(TBalanceExample example);

    TBalance selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") TBalance record, @Param("example") TBalanceExample example);

    int updateByExample(@Param("record") TBalance record, @Param("example") TBalanceExample example);

    int updateByPrimaryKeySelective(TBalance record);

    int updateByPrimaryKey(TBalance record);
}