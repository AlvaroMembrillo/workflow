package com.alvaro.workflow.candidatura;

import com.alvaro.workflow.common.ConflictoException;

public class CandidaturaDuplicadaException extends ConflictoException {

	public CandidaturaDuplicadaException() {
		super("Candidatura duplicada", "Ya te has inscrito en esta oferta");
	}

}
