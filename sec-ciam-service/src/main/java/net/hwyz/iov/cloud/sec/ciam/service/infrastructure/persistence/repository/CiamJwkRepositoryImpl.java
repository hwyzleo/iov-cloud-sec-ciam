package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamJwkRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamJwkMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.JwkPo;
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
    public Optional<JwkPo> findByKeyId(String keyId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<JwkPo>()
                        .eq(JwkPo::getKeyId, keyId)));
    }

    @Override
    public Optional<JwkPo> findPrimary() {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<JwkPo>()
                        .eq(JwkPo::getIsPrimary, 1)
                        .eq(JwkPo::getStatus, 1)
                        .orderByDesc(JwkPo::getIssueTime)
                        .last("LIMIT 1")));
    }

    @Override
    public List<JwkPo> findAllActive() {
        return mapper.selectList(
                new LambdaQueryWrapper<JwkPo>()
                        .eq(JwkPo::getStatus, 1));
    }

    @Override
    public int insert(JwkPo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int update(JwkPo entity) {
        entity.setModifyTime(DateTimeUtil.getNowInstant());
        return mapper.update(entity,
                new LambdaUpdateWrapper<JwkPo>()
                        .eq(JwkPo::getKeyId, entity.getKeyId()));
    }

    @Override
    public int revokePrimary() {
        JwkPo update = new JwkPo();
        update.setIsPrimary(0);
        update.setModifyTime(DateTimeUtil.getNowInstant());
        return mapper.update(update,
                new LambdaUpdateWrapper<JwkPo>()
                        .eq(JwkPo::getIsPrimary, 1));
    }
}
