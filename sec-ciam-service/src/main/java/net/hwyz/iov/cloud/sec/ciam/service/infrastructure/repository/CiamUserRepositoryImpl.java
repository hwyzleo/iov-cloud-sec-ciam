package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.query.UserQuery;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.CiamUserMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.UserPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamUserRepositoryImpl implements CiamUserRepository {

    private final CiamUserMapper mapper;

    @Override
    public Optional<UserPo> findByUserId(String userId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<UserPo>()
                        .eq(UserPo::getUserId, userId)
                        .eq(UserPo::getRowValid, 1)));
    }

    @Override
    public List<UserPo> search(UserQuery query) {
        return mapper.searchUsers(
                query.getUserId(),
                query.getIdentityType(),
                query.getIdentityValue(),
                query.getNickname(),
                query.getRegisterSource(),
                query.getUserStatus(),
                query.getStartTime(),
                query.getEndTime());
    }

    @Override
    public List<UserPo> findByUserStatus(int userStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<UserPo>()
                        .eq(UserPo::getUserStatus, userStatus)
                        .eq(UserPo::getRowValid, 1));
    }

    @Override
    public int insert(UserPo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByUserId(UserPo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<UserPo>()
                        .eq(UserPo::getUserId, entity.getUserId()));
    }

    @Override
    public int physicalDeleteByUserId(String userId) {
        return mapper.delete(
                new LambdaQueryWrapper<UserPo>()
                        .eq(UserPo::getUserId, userId));
    }
}
