package com.alvaro.workflow.correo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;

@TestConfiguration(proxyBeanMethods = false)
public class CorreoDePruebaConfiguration {

	@Bean
	@Primary
	BuzonDePrueba buzonDePrueba() {
		return new BuzonDePrueba();
	}

	/** Las tareas @Async se ejecutan en el mismo hilo: al volver la petición, el correo ya está en el buzón. */
	@Bean
	SyncTaskExecutor taskExecutor() {
		return new SyncTaskExecutor();
	}

}
