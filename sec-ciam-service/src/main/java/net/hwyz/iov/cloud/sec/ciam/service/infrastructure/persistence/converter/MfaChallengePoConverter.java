package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.MfaChallenge;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.MfaChallengePo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MfaChallengePoConverter {
    MfaChallengePoConverter INSTANCE = Mappers.getMapper(MfaChallengePoConverter.class);
    MfaChallenge toDomain(MfaChallengePo po);
    MfaChallengePo toPo(MfaChallenge domain);
}
