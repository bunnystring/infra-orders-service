package com.infragest.infra_orders_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.infragest.infra_orders_service.client")
public class InfraOrdersServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InfraOrdersServiceApplication.class, args);
	}

}
