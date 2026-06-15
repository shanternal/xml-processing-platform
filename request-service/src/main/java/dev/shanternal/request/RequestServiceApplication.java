package dev.shanternal.request;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class RequestServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RequestServiceApplication.class, args);
	}

}
