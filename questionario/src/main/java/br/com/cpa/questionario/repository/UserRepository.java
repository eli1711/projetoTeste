package br.com.cpa.questionario.repository;

import br.com.cpa.questionario.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    User findByUsername(String username);

    boolean existsByRa(String ra);   // usado no import pra evitar duplicidade
}
