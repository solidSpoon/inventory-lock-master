package xyz.solidspoon.lockcore.param;

import lombok.Builder;
import lombok.Getter;
import xyz.solidspoon.lockcore.entity.PStockInstanceFlow;

import java.util.List;
import java.util.Map;

@Builder
@Getter
public class AnalysisResults {
    private Map<String, Integer> operationMapping;
    List<PStockInstanceFlow> pStockInstanceFlows;
}
