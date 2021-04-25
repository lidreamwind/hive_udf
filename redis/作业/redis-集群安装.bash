# 安装为了偷懒，直接使用一台机器的不同端口实现（大笑）

# 创建redis集群，3主3从
	# 1、创建redis集群安装目录
	mkdir /opt/lagou/servers/redis-cluster/redis1 -p
	# 2、上传redis源码，并解压 /opt/lagou/software
	cd /opt/lagou/software
	tar -zxvf redis-5.0.5.tar.gz
	# 3、编译redis源码
	yum install -y gcc-c++		# 安装编译依赖
	cd /opt/lagou/software/redis-5.0.5/src  	# 进入源码目录
	make		# 编译
	# 4、安装，指定到/opt/lagou/servers/redis-cluster/redis1目录，若是多台机器，在多台机器上执行编译安装即可
	make install PREFIX=/opt/lagou/servers/redis-cluster/redis1
	# 5、拷贝配置文件到，/opt/lagou/servers/redis-cluster/redis1/bin中
	cp /opt/lagou/software/redis-5.0.5/redis.conf /opt/lagou/servers/redis-cluster/redis1/bin/
	# 6、修改配置文件
	cd /opt/lagou/servers/redis-cluster/redis1/bin/ 	# 进入redis安装目录
	vi redis.conf # 编辑配置文件，并按照如内容进行修改
		# <-----------------redis.conf 文件中需要修改的内容 ------------------>
			# bind 127.0.0.1  # 注释掉此行数据
			port 7001  	# 端口由6379改成7001
			daemonize yes	# 默认值是no改成yes
			cluster-enabled yes  # 默认此条记录是注释掉的，需要打开
			protected-mode no 	# 默认值时es，不要改成no
		# <-----------------redis.conf 文件中需要修改的内容 ------------------>
	# 7、拷贝/opt/lagou/servers/redis-cluster/redis1目录为redis2,redis3,redis4,redis5,redis6
	cp -r redis1 redis2		# 修改redis.conf为7002，若是其他机器则不用修改
	cp -r redis1 redis3		# 修改redis.conf为7003，若是其他机器则不用修改
	cp -r redis1 redis4		# 修改redis.conf为7004，若是其他机器则不用修改
	cp -r redis1 redis5		# 修改redis.conf为7005，若是其他机器则不用修改
	cp -r redis1 redis6		# 修改redis.conf为7006，若是其他机器则不用修改
	# 7、一件启动脚本
	cd /opt/lagou/servers/  	# 进入目录
	vi redis-start.sh
		# <----------------- redis-start.sh启动脚本的内容 ------------------>
			#!/usr/bin
			# start redis1
			cd  /opt/lagou/servers/redis-cluster/redis1/bin/
			./redis-server redis.conf

			# start redis2
			cd  /opt/lagou/servers/redis-cluster/redis2/bin/
			./redis-server redis.conf

			# start redis3
			cd  /opt/lagou/servers/redis-cluster/redis3/bin/
			./redis-server redis.conf

			# start redis4
			cd  /opt/lagou/servers/redis-cluster/redis4/bin/
			./redis-server redis.conf

			# start redis5
			cd  /opt/lagou/servers/redis-cluster/redis5/bin/
			./redis-server redis.conf

			# start redis6
			cd  /opt/lagou/servers/redis-cluster/redis6/bin/
			./redis-server redis.conf
		# <----------------- redis-start.sh启动脚本的内容 ------------------>
	
	# 8、创建集群
	sh /opt/lagou/servers/redis-start.sh	# 启动集群
	cd /opt/lagou/servers/redis-cluster/redis1/bin    # 进入安装目录，创建集群
	./redis-cli --cluster create 192.168.233.166:7001 192.168.233.166:7002 192.168.233.166:7003 192.168.233.166:7004 192.168.233.166:7005 192.168.233.166:7006 --cluster-replicas 1
	# 9、连接集群
	./redis-cli -h 127.0.0.1 -p 7001 -c
	
# 任务二：添加一主一从
	# 1、创建redis集群安装目录
	mkdir /opt/lagou/servers/redis-cluster/redis7 -p
	mkdir /opt/lagou/servers/redis-cluster/redis8 -p
	# 2、上传redis源码，并解压 /opt/lagou/software
	cd /opt/lagou/software
	tar -zxvf redis-5.0.5.tar.gz
	# 3、编译redis源码
	yum install -y gcc-c++		# 安装编译依赖
	cd /opt/lagou/software/redis-5.0.5/src  	# 进入源码目录
	make		# 编译
	# 4、安装，指定到/opt/lagou/servers/redis-cluster/redis1目录，若是多台机器，在多台机器上执行编译安装即可
	make install PREFIX=/opt/lagou/servers/redis-cluster/redis7
	make install PREFIX=/opt/lagou/servers/redis-cluster/redis8
	# 6、修改配置文件
	cd /opt/lagou/servers/redis-cluster/redis7/bin/ 	# 进入redis安装目录
	vi redis.conf # 编辑配置文件，并按照如内容进行修改
		# <-----------------redis.conf 文件中需要修改的内容 ------------------>
			# bind 127.0.0.1  # 注释掉此行数据
			port 7007  	# 端口由6379改成7001
			daemonize yes	# 默认值是no改成yes
			cluster-enabled yes  # 默认此条记录是注释掉的，需要打开
			protected-mode no 	# 默认值时es，不要改成no
		# <-----------------redis.conf 文件中需要修改的内容 ------------------>
	# 同理修改redis8中的redis.conf配置文件中端口为7008
	
	# 7、添加节点到集群
	# 启动节点
		# start redis7
			cd  /opt/lagou/servers/redis-cluster/redis7/bin/
			./redis-server redis.conf

			# start redis8
			cd  /opt/lagou/servers/redis-cluster/redis8/bin/
			./redis-server redis.conf
	./redis-cli --cluster add-node 192.168.233.166:7007 192.168.233.166:7001 		# 添加节点7007
	./redis-cli --cluster reshard 192.168.233.168:7007			# 对新节点重新分配数据槽
	# 给新节点增加slave， ./redis-cli --cluster add-node 新节点的ip和端口  旧节点ip和端口 --cluster-slave --cluster-master-id 主节点id
	./redis-cli --cluster add-node 192.168.233.166:7008 192.168.233.166:7007 --cluster-slave --cluster-master-id 52ca3adcbd32f9551cee06a99f9d1fdeb3619d79