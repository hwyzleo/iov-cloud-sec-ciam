package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.User;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserSearchCriteria;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.UserPoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamUserMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final CiamUserMapper mapper;
    private final UserPoConverter userPoConverter;

    @Override
    public Optional<User> findByUserId(String userId) {
        UserPo po = mapper.selectOne(
                new LambdaQueryWrapper<UserPo>()
                        .eq(UserPo::getUserId, userId)
                        .eq(UserPo::getRowValid, 1));
        return Optional.ofNullable(userPoConverter.toDomain(po));
    }

    @Override
    public List<User> search(UserSearchCriteria criteria) {
        List<UserPo> pos = mapper.searchUsers(
                criteria.getUserId(),
                criteria.getIdentityType(),
                criteria.getIdentityValue(),
                criteria.getNickname(),
                criteria.getRegisterSource(),
                criteria.getUserStatus(),
                criteria.getStartTime(),
                criteria.getEndTime());
        return pos.stream().map(userPoConverter::toDomain).toList();
    }

    @Override
    public List<User> findByUserStatus(int userStatus) {
        List<UserPo> pos = mapper.selectList(
                new LambdaQueryWrapper<UserPo>()
                        .eq(UserPo::getUserStatus, userStatus)
                        .eq(UserPo::getRowValid, 1));
        return pos.stream().map(userPoConverter::toDomain).toList();
    }

    @Override
    public int insert(User entity) {
        return mapper.insert(userPoConverter.toPo(entity));
    }

    @Override
    public int updateByUserId(User entity) {
        UserPo po = userPoConverter.toPo(entity);
        return mapper.update(po,
                new LambdaUpdateWrapper<UserPo>()
                        .eq(UserPo::getUserId, po.getUserId()));
    }

    @Override
    public int physicalDeleteByUserId(String userId) {
        return mapper.delete(
                new LambdaQueryWrapper<UserPo>()
                        .eq(UserPo::getUserId, userId));
    }
}
