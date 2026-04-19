package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserTagRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.CiamUserTagMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserTagDo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamUserTagRepositoryImpl implements CiamUserTagRepository {

    private final CiamUserTagMapper mapper;

    @Override
    public Optional<CiamUserTagDo> findByUserIdAndTagCode(String userId, String tagCode) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamUserTagDo>()
                        .eq(CiamUserTagDo::getUserId, userId)
                        .eq(CiamUserTagDo::getTagCode, tagCode)
                        .eq(CiamUserTagDo::getRowValid, 1)));
    }

    @Override
    public List<CiamUserTagDo> findByUserId(String userId) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamUserTagDo>()
                        .eq(CiamUserTagDo::getUserId, userId)
                        .eq(CiamUserTagDo::getRowValid, 1));
    }

    @Override
    public Optional<CiamUserTagDo> findByTagId(String tagId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamUserTagDo>()
                        .eq(CiamUserTagDo::getTagId, tagId)));
    }

    @Override
    public int insert(CiamUserTagDo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByTagId(CiamUserTagDo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<CiamUserTagDo>()
                        .eq(CiamUserTagDo::getTagId, entity.getTagId()));
    }
}
