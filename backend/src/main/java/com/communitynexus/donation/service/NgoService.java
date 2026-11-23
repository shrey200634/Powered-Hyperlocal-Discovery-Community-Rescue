package com.communitynexus.donation.service;

import com.communitynexus.common.exception.NexusException;
import com.communitynexus.donation.dto.CreateNgoRequest;
import com.communitynexus.donation.dto.NgoResponse;
import com.communitynexus.donation.entity.Ngo;
import com.communitynexus.donation.geo.NgoGeoIndex;
import com.communitynexus.donation.repo.NgoRepository;
import com.communitynexus.user.UserDirectory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NgoService {

    private final NgoRepository ngoRepo;
    private final NgoGeoIndex geoIndex;
    private final UserDirectory userDirectory;

    @Transactional
    public NgoResponse create(Long accountId, CreateNgoRequest req) {
        // Doc 12: "only verified NGOs receive food"
        if (!userDirectory.isVerified(accountId)) {
            throw new NexusException(HttpStatus.FORBIDDEN, "NGO_NOT_VERIFIED",
                    "Your NGO can receive food once the Nexus team verifies you");
        }
        if (ngoRepo.existsByAccountId(accountId)) {
            throw NexusException.conflict("NGO_EXISTS", "Your NGO profile already exists");
        }

        Ngo ngo = ngoRepo.save(Ngo.create(accountId, req.name(), req.city(), req.lat(), req.lng(),
                req.capacityPerDay(), req.headcount()));
        geoIndex.addAfterCommit(ngo.getCity(), ngo.getId(), ngo.getLat(), ngo.getLng());

        log.info("NGO {} '{}' registered by account {}", ngo.getId(), ngo.getName(), accountId);
        return NgoResponse.from(ngo);
    }

    @Transactional(readOnly = true)
    public NgoResponse mine(Long accountId) {
        return NgoResponse.from(ngoRepo.findByAccountId(accountId)
                .orElseThrow(() -> NexusException.notFound("NGO profile")));
    }
}