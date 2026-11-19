package com.communitynexus.user.repo;

import com.communitynexus.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {

    /** profile_Id = go into the "profile" field, then its "id". */
    List<Address> findByProfile_IdOrderByIdAsc(Long profileId);

    long countByProfile_Id(Long profileId);
}