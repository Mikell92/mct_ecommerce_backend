package com.muebleria.mctecommercebackend.repository;

import com.muebleria.mctecommercebackend.model.UserAccessRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserAccessRuleRepository extends JpaRepository<UserAccessRule, Long> {

    List<UserAccessRule> findByUserId(Long userId);

    Optional<UserAccessRule> findByUserIdAndDayOfWeek(Long userId, DayOfWeek dayOfWeek);

    @Modifying
    @Query("DELETE FROM UserAccessRule uar WHERE uar.user.id = :userId")
    void deleteByUserId(Long userId);
}