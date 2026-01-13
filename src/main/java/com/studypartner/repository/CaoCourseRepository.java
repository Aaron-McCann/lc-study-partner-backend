package com.studypartner.repository;

import com.studypartner.entity.CaoCourse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaoCourseRepository extends JpaRepository<CaoCourse, String> {
    
    @Query("SELECT c FROM CaoCourse c WHERE " +
           "LOWER(c.courseName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.institution) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.categories) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<CaoCourse> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    Page<CaoCourse> findByInstitutionContainingIgnoreCase(String institution, Pageable pageable);
    
    Page<CaoCourse> findByCategoriesContainingIgnoreCase(String category, Pageable pageable);
    
    Page<CaoCourse> findByNfqLevel(Integer nfqLevel, Pageable pageable);
    
    @Query("SELECT DISTINCT c.institution FROM CaoCourse c ORDER BY c.institution")
    List<String> findAllInstitutions();
    
    @Query("SELECT DISTINCT c.categories FROM CaoCourse c WHERE c.categories IS NOT NULL")
    List<String> findAllCategories();
    
    @Query("SELECT DISTINCT c.nfqLevel FROM CaoCourse c WHERE c.nfqLevel IS NOT NULL ORDER BY c.nfqLevel")
    List<Integer> findAllNfqLevels();
    
    long countByInstitutionContainingIgnoreCase(String institution);
    
    long countByCategoriesContainingIgnoreCase(String category);
}