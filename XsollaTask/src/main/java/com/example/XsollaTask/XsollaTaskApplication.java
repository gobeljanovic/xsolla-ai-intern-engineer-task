package com.example.XsollaTask;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class XsollaTaskApplication {

	public static void main(String[] args) {
		SpringApplication.run(XsollaTaskApplication.class, args);
	}

}
