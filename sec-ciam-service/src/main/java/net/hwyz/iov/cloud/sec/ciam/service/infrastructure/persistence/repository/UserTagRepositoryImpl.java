package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserTag;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserTagRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.UserTagPoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamUserTagMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserTagPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserTagRepositoryImpl implements UserTagRepository {

    private final CiamUserTagMapper mapper;

    @Override
    public Optional<UserTag> findByUserIdAndTagCode(String userId, String tagCode) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<UserTagPo>()
                        .eq(UserTagPo::getUserId, userId)
                        .eq(UserTagPo::getTagCode, tagCode)
                        .eq(UserTagPo::getRowValid, 1)))
                .map(UserTagPoConverter.INSTANCE::toDomain);
    }

    @Override
    public List<UserTag> findByUserId(String userId) {
        return mapper.selectList(
                new LambdaQueryWrapper<UserTagPo>()
                        .eq(UserTagPo::getUserId, userId)
                        .eq(UserTagPo::getRowValid, 1))
                .stream()
                .map(UserTagPoConverter.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserTag> findByTagId(String tagId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<UserTagPo>()
                        .eq(UserTagPo::getTagId, tagId)))
                .map(UserTagPoConverter.INSTANCE::toDomain);
    }

    @Override
    public int insert(UserTag entity) {
        return mapper.insert(UserTagPoConverter.INSTANCE.toPo(entity));
    }

    @Override
    public int updateByTagId(UserTag entity) {
        UserTagPo po = UserTagPoConverter.INSTANCE.toPo(entity);
        return mapper.update(po,
                new LambdaUpdateWrapper<UserTagPo>()
                        .eq(UserTagPo::getTagId, po.getTagId()));
    }
}
