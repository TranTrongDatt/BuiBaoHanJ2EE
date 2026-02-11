package fit.hutech.BuiBaoHan.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.Role;

@Repository
public interface IRoleRepository extends JpaRepository<Role, Long> {

    Role findRoleById(Long id);
    
    Optional<Role> findByName(String name);
    
    boolean existsByName(String name);
}
