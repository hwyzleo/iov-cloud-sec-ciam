package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.CiamUserMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserDo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamUserRepositoryImpl implements CiamUserRepository {

    private final CiamUserMapper mapper;

    @Override
    public Optional<CiamUserDo> findByUserId(String userId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamUserDo>()
                        .eq(CiamUserDo::getUserId, userId)
                        .eq(CiamUserDo::getRowValid, 1)));
    }

    @Override
    public List<CiamUserDo> findAll() {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamUserDo>()
                        .eq(CiamUserDo::getRowValid, 1));
    }

    @Override
    public List<CiamUserDo> findByUserStatus(int userStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamUserDo>()
                        .eq(CiamUserDo::getUserStatus, userStatus)
                        .eq(CiamUserDo::getRowValid, 1));
    }

    @Override
    public int insert(CiamUserDo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByUserId(CiamUserDo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<CiamUserDo>()
                        .eq(CiamUserDo::getUserId, entity.getUserId()));
    }

    @Override
    public int physicalDeleteByUserId(String userId) {
        return mapper.delete(
                new LambdaQueryWrapper<CiamUserDo>()
                        .eq(CiamUserDo::getUserId, userId));
    }
}
