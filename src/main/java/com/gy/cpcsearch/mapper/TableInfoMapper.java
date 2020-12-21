package com.gy.cpcsearch.mapper;

import com.gy.cpcsearch.entity.TableInfo;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

public interface TableInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TableInfo record);

    int insertSelective(TableInfo record);

    TableInfo selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TableInfo record);

    int updateByPrimaryKey(TableInfo record);

    String getAliasByName(@Param("name") String name);

    String getNameByAlias(@Param("alias") String alias);

    List<String> findAllAlias();

    List<String> findAllName();

    List<String> findAllAflow();

    List<TableInfo> findAllTable();

    List<Map> getAliasType();

    String getTimeFieldByName(@Param("name") String name);
}