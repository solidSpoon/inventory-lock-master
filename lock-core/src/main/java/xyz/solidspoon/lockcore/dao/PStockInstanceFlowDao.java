package xyz.solidspoon.lockcore.dao;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import xyz.solidspoon.lockcore.entity.PStockInstanceFlow;
import org.apache.ibatis.annotations.Param;

/**
 * 库存流水表(PStockInstanceFlow)表数据库访问层
 *
 * @author makejava
 * @since 2022-05-18 18:20:41
 */
public interface PStockInstanceFlowDao extends BaseMapper<PStockInstanceFlow> {

/**
* 批量新增数据（MyBatis原生foreach方法）
*
* @param entities List<PStockInstanceFlow> 实例对象列表
* @return 影响行数
*/
int insertBatch(@Param("entities") List<PStockInstanceFlow> entities);

/**
* 批量新增或按主键更新数据（MyBatis原生foreach方法）
*
* @param entities List<PStockInstanceFlow> 实例对象列表
* @return 影响行数
* @throws org.springframework.jdbc.BadSqlGrammarException 入参是空List的时候会抛SQL语句错误的异常，请自行校验入参
*/
int insertOrUpdateBatch(@Param("entities") List<PStockInstanceFlow> entities);

}

