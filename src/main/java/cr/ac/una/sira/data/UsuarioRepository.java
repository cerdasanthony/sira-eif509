package cr.ac.una.sira.data;

import java.util.Optional;

public interface UsuarioRepository extends RepositorioJpaBase<Usuario, Long> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);
}
