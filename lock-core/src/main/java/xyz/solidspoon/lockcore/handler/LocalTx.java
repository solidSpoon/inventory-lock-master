package xyz.solidspoon.lockcore.handler;

import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class LocalTx<T> {
    private final Set<Object> tempOrderHistory = new ConcurrentSkipListSet<>();
    private  Set<Object> orderHistory;

    private RSet<Object> rSet;

    public LocalTx(Set<Object> orderHistory, RSet rSet) {
        this.orderHistory = orderHistory;
        this.rSet = rSet;
    }

    public void start() {
        tempOrderHistory.clear();
    }

    public boolean addIfAbsent(T orderId) {
        if (mayContains(orderId)) {
            return false;
        }
        tempOrderHistory.add(orderId);
        return true;
    }

    public boolean addIfAbsent(Collection<T> ids) {
        long count = ids.stream().distinct().count();
        if (count != ids.size()) {
            return false;
        }

        for (T id : ids) {
            if (mayContains(id)) {
                return false;
            }
        }
        tempOrderHistory.addAll(ids);
        return true;
    }

    public void commit() {
        orderHistory.addAll(tempOrderHistory);
        rSet.addAllAsync(tempOrderHistory);
    }

    public boolean mayContains(T orderId) {
        if (orderHistory.contains(orderId)) {
            return true;
        }
        return tempOrderHistory.contains(orderId);
    }
}
