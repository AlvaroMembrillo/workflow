package com.alvaro.workflow.moderacion;

public enum MotivoDenuncia {
	/** Parece una estafa: pide dinero, datos bancarios o que se pague por trabajar. */
	FRAUDE,
	/** Excluye por edad, sexo, origen u otra razón ilegal. */
	DISCRIMINACION,
	/** Las condiciones reales no son las que dice (salario, contrato, puesto). */
	ENGANOSA,
	OTRO
}
