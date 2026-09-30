package com.alvaro.workflow.empresa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {

	Optional<Empresa> findByUsuarioId(UUID usuarioId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Empresa e where e.id = :id")
	void borrarPorId(UUID id);

}
