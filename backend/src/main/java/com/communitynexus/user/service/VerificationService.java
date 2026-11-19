package com.communitynexus.user.service;

import com.communitynexus.common.exception.NexusException;
import com.communitynexus.user.dto.VerificationResponse;
import com.communitynexus.user.entity.Verification;
import com.communitynexus.user.entity.VerificationStatus;
import com.communitynexus.user.repo.VerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationRepository verificationRepo;

    @Transactional(readOnly = true)
    public List<VerificationResponse> list(VerificationStatus status) {
        return verificationRepo.findByStatusOrderByCreatedAtAsc(status)
                .stream()
                .map(VerificationResponse::from)
                .toList();
    }

    @Transactional
    public VerificationResponse approve(Long verificationId, Long adminAccountId) {
        Verification v = load(verificationId);
        v.approve(adminAccountId);          // also sets profile.verified = true
        log.info("Verification {} approved by admin {}", verificationId, adminAccountId);
        return VerificationResponse.from(v);
    }

    @Transactional
    public VerificationResponse reject(Long verificationId, Long adminAccountId, String note) {
        Verification v = load(verificationId);
        v.reject(adminAccountId, note.trim());
        log.info("Verification {} rejected by admin {}", verificationId, adminAccountId);
        return VerificationResponse.from(v);
    }

    private Verification load(Long id) {
        return verificationRepo.findById(id)
                .orElseThrow(() -> NexusException.notFound("Verification"));
    }
}