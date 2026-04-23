package net.hwyz.iov.cloud.sec.ciam.service.application;

import net.hwyz.iov.cloud.sec.ciam.service.application.dto.MergeRequestDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.UserSearchDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.AccountQueryAppService;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.UserStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.*;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.query.UserQuery;
import net.hwyz.iov.cloud.sec.ciam.service.domain.search.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountQueryAppServiceTest {

    private UserRepository userRepository;
    private UserIdentityRepository identityRepository;
    private UserProfileRepository profileRepository;
    private UserTagRepository tagRepository;
    private MergeRequestRepository mergeRequestRepository;
    private DeactivationRequestRepository deactivationRequestRepository;
    private SearchService searchService;
    private FieldEncryptor fieldEncryptor;

    private AccountQueryAppService service;

    private static final String USER_ID = "user-001";

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        identityRepository = mock(UserIdentityRepository.class);
        profileRepository = mock(UserProfileRepository.class);
        tagRepository = mock(UserTagRepository.class);
        mergeRequestRepository = mock(MergeRequestRepository.class);
        deactivationRequestRepository = mock(DeactivationRequestRepository.class);
        searchService = mock(SearchService.class);
        fieldEncryptor = mock(FieldEncryptor.class);

        service = new AccountQueryAppService(
                userRepository, identityRepository, profileRepository,
                tagRepository, mergeRequestRepository, deactivationRequestRepository,
                searchService, fieldEncryptor);
    }

    private User stubUser() {
        return User.builder().userId(USER_ID).userStatus(UserStatus.ACTIVE.getCode()).build();
    }

    private UserIdentity stubIdentity() {
        return UserIdentity.builder().userId(USER_ID).identityType("MOBILE").identityValue("13800000000").build();
    }

    @Test
    void queryUser_successfully() {
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubUser()));
        when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of(stubIdentity()));
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        AccountQueryAppService.UserDetail detail = service.queryUser(USER_ID);
        assertEquals(USER_ID, detail.userId());
    }

    @Test
    void queryUserList_successfully() {
        when(userRepository.search(any(UserSearchCriteria.class))).thenReturn(List.of(stubUser()));
        when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of(stubIdentity()));
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        List<UserSearchDto> result = service.queryUserList(UserQuery.builder().build());
        assertFalse(result.isEmpty());
        assertEquals(USER_ID, result.get(0).getUserId());
    }

    @Test
    void queryMergeRequests_successfully() {
        MergeRequest request = MergeRequest.builder().mergeRequestId("m1").build();
        when(mergeRequestRepository.findByReviewStatus(anyInt())).thenReturn(List.of(request));
        List<MergeRequestDto> result = service.queryMergeRequests(0);
        assertEquals(1, result.size());
    }
}
