package cr.ac.una.sira.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface RepositorioJpaBase<T, ID>
        extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
}
