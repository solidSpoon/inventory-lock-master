# 重置
update p_stock_instance
set stock_quantity   = 1,
    locking_quantity = 0
where 1;

# 库存概览
select product_sid, count(*)
from p_stock_instance
group by product_sid
order by count(*) DESC;

# 查询锁库行数
select count(*)
from p_stock_instance
where locking_quantity > 0;

