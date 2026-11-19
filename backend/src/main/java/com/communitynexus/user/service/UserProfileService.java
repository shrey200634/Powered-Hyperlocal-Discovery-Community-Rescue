package com.communitynexus.user.service;

import com.communitynexus.common.exception.NexusException;
import com.communitynexus.user.dto.*;
import com.communitynexus.user.entity.Address;
import com.communitynexus.user.entity.UserProfile;
import com.communitynexus.user.repo.AddressRepository;
import com.communitynexus.user.repo.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private static final int MAX_ADDRESSES = 5;

    private final UserProfileRepository profileRepo;
    private final AddressRepository addressRepo;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long accountId) {
        return ProfileResponse.from(load(accountId));
    }

    @Transactional
    public ProfileResponse updateProfile(Long accountId, UpdateProfileRequest req) {
        UserProfile profile = load(accountId);
        profile.updateSettings(req.name(), req.language(), req.seniorMode(), req.liteMode());
        // No save() needed: the entity is "managed", Hibernate writes changes at commit (dirty checking)
        return ProfileResponse.from(profile);
    }

    @Transactional
    public AddressResponse addAddress(Long accountId, AddressRequest req) {
        UserProfile profile = load(accountId);
        List<Address> existing = addressRepo.findByProfile_IdOrderByIdAsc(profile.getId());

        if (existing.size() >= MAX_ADDRESSES) {
            throw NexusException.badRequest("ADDRESS_LIMIT", "You can save up to " + MAX_ADDRESSES + " addresses");
        }

        // First address is always default. A new default clears the old one.
        boolean makeDefault = existing.isEmpty() || Boolean.TRUE.equals(req.makeDefault());
        if (makeDefault) {
            existing.forEach(Address::clearDefault);
        }

        Address saved = addressRepo.save(
                Address.create(profile, req.label(), req.line(), req.lat(), req.lng(), makeDefault));
        return AddressResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(Long accountId) {
        UserProfile profile = load(accountId);
        return addressRepo.findByProfile_IdOrderByIdAsc(profile.getId())
                .stream()
                .map(AddressResponse::from)
                .toList();
    }

    private UserProfile load(Long accountId) {
        return profileRepo.findByAccountId(accountId)
                .orElseThrow(() -> NexusException.notFound("Profile"));
    }
}