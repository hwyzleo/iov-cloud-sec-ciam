package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserTagRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamUserTagMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserTagPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserTagRepositoryImpl implements UserTagRepository {

    private final CiamUserTagMapper mapper;

    @Override
    public Optional<UserTagPo> findByUserIdAndTagCode(String userId, String tagCode) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<UserTagPo>()
                        .eq(UserTagPo::getUserId, userId)
                        .eq(UserTagPo::getTagCode, tagCode)
                        .eq(UserTagPo::getRowValid, 1)));
    }

    @Override
    public List<UserTagPo> findByUserId(String userId) {
        return mapper.selectList(
                new LambdaQueryWrapper<UserTagPo>()
                        .eq(UserTagPo::getUserId, userId)
                        .eq(UserTagPo::getRowValid, 1));
    }

    @Override
    public Optional<UserTagPo> findByTagId(String tagId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<UserTagPo>()
                        .eq(UserTagPo::getTagId, tagId)));
    }

    @Override
    public int insert(UserTagPo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByTagId(UserTagPo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<UserTagPo>()
                        .eq(UserTagPo::getTagId, entity.getTagId()));
    }
}
