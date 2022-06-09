package xyz.solidspoon.lockcore;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "xyz.solidspoon")
@MapperScan("xyz.solidspoon.lockcore.dao")
// TODO 这注解干啥的来着？
public class LockCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(LockCoreApplication.class, args);
    }

}
