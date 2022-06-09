package xyz.solidspoon.lockeureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication(scanBasePackages = "xyz.solidspoon")
@EnableEurekaServer
public class LockEurekaApplication {

	public static void main(String[] args) {
		SpringApplication.run(LockEurekaApplication.class, args);
	}

}
