package com.alvaro.workflow;

import org.springframework.boot.SpringApplication;

public class TestWorkflowApplication {

	public static void main(String[] args) {
		SpringApplication.from(WorkflowApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
