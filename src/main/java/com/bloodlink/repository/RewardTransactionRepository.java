package com.bloodlink.repository;

import com.bloodlink.model.*;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RewardTransactionRepository extends JpaRepository<RewardTransaction, Long> {
    @Query("select coalesce(sum(r.points), 0) from RewardTransaction r where r.donation.response.donor.id = :donorId")
    long pointsFor(@Param("donorId") Long donorId);
}

