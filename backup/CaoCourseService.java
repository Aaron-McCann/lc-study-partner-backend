package com.studypartner.service;

import com.studypartner.entity.CaoCourse;
import com.studypartner.entity.CaoPointsHistory;
import com.studypartner.repository.CaoCourseRepository;
import com.studypartner.repository.CaoPointsHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CaoCourseService {
    
    private final CaoCourseRepository caoCourseRepository;
    private final CaoPointsHistoryRepository pointsHistoryRepository;
    
    @Autowired
    public CaoCourseService(CaoCourseRepository caoCourseRepository, 
                           CaoPointsHistoryRepository pointsHistoryRepository) {
        this.caoCourseRepository = caoCourseRepository;
        this.pointsHistoryRepository = pointsHistoryRepository;
    }
    
    public Page<CaoCourse> getAllCourses(Pageable pageable) {
        return caoCourseRepository.findAll(pageable);
    }
    
    public Optional<CaoCourse> getCourseByCode(String caoCode) {
        return caoCourseRepository.findById(caoCode);
    }
    
    public Page<CaoCourse> searchCourses(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return caoCourseRepository.findAll(pageable);
        }
        return caoCourseRepository.findBySearchTerm(searchTerm.trim(), pageable);
    }
    
    public Page<CaoCourse> getCoursesByInstitution(String institution, Pageable pageable) {
        return caoCourseRepository.findByInstitutionContainingIgnoreCase(institution, pageable);
    }
    
    public Page<CaoCourse> getCoursesByCategory(String category, Pageable pageable) {
        return caoCourseRepository.findByCategoriesContainingIgnoreCase(category, pageable);
    }
    
    public Page<CaoCourse> getCoursesByNfqLevel(Integer nfqLevel, Pageable pageable) {
        return caoCourseRepository.findByNfqLevel(nfqLevel, pageable);
    }
    
    public List<CaoPointsHistory> getPointsHistory(String caoCode) {
        return pointsHistoryRepository.findByCaoCourseCaoCodeOrderByYearDesc(caoCode);
    }
    
    public List<CaoPointsHistory> getPointsHistoryByYearRange(String caoCode, Integer startYear, Integer endYear) {
        return pointsHistoryRepository.findByCaoCodeAndYearRange(caoCode, startYear, endYear);
    }
    
    public List<String> getAllInstitutions() {
        return caoCourseRepository.findAllInstitutions();
    }
    
    public List<String> getAllCategories() {
        return caoCourseRepository.findAllCategories();
    }
    
    public List<Integer> getAllNfqLevels() {
        return caoCourseRepository.findAllNfqLevels();
    }
    
    public List<Integer> getAllYears() {
        return pointsHistoryRepository.findAllYears();
    }
    
    public CaoCourse saveCourse(CaoCourse course) {
        return caoCourseRepository.save(course);
    }
    
    public void deleteCourse(String caoCode) {
        caoCourseRepository.deleteById(caoCode);
    }
    
    public long getTotalCourses() {
        return caoCourseRepository.count();
    }
    
    public long getCourseCountByInstitution(String institution) {
        return caoCourseRepository.countByInstitutionContainingIgnoreCase(institution);
    }
    
    public long getCourseCountByCategory(String category) {
        return caoCourseRepository.countByCategoriesContainingIgnoreCase(category);
    }
}