package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamOAuthClientRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamOAuthClientMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.OAuthClientPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamOAuthClientRepositoryImpl implements CiamOAuthClientRepository {

    private final CiamOAuthClientMapper mapper;

    @Override
    public Optional<OAuthClientPo> findByClientId(String clientId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<OAuthClientPo>()
                        .eq(OAuthClientPo::getClientId, clientId)
                        .eq(OAuthClientPo::getRowValid, 1)));
    }

    @Override
    public List<OAuthClientPo> findByClientStatus(int clientStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<OAuthClientPo>()
                        .eq(OAuthClientPo::getClientStatus, clientStatus)
                        .eq(OAuthClientPo::getRowValid, 1));
    }

    @Override
    public int insert(OAuthClientPo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByClientId(OAuthClientPo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<OAuthClientPo>()
                        .eq(OAuthClientPo::getClientId, entity.getClientId()));
    }
}
