package xyz.solidspoon.lockcore.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class Message implements Serializable {
    private String id;
    private List<LockStoreDTO> lockStockDTOs;
    private Boolean isSuccess;
    private String errorMessage;

    public List<String> getOrderIds() {
        return lockStockDTOs.stream().map(LockStoreDTO::getOrderSn).collect(Collectors.toList());
    }
}
