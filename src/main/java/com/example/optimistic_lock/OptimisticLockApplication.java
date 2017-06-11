package com.example.optimistic_lock;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.optimistic_lock.mapper")
public class OptimisticLockApplication {

	public static void main(String[] args) {
		SpringApplication.run(OptimisticLockApplication.class, args);
	}
}
