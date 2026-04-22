package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamAuthCodeRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.CiamAuthCodeMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.AuthCodePo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamAuthCodeRepositoryImpl implements CiamAuthCodeRepository {

    private final CiamAuthCodeMapper mapper;

    @Override
    public Optional<AuthCodePo> findByCodeHash(String codeHash) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<AuthCodePo>()
                        .eq(AuthCodePo::getCodeHash, codeHash)));
    }

    @Override
    public Optional<AuthCodePo> findByAuthCodeId(String authCodeId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<AuthCodePo>()
                        .eq(AuthCodePo::getAuthCodeId, authCodeId)));
    }

    @Override
    public int insert(AuthCodePo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByAuthCodeId(AuthCodePo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<AuthCodePo>()
                        .eq(AuthCodePo::getAuthCodeId, entity.getAuthCodeId()));
    }
}
