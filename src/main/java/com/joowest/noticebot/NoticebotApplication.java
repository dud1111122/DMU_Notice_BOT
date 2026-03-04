package com.joowest.noticebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NoticebotApplication {

	public static void main(String[] args) {
		SpringApplication.run(NoticebotApplication.class, args);
	}

}
