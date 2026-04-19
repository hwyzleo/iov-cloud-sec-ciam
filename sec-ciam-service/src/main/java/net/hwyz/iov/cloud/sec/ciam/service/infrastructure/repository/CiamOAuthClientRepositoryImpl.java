package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamOAuthClientRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.CiamOAuthClientMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamOAuthClientDo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamOAuthClientRepositoryImpl implements CiamOAuthClientRepository {

    private final CiamOAuthClientMapper mapper;

    @Override
    public Optional<CiamOAuthClientDo> findByClientId(String clientId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamOAuthClientDo>()
                        .eq(CiamOAuthClientDo::getClientId, clientId)
                        .eq(CiamOAuthClientDo::getRowValid, 1)));
    }

    @Override
    public List<CiamOAuthClientDo> findByClientStatus(int clientStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamOAuthClientDo>()
                        .eq(CiamOAuthClientDo::getClientStatus, clientStatus)
                        .eq(CiamOAuthClientDo::getRowValid, 1));
    }

    @Override
    public int insert(CiamOAuthClientDo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByClientId(CiamOAuthClientDo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<CiamOAuthClientDo>()
                        .eq(CiamOAuthClientDo::getClientId, entity.getClientId()));
    }
}
