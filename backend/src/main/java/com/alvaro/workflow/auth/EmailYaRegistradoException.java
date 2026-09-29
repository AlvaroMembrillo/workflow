package com.alvaro.workflow.auth;

public class EmailYaRegistradoException extends RuntimeException {

	public EmailYaRegistradoException(String email) {
		super("Ya existe una cuenta con el email " + email);
	}

}
