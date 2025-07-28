package com.muebleria.mctecommercebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO) // Añade esta anotación
public class MctEcommerceBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MctEcommerceBackendApplication.class, args);
	}

}
