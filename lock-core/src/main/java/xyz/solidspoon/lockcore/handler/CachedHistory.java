package xyz.solidspoon.lockcore.handler;

import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Component
public class CachedHistory<T> implements ApplicationRunner {

    @Autowired
    private RedissonClient redissonClient;
    private RSet<Object> rSet;
    private LocalTx<T> localTx;


    // TODO LRU
    // 其他的线程安全类
    private static Set<Object> orderHistory;

    public LocalTx<T> getLocalTx() {
        return localTx;
    }


    public boolean contains(T id) {
        if (orderHistory.contains(id)) {
            return true;
        }
        return rSet.contains(id);
    }
    public boolean containsAny(Collection<T> ids) {
        for (T id : ids) {
            if (contains(id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        rSet = redissonClient.getSet("ced:orderHistory");
        orderHistory = new ConcurrentSkipListSet<>();
        localTx = new LocalTx<>(orderHistory, rSet);
    }
}
