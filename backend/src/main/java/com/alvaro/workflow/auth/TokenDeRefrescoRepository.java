package com.alvaro.workflow.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface TokenDeRefrescoRepository extends JpaRepository<TokenDeRefresco, UUID> {

	@EntityGraph(attributePaths = "usuario")
	Optional<TokenDeRefresco> findByHash(String hash);

	/**
	 * Canjea el token si sigue vigente y nadie lo ha canjeado antes. En una sola sentencia, para que de
	 * dos peticiones simultáneas con el mismo token solo una lo consiga.
	 *
	 * @return 1 si se ha canjeado; 0 si no existe, ha caducado, está revocado o ya se había canjeado
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update TokenDeRefresco t set t.fechaUso = :ahora
			where t.hash = :hash and t.fechaUso is null and t.fechaRevocacion is null and t.fechaCaducidad > :ahora
			""")
	int canjear(String hash, Instant ahora);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update TokenDeRefresco t set t.fechaRevocacion = :ahora where t.familia = :familia and t.fechaRevocacion is null")
	int revocarFamilia(UUID familia, Instant ahora);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update TokenDeRefresco t set t.fechaRevocacion = :ahora where t.usuario.id = :usuarioId and t.fechaRevocacion is null")
	int revocarDelUsuario(UUID usuarioId, Instant ahora);

	@Modifying
	@Query("delete from TokenDeRefresco t where t.fechaCaducidad < :fecha")
	int borrarCaducadosAntesDe(Instant fecha);

}
