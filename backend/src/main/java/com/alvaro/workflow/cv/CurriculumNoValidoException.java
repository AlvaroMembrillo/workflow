package com.alvaro.workflow.cv;

import com.alvaro.workflow.common.CampoNoValidoException;

public class CurriculumNoValidoException extends CampoNoValidoException {

	public CurriculumNoValidoException(String mensaje) {
		super("fichero", mensaje);
	}

}
