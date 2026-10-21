package com.bloodlink.repository;

import com.bloodlink.model.*;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DonorResponseRepository extends JpaRepository<DonorResponse, Long> {
    Optional<DonorResponse> findByRequestIdAndDonorId(Long requestId, Long donorId);
    List<DonorResponse> findByRequestIdOrderByRespondedAtAsc(Long requestId);
    List<DonorResponse> findByDonorIdOrderByRespondedAtDesc(Long donorId);
}

