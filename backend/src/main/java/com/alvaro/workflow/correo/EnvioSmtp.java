package com.alvaro.workflow.correo;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class EnvioSmtp implements EnvioDeCorreo {

	private final JavaMailSender mailSender;
	private final CorreoProperties properties;

	@Override
	public void enviar(Mensaje mensaje) {
		SimpleMailMessage correo = new SimpleMailMessage();
		correo.setFrom(properties.remitente());
		correo.setTo(mensaje.para());
		correo.setSubject(mensaje.asunto());
		correo.setText(mensaje.texto());
		mailSender.send(correo);
	}

}
