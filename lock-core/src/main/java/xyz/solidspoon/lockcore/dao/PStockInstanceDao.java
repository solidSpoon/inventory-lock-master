package xyz.solidspoon.lockcore.dao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import javafx.util.Pair;
import xyz.solidspoon.lockcore.dto.SummarizeDTO;
import xyz.solidspoon.lockcore.entity.PStockInstance;
import org.apache.ibatis.annotations.Param;

/**
 * 即时库存表(PStockInstance)表数据库访问层
 *
 * @author makejava
 * @since 2022-05-18 11:36:48
 */
public interface PStockInstanceDao extends BaseMapper<PStockInstance> {

    /**
     * 批量新增数据（MyBatis原生foreach方法）
     *
     * @param entities List<PStockInstance> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<PStockInstance> entities);

    /**
     * 批量新增或按主键更新数据（MyBatis原生foreach方法）
     *
     * @param entities List<PStockInstance> 实例对象列表
     * @return 影响行数
     * @throws org.springframework.jdbc.BadSqlGrammarException 入参是空List的时候会抛SQL语句错误的异常，请自行校验入参
     */
    int insertOrUpdateBatch(@Param("entities") List<PStockInstance> entities);

    List<PStockInstance> listLockable(@Param("skus") Set<String> skus, @Param("limit") Integer limit, @Param("offset") Integer offset);

    void createTempTable();

    void insertTemp(@Param("operationMapping") Map<String, Integer> operationMapping);

    void updateStock();

    void dropTempTable();

    Boolean isSuccess();

    Boolean operationStockBigData(@Param("operationMapping") Map<String, Integer> operationMapping);
    Integer operationStockSmallData(@Param("operationMapping") Map<String, Integer> operationMapping);


    List<SummarizeDTO> summarize();
}

