package com.alvaro.workflow.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface TokenDeUnUsoRepository extends JpaRepository<TokenDeUnUso, UUID> {

	@EntityGraph(attributePaths = "usuario")
	Optional<TokenDeUnUso> findByHashAndTipo(String hash, TipoDeToken tipo);

	/**
	 * Marca el token como usado si sigue vigente. Al hacerlo en una sola sentencia, dos peticiones
	 * simultáneas con el mismo enlace no pueden usarlo las dos.
	 *
	 * @return 1 si el token era válido y ha quedado usado; 0 si no existe, ha caducado o ya se usó
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update TokenDeUnUso t set t.fechaUso = :ahora
			where t.hash = :hash and t.tipo = :tipo and t.fechaUso is null and t.fechaCaducidad > :ahora
			""")
	int usar(String hash, TipoDeToken tipo, Instant ahora);

	/** Al pedir un enlace nuevo, los anteriores del mismo tipo dejan de valer. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from TokenDeUnUso t where t.usuario.id = :usuarioId and t.tipo = :tipo")
	void borrarDelUsuario(UUID usuarioId, TipoDeToken tipo);

	@Modifying
	@Query("delete from TokenDeUnUso t where t.fechaCaducidad < :fecha")
	int borrarCaducadosAntesDe(Instant fecha);

}
