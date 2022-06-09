package xyz.solidspoon.lockcore.handler;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

public class GuardedObject<T, K> {
    //受保护的对象
    T obj;
    final Lock lock = new ReentrantLock();
    final Condition done = lock.newCondition();
    final int timeout = 60;
    //保存所有GuardedObject
    final static Map<Object, GuardedObject> gos = new ConcurrentHashMap<>();

    public GuardedObject(K key) {
        this.key = key;
    }

    K key;

    //静态方法创建GuardedObject
    public static <K> GuardedObject create(K key) {
        GuardedObject go = new GuardedObject(key.toString());
        gos.put(key, go);
        return go;
    }

    public static <K, T> void fireEvent(K key, T obj) {
        GuardedObject go = gos.remove(key);
        if (go != null) {
            go.onChanged(obj);
        }
    }

    //获取受保护对象
    public Optional<T> get(Predicate<T> p) {
        lock.lock();
        Long start = System.currentTimeMillis() / 1000;
        try {
            //MESA管程推荐写法
            while (!p.test(obj)) {
                done.await(timeout, TimeUnit.SECONDS);
                if (System.currentTimeMillis() / 1000 - start >= timeout) {
                    gos.remove(key);
                    break;
                }
            }
            return Optional.ofNullable(obj);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    //事件通知方法
    void onChanged(T obj) {
        lock.lock();
        try {
            this.obj = obj;
            done.signalAll();
        } finally {
            lock.unlock();
        }
    }
}