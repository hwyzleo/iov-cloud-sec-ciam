package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamJwkRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.CiamJwkMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamJwkDo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JWK 密钥表仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class CiamJwkRepositoryImpl implements CiamJwkRepository {

    private final CiamJwkMapper mapper;

    @Override
    public Optional<CiamJwkDo> findByKeyId(String keyId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamJwkDo>()
                        .eq(CiamJwkDo::getKeyId, keyId)));
    }

    @Override
    public Optional<CiamJwkDo> findPrimary() {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamJwkDo>()
                        .eq(CiamJwkDo::getIsPrimary, 1)
                        .eq(CiamJwkDo::getStatus, 1)
                        .orderByDesc(CiamJwkDo::getIssueTime)
                        .last("LIMIT 1")));
    }

    @Override
    public List<CiamJwkDo> findAllActive() {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamJwkDo>()
                        .eq(CiamJwkDo::getStatus, 1));
    }

    @Override
    public int insert(CiamJwkDo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int update(CiamJwkDo entity) {
        entity.setModifyTime(DateTimeUtil.getNowInstant());
        return mapper.update(entity,
                new LambdaUpdateWrapper<CiamJwkDo>()
                        .eq(CiamJwkDo::getKeyId, entity.getKeyId()));
    }

    @Override
    public int revokePrimary() {
        CiamJwkDo update = new CiamJwkDo();
        update.setIsPrimary(0);
        update.setModifyTime(DateTimeUtil.getNowInstant());
        return mapper.update(update,
                new LambdaUpdateWrapper<CiamJwkDo>()
                        .eq(CiamJwkDo::getIsPrimary, 1));
    }
}
