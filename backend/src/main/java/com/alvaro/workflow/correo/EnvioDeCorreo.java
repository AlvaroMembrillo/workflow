package com.alvaro.workflow.correo;

/** Entrega un mensaje al servidor de correo. Los tests lo sustituyen por un buzón en memoria. */
public interface EnvioDeCorreo {

	void enviar(Mensaje mensaje);

}
