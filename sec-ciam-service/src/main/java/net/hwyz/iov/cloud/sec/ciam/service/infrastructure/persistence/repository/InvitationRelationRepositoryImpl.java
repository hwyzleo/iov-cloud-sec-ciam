package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.InvitationRelation;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.InvitationRelationRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.InvitationRelationPoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamInvitationRelationMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.InvitationRelationPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class InvitationRelationRepositoryImpl implements InvitationRelationRepository {

    private final CiamInvitationRelationMapper mapper;

    @Override
    public Optional<InvitationRelation> findByRelationId(String relationId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<InvitationRelationPo>()
                        .eq(InvitationRelationPo::getRelationId, relationId)))
                .map(InvitationRelationPoConverter.INSTANCE::toDomain);
    }

    @Override
    public Optional<InvitationRelation> findByInviteeUserId(String inviteeUserId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<InvitationRelationPo>()
                        .eq(InvitationRelationPo::getInviteeUserId, inviteeUserId)
                        .eq(InvitationRelationPo::getRowValid, 1)))
                .map(InvitationRelationPoConverter.INSTANCE::toDomain);
    }

    @Override
    public List<InvitationRelation> findByInviterUserId(String inviterUserId) {
        return mapper.selectList(
                new LambdaQueryWrapper<InvitationRelationPo>()
                        .eq(InvitationRelationPo::getInviterUserId, inviterUserId)
                        .eq(InvitationRelationPo::getRowValid, 1))
                .stream()
                .map(InvitationRelationPoConverter.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InvitationRelation> findByInviteChannelCode(String inviteChannelCode) {
        return mapper.selectList(
                new LambdaQueryWrapper<InvitationRelationPo>()
                        .eq(InvitationRelationPo::getInviteChannelCode, inviteChannelCode)
                        .eq(InvitationRelationPo::getRowValid, 1))
                .stream()
                .map(InvitationRelationPoConverter.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int insert(InvitationRelation entity) {
        return mapper.insert(InvitationRelationPoConverter.INSTANCE.toDo(entity));
    }
}
