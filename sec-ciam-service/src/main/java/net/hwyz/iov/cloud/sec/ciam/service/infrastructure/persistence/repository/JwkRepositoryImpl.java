package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Jwk;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.JwkRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.JwkPoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamJwkMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.JwkPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JWK 密钥表仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class JwkRepositoryImpl implements JwkRepository {

    private final CiamJwkMapper mapper;

    @Override
    public Optional<Jwk> findByKeyId(String keyId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<JwkPo>()
                        .eq(JwkPo::getKeyId, keyId)))
                .map(JwkPoConverter.INSTANCE::toDomain);
    }

    @Override
    public Optional<Jwk> findPrimary() {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<JwkPo>()
                        .eq(JwkPo::getIsPrimary, 1)
                        .eq(JwkPo::getStatus, 1)
                        .orderByDesc(JwkPo::getIssueTime)
                        .last("LIMIT 1")))
                .map(JwkPoConverter.INSTANCE::toDomain);
    }

    @Override
    public List<Jwk> findAllActive() {
        return mapper.selectList(
                new LambdaQueryWrapper<JwkPo>()
                        .eq(JwkPo::getStatus, 1))
                .stream()
                .map(JwkPoConverter.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int insert(Jwk entity) {
        return mapper.insert(JwkPoConverter.INSTANCE.toPo(entity));
    }

    @Override
    public int update(Jwk entity) {
        return mapper.update(JwkPoConverter.INSTANCE.toPo(entity),
                new LambdaUpdateWrapper<JwkPo>()
                        .eq(JwkPo::getKeyId, entity.getKeyId()));
    }

    @Override
    public int revokePrimary() {
        JwkPo update = new JwkPo();
        update.setIsPrimary(0);
        return mapper.update(update,
                new LambdaUpdateWrapper<JwkPo>()
                        .eq(JwkPo::getIsPrimary, 1));
    }
}
