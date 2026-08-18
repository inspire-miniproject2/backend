package com.gcivil.complaint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class ComplaintServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ComplaintServiceApplication.class, args);
    }
}
