package com.studypartner.controller;

import com.studypartner.dto.DashboardStatsDto;
import com.studypartner.entity.User;
import com.studypartner.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8081", "http://localhost:8082"})
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        DashboardStatsDto stats = dashboardService.getUserStats(user.getId());
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/user")
    public ResponseEntity<User> getUserInfo(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/quick-actions")
    public ResponseEntity<?> getQuickActions(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        var quickActions = dashboardService.getQuickActions(user.getId());
        return ResponseEntity.ok(quickActions);
    }
}