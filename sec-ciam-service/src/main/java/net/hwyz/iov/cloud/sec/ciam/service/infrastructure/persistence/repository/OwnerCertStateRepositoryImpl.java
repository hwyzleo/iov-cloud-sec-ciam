package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.OwnerCertState;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.OwnerCertStateRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.OwnerCertStatePoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamOwnerCertStateMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.OwnerCertStatePo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OwnerCertStateRepositoryImpl implements OwnerCertStateRepository {

    private final CiamOwnerCertStateMapper mapper;

    @Override
    public Optional<OwnerCertState> findByOwnerCertId(String ownerCertId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<OwnerCertStatePo>()
                        .eq(OwnerCertStatePo::getOwnerCertId, ownerCertId)))
                .map(OwnerCertStatePoConverter.INSTANCE::toDomain);
    }

    @Override
    public List<OwnerCertState> findByUserIdAndCertStatus(String userId, int certStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<OwnerCertStatePo>()
                        .eq(OwnerCertStatePo::getUserId, userId)
                        .eq(OwnerCertStatePo::getCertStatus, certStatus)
                        .eq(OwnerCertStatePo::getRowValid, 1))
                .stream()
                .map(OwnerCertStatePoConverter.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OwnerCertState> findByUserId(String userId) {
        return mapper.selectList(
                new LambdaQueryWrapper<OwnerCertStatePo>()
                        .eq(OwnerCertStatePo::getUserId, userId)
                        .eq(OwnerCertStatePo::getRowValid, 1))
                .stream()
                .map(OwnerCertStatePoConverter.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int insert(OwnerCertState entity) {
        return mapper.insert(OwnerCertStatePoConverter.INSTANCE.toPo(entity));
    }

    @Override
    public int updateByOwnerCertId(OwnerCertState entity) {
        OwnerCertStatePo po = OwnerCertStatePoConverter.INSTANCE.toPo(entity);
        return mapper.update(po,
                new LambdaUpdateWrapper<OwnerCertStatePo>()
                        .eq(OwnerCertStatePo::getOwnerCertId, po.getOwnerCertId()));
    }
}
