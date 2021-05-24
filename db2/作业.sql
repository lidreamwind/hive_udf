--1、用拉链表实现核心交易分析中DIM层商家维表，并实现该拉链表的回滚
    -- 1） 构造数据
    create database dim;
    -- 1.1) 商家表  bid,name,category,joindt，ods层-用来加载当天数据
    create table ods.ods_trade_shop_buisses(
        bid string,
        name string,
        category string,
        joindt string
    ) row format   delimited
    fields terminated by ",";
    -- 1.2）加载数据
    load data local inpath '/root/task_2.date' overwrite into table ods.ods_trade_shop_buisses;
        -- 数据
--             1,用友,财务,2021-05-20
--             2,金蝶,财务,2021-05-20
--             3,远光,财务,2021-05-20
--             4,天大天财,财务,2021-05-20
--             5,aaaa,bb,2021-05-20
    -- 1.3） 加载数据到dim层
    create table dim.ods_trade_shop_buisses(
        bid string,
        name string,
        category string,
        start_date string,
        end_date string
    );
    -- 1.4）数据初始化到维度表
    insert overwrite table dim.ods_trade_shop_buisses select bid,name,category,joindt,'9999-12-31' from ods.ods_trade_shop_buisses;
    select * from dim.ods_trade_shop_buisses cluster by bid;
    -- 1.5) 加载增量数据
    load data local inpath '/root/task_2_incr.date' overwrite into table ods.ods_trade_shop_buisses;
    -- 数据
--             1,用友,金融,2021-05-21
--             3,远光,金融,2021-05-21
--             4,天大天财,电力,2021-05-21
--             6,拉钩,互联网,2021-05-21
--             7,百度,互联网,2021-05-21
    --）1.6 同步到拉链表
    insert overwrite  table dim.ods_trade_shop_buisses
    select bid,name,category,joindt,'9999-12-31' from ods.ods_trade_shop_buisses
    union  all
    select A.bid,A.name, A.category,
           A.start_date,case when joindt is null then a.end_date else date_add(joindt,-1) end  from dim.ods_trade_shop_buisses A left join ods.ods_trade_shop_buisses B on A.bid=B.bid;
    --) 1.7 回退到2021-05-20日
    create table dim.tmp as select * from dim.ods_trade_shop_buisses where 1=2;
    insert overwrite table dim.tmp
    select bid,name,category,start_date,case when end_date>='2021-05-20' then '9999-12-31' else end_date end end_date
    from dim.ods_trade_shop_buisses where start_date<= '2021-05-20';

    select* from dim.tmp;
    select * from dim.ods_trade_shop_buisses cluster by bid;

--2、在会员分析中计算沉默会员数和流失会员数
    -- 2.1）沉默会员数---只有安装当天启动过APP，而且安装时间是在7天前
        -- 隐含了两个信息，a：只有一天访问过；b：是在7天之前访问过
    select uid,dt
    from
         -- 查询只有一天访问过的会员数据有哪些
        (select uid,dt
            from dws.dws_member_start_day group by uid, dt having count(*)<=1
        ) t
    where dt<=date_add(`current_date`(),-7);  -- 过滤出7天之前的会员

     -- 2.2）流失会员---最近30天未访问过的会员
    --查询最近访问的一次日期，然后以最近一次访问日期衡量距离今天是否有30天
    select uid,m_dt
    from (select uid, max(dt) m_dt
          from dws.dws_member_start_day
          group by uid
         ) t
    where m_dt<=date_add(`current_date`(),-30)

--3、在核心交易分析中完成如下指标的计算
    -- 3.1 统计2020年每个季度的销售订单笔数，订单总额
    select  year,mm,sum(totalMoney) as sum
    from (
        select substr(payTime, 1, 4) as year,
               case when substr(payTime, 6, 2)=03 then "第一季度"
                    when substr(payTime, 6, 2)<=06 then "第二季度"
                    when substr(payTime, 6, 2)<=09 then "第三季度"
                    else "第四季度" end mm, totalMoney
        from ods.ods_trade_orders
        ) t
    group by year, mm;

    -- 3.2 统计2020年每个月的销售订单笔数，订单总额
    select substr(payTime,1,7)  mon_year,
           count(orderNo) total_order,
           sum(totalMoney)  total_money
    from ods.ods_trade_orders
    group by substr(payTime,1,7)
    -- 3.3 统计2020年每周（周一到周日）的销售订单笔数、订单总额
    -- 后边两道题的思路是，构建一张日期表，工作繁琐，，就不做了，然后所有问题就迎刃而解了。
    -- 3.4 统计2020年国家法定节假日、休息日、工作日的订单笔数、订单总额

