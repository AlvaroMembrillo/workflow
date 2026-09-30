package com.alvaro.workflow.usuario;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

	Optional<Usuario> findByEmail(String email);

	boolean existsByEmail(String email);

	long countByRolNot(Rol rol);

	/** Cuentas que se crearon antes de la fecha y nunca han confirmado su email. */
	@Query("select u.id from Usuario u where u.emailVerificadoEn is null and u.fechaCreacion < :fecha")
	List<UUID> idsSinVerificarCreadasAntesDe(Instant fecha);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Usuario u where u.id = :id")
	void borrarPorId(UUID id);

}
