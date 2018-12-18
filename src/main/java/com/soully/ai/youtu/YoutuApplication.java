package com.soully.ai.youtu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
public class YoutuApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(YoutuApplication.class, args);
	}

	private static final Logger logger = LoggerFactory.getLogger(YoutuApplication.class);

	@Override
	public void run(String...args) throws Exception {
		logger.info("running...");
	}
}

