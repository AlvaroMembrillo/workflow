package com.alvaro.workflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa las tareas programadas (@Scheduled). Con varias instancias del backend habría que coordinarlas
 * (por ejemplo con ShedLock) para que no se ejecuten a la vez; con una sola no hace falta.
 */
@Configuration
@EnableScheduling
class TareasProgramadasConfig {
}
