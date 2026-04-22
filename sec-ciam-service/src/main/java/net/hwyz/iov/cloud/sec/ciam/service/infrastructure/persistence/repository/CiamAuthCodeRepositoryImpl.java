package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.AuthCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamAuthCodeRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.AuthCodePoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamAuthCodeMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.AuthCodePo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamAuthCodeRepositoryImpl implements CiamAuthCodeRepository {

    private final CiamAuthCodeMapper mapper;
    private final AuthCodePoConverter poConverter;

    @Override
    public Optional<AuthCode> findByCodeHash(String codeHash) {
        AuthCodePo po = mapper.selectOne(
                new LambdaQueryWrapper<AuthCodePo>()
                        .eq(AuthCodePo::getCodeHash, codeHash));
        return Optional.ofNullable(poConverter.toDomain(po));
    }

    @Override
    public Optional<AuthCode> findByAuthCodeId(String authCodeId) {
        AuthCodePo po = mapper.selectOne(
                new LambdaQueryWrapper<AuthCodePo>()
                        .eq(AuthCodePo::getAuthCodeId, authCodeId));
        return Optional.ofNullable(poConverter.toDomain(po));
    }

    @Override
    public int insert(AuthCode entity) {
        return mapper.insert(poConverter.toPo(entity));
    }

    @Override
    public int updateByAuthCodeId(AuthCode entity) {
        return mapper.update(poConverter.toPo(entity),
                new LambdaUpdateWrapper<AuthCodePo>()
                        .eq(AuthCodePo::getAuthCodeId, entity.getAuthCodeId()));
    }
}
