package com.studypartner.controller;

import com.studypartner.entity.CaoCourse;
import com.studypartner.entity.CaoPointsHistory;
import com.studypartner.service.CaoCourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/cao")
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:8082"})
public class CaoCourseController {
    
    private final CaoCourseService caoCourseService;
    
    @Autowired
    public CaoCourseController(CaoCourseService caoCourseService) {
        this.caoCourseService = caoCourseService;
    }
    
    @GetMapping("/courses")
    public ResponseEntity<Page<CaoCourse>> getAllCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "courseName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<CaoCourse> courses = caoCourseService.getAllCourses(pageable);
        return ResponseEntity.ok(courses);
    }
    
    @GetMapping("/courses/search")
    public ResponseEntity<Page<CaoCourse>> searchCourses(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "courseName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<CaoCourse> courses = caoCourseService.searchCourses(q, pageable);
        return ResponseEntity.ok(courses);
    }
    
    @GetMapping("/courses/{caoCode}")
    public ResponseEntity<CaoCourse> getCourseByCode(@PathVariable String caoCode) {
        Optional<CaoCourse> course = caoCourseService.getCourseByCode(caoCode);
        return course.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/courses/institution/{institution}")
    public ResponseEntity<Page<CaoCourse>> getCoursesByInstitution(
            @PathVariable String institution,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("courseName"));
        Page<CaoCourse> courses = caoCourseService.getCoursesByInstitution(institution, pageable);
        return ResponseEntity.ok(courses);
    }
    
    @GetMapping("/courses/category/{category}")
    public ResponseEntity<Page<CaoCourse>> getCoursesByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("courseName"));
        Page<CaoCourse> courses = caoCourseService.getCoursesByCategory(category, pageable);
        return ResponseEntity.ok(courses);
    }
    
    @GetMapping("/courses/nfq/{level}")
    public ResponseEntity<Page<CaoCourse>> getCoursesByNfqLevel(
            @PathVariable Integer level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("courseName"));
        Page<CaoCourse> courses = caoCourseService.getCoursesByNfqLevel(level, pageable);
        return ResponseEntity.ok(courses);
    }
    
    @GetMapping("/courses/{caoCode}/points-history")
    public ResponseEntity<List<CaoPointsHistory>> getPointsHistory(@PathVariable String caoCode) {
        List<CaoPointsHistory> history = caoCourseService.getPointsHistory(caoCode);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/courses/{caoCode}/points-history/{startYear}/{endYear}")
    public ResponseEntity<List<CaoPointsHistory>> getPointsHistoryByYearRange(
            @PathVariable String caoCode,
            @PathVariable Integer startYear,
            @PathVariable Integer endYear) {
        
        List<CaoPointsHistory> history = caoCourseService.getPointsHistoryByYearRange(caoCode, startYear, endYear);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/institutions")
    public ResponseEntity<List<String>> getAllInstitutions() {
        List<String> institutions = caoCourseService.getAllInstitutions();
        return ResponseEntity.ok(institutions);
    }
    
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        List<String> categories = caoCourseService.getAllCategories();
        return ResponseEntity.ok(categories);
    }
    
    @GetMapping("/nfq-levels")
    public ResponseEntity<List<Integer>> getAllNfqLevels() {
        List<Integer> levels = caoCourseService.getAllNfqLevels();
        return ResponseEntity.ok(levels);
    }
    
    @GetMapping("/years")
    public ResponseEntity<List<Integer>> getAllYears() {
        List<Integer> years = caoCourseService.getAllYears();
        return ResponseEntity.ok(years);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCourses", caoCourseService.getTotalCourses());
        stats.put("institutions", caoCourseService.getAllInstitutions().size());
        stats.put("categories", caoCourseService.getAllCategories().size());
        stats.put("nfqLevels", caoCourseService.getAllNfqLevels());
        stats.put("availableYears", caoCourseService.getAllYears());
        return ResponseEntity.ok(stats);
    }
}