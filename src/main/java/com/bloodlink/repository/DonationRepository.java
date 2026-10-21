package com.bloodlink.repository;

import com.bloodlink.model.*;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DonationRepository extends JpaRepository<Donation, Long> {
    List<Donation> findByResponseDonorIdOrderByConfirmedAtDesc(Long donorId);
    List<Donation> findByResponseRequestHospitalIdOrderByConfirmedAtDesc(Long hospitalId);
}

