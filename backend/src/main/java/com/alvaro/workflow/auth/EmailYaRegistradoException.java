package com.alvaro.workflow.auth;

import com.alvaro.workflow.common.ConflictoException;

public class EmailYaRegistradoException extends ConflictoException {

	public EmailYaRegistradoException(String email) {
		super("Email ya registrado", "Ya existe una cuenta con el email " + email);
	}

}
