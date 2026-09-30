package com.alvaro.workflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa las tareas programadas (@Scheduled) y las que se ejecutan en segundo plano (@Async, como el
 * envío de correo). Con varias instancias del backend habría que coordinar las programadas (por ejemplo
 * con ShedLock) para que no se ejecuten a la vez; con una sola no hace falta.
 */
@Configuration
@EnableScheduling
@EnableAsync
class TareasProgramadasConfig {
}
