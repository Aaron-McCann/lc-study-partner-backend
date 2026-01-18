package com.studypartner.repository;

import com.studypartner.entity.CaoPointsHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaoPointsHistoryRepository extends JpaRepository<CaoPointsHistory, Long> {
    
    List<CaoPointsHistory> findByCaoCourseCaoCodeOrderByYearDesc(String caoCode);
    
    List<CaoPointsHistory> findByYearOrderByCaoCourseCaoCodeAsc(Integer year);
    
    Optional<CaoPointsHistory> findByCaoCourseCaoCodeAndYear(String caoCode, Integer year);
    
    @Query("SELECT ph FROM CaoPointsHistory ph WHERE ph.caoCourse.caoCode = :caoCode AND ph.year BETWEEN :startYear AND :endYear ORDER BY ph.year")
    List<CaoPointsHistory> findByCaoCodeAndYearRange(@Param("caoCode") String caoCode, 
                                                    @Param("startYear") Integer startYear, 
                                                    @Param("endYear") Integer endYear);
    
    @Query("SELECT DISTINCT ph.year FROM CaoPointsHistory ph ORDER BY ph.year DESC")
    List<Integer> findAllYears();
    
    @Query("SELECT AVG(ph.round1Points) FROM CaoPointsHistory ph WHERE ph.year = :year AND ph.round1Points IS NOT NULL")
    Optional<Double> getAveragePointsForYear(@Param("year") Integer year);
}