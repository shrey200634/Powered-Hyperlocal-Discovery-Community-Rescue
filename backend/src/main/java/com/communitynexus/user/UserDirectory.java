package com.communitynexus.user;

import com.communitynexus.user.entity.UserProfile;
import com.communitynexus.user.repo.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** The User module's public door. Other modules use THIS, never the repository. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDirectory {

    private final UserProfileRepository profileRepo;

    public Optional<UserSummary> find(Long accountId) {
        return profileRepo.findByAccountId(accountId).map(UserDirectory::toSummary);
    }

    public boolean isVerified(Long accountId) {
        return profileRepo.findByAccountId(accountId).map(UserProfile::isVerified).orElse(false);
    }

    private static UserSummary toSummary(UserProfile p) {
        return new UserSummary(p.getAccountId(), p.getName(), p.getRole(),
                p.getLanguage(), p.isSeniorMode(), p.isVerified());
    }
}