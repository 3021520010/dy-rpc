package com.dy.dyrpcspringbootstarter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(scanBasePackages = {
		"com.dy.dyrpcspringbootstarter",  // 改成你实际的包路径
		// 如有其它要扫描的包也写完整
})
public class DyRpcSpringBootStarterApplication {

	public static void main(String[] args) {
		SpringApplication.run(DyRpcSpringBootStarterApplication.class, args);
	}

}
