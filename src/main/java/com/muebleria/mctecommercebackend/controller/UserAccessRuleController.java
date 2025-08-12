package com.muebleria.mctecommercebackend.controller;

import com.muebleria.mctecommercebackend.dto.UserAccessRuleDTO;
import com.muebleria.mctecommercebackend.service.UserAccessRuleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/rules")
public class UserAccessRuleController {

    private final UserAccessRuleService ruleService;

    @Autowired
    public UserAccessRuleController(UserAccessRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<UserAccessRuleDTO> createRule(@PathVariable Long userId, @Valid @RequestBody UserAccessRuleDTO ruleDTO) {
        UserAccessRuleDTO createdRule = ruleService.createRuleForUser(userId, ruleDTO);
        return new ResponseEntity<>(createdRule, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<List<UserAccessRuleDTO>> getRulesForUser(@PathVariable Long userId) {
        List<UserAccessRuleDTO> rules = ruleService.getRulesByUserId(userId);
        return ResponseEntity.ok(rules);
    }

    @PutMapping("/{ruleId}")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<UserAccessRuleDTO> updateRule(@PathVariable Long userId, @PathVariable Long ruleId, @Valid @RequestBody UserAccessRuleDTO ruleDTO) {
        UserAccessRuleDTO updatedRule = ruleService.updateRule(ruleId, ruleDTO);
        return ResponseEntity.ok(updatedRule);
    }

    @DeleteMapping("/{ruleId}")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<Void> deleteRule(@PathVariable Long userId, @PathVariable Long ruleId) {
        ruleService.deleteRule(ruleId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}