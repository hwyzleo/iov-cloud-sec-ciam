package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamAuthCodeRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.CiamAuthCodeMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamAuthCodeDo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamAuthCodeRepositoryImpl implements CiamAuthCodeRepository {

    private final CiamAuthCodeMapper mapper;

    @Override
    public Optional<CiamAuthCodeDo> findByCodeHash(String codeHash) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamAuthCodeDo>()
                        .eq(CiamAuthCodeDo::getCodeHash, codeHash)));
    }

    @Override
    public Optional<CiamAuthCodeDo> findByAuthCodeId(String authCodeId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamAuthCodeDo>()
                        .eq(CiamAuthCodeDo::getAuthCodeId, authCodeId)));
    }

    @Override
    public int insert(CiamAuthCodeDo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByAuthCodeId(CiamAuthCodeDo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<CiamAuthCodeDo>()
                        .eq(CiamAuthCodeDo::getAuthCodeId, entity.getAuthCodeId()));
    }
}
