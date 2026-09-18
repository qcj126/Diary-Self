package com.executor;

import diary.config.ossconfig.OssConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * @author xuxueli 2018-10-28 00:38:13
 */
@SpringBootApplication
@MapperScan("com.executor.mapper")
@ComponentScan(basePackages = {"com.executor", "diary.common"})
@Import(OssConfig.class)
public class XxlJobExecutorApplication {
	public static void main(String[] args) {
        SpringApplication.run(XxlJobExecutorApplication.class, args);
	}

}
