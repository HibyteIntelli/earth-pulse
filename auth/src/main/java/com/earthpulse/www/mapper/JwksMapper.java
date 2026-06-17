package com.earthpulse.www.mapper;

import com.earthpulse.www.dto.JwkKeyDto;
import com.earthpulse.www.dto.JwksDto;
import com.nimbusds.jose.jwk.RSAKey;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface JwksMapper {

    default JwksDto toDto(RSAKey rsaKey) {
        RSAKey pub = rsaKey.toPublicJWK();
        return new JwksDto(List.of(new JwkKeyDto(
                pub.getKeyType().getValue(),
                pub.getKeyUse().identifier(),
                pub.getKeyID(),
                pub.getAlgorithm().getName(),
                pub.getModulus().toString(),
                pub.getPublicExponent().toString()
        )));
    }
}
