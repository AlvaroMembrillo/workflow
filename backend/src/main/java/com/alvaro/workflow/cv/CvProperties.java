package com.alvaro.workflow.cv;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

/** @param tamanoMaximo tamaño máximo del PDF del currículum */
@Validated
@ConfigurationProperties(prefix = "app.cv")
public record CvProperties(@NotNull DataSize tamanoMaximo) {
}
