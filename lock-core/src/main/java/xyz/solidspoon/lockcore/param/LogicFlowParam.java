package xyz.solidspoon.lockcore.param;

import lombok.Builder;
import lombok.Getter;
import xyz.solidspoon.lockcore.entity.PStockInstance;

@Getter
@Builder
public class LogicFlowParam {
    PStockInstance pStockInstance;
    Integer opQty;
}
