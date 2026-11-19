package com.communitynexus.user.dto;

import com.communitynexus.user.entity.Address;
import com.communitynexus.user.entity.AddressLabel;

public record AddressResponse(
        Long id,
        AddressLabel label,
        String line,
        Double lat,
        Double lng,
        boolean isDefault
) {
    public static AddressResponse from(Address a) {
        return new AddressResponse(a.getId(), a.getLabel(), a.getLine(), a.getLat(), a.getLng(), a.isDefaultAddress());
    }
}