package cr.ac.una.sira.data;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface RepositorioMongoBase<T, ID> extends MongoRepository<T, ID> {
}
