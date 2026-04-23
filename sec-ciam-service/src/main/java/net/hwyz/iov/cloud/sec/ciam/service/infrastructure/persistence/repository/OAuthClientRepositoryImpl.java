package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.OAuthClient;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.OAuthClientRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.OAuthClientPoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamOAuthClientMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.OAuthClientPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OAuthClientRepositoryImpl implements OAuthClientRepository {

    private final CiamOAuthClientMapper mapper;

    @Override
    public Optional<OAuthClient> findByClientId(String clientId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<OAuthClientPo>()
                        .eq(OAuthClientPo::getClientId, clientId)
                        .eq(OAuthClientPo::getRowValid, 1)))
                .map(OAuthClientPoConverter.INSTANCE::toDomain);
    }

    @Override
    public List<OAuthClient> findByClientStatus(int clientStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<OAuthClientPo>()
                        .eq(OAuthClientPo::getClientStatus, clientStatus)
                        .eq(OAuthClientPo::getRowValid, 1))
                .stream()
                .map(OAuthClientPoConverter.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int insert(OAuthClient entity) {
        return mapper.insert(OAuthClientPoConverter.INSTANCE.toPo(entity));
    }

    @Override
    public int updateByClientId(OAuthClient entity) {
        OAuthClientPo po = OAuthClientPoConverter.INSTANCE.toPo(entity);
        return mapper.update(po,
                new LambdaUpdateWrapper<OAuthClientPo>()
                        .eq(OAuthClientPo::getClientId, po.getClientId()));
    }
}
