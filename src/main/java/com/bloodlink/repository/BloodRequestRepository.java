package com.bloodlink.repository;

import com.bloodlink.model.*;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {
    @Query("select r.id from BloodRequest r where r.status in (com.bloodlink.model.RequestStatus.OPEN, com.bloodlink.model.RequestStatus.IN_PROGRESS) and r.requiredBy <= :now")
    List<Long> expiredCandidateIds(@Param("now") java.time.LocalDateTime now);
    List<BloodRequest> findAllByOrderByCreatedAtDesc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from BloodRequest r where r.id = :id")
    Optional<BloodRequest> findLockedById(@Param("id") Long id);
}
