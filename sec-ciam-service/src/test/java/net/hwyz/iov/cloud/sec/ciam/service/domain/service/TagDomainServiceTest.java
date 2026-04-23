package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.TagStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserTag;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TagDomainServiceTest {

    private UserTagRepository tagRepository;
    private TagDomainService service;

    @BeforeEach
    void setUp() {
        tagRepository = mock(UserTagRepository.class);
        when(tagRepository.insert(any())).thenReturn(1);
        when(tagRepository.updateByTagId(any())).thenReturn(1);
        service = new TagDomainService(tagRepository);
    }

    private UserTag stubTag(String userId, String tagCode, int status) {
        return UserTag.builder()
                .tagId("tag-001")
                .userId(userId)
                .tagCode(tagCode)
                .tagName("测试标签")
                .tagStatus(status)
                .tagSource("system")
                .build();
    }

    // ---- addTag ----

    @Nested
    class AddTagTests {

        @Test
        void addTag_createsNewTag() {
            when(tagRepository.findByUserIdAndTagCode("u1", "real_name_verified"))
                    .thenReturn(Optional.empty());

            UserTag result = service.addTag("u1", "real_name_verified", "已实名", "system");

            assertNotNull(result.getTagId());
            assertEquals(32, result.getTagId().length());
            assertEquals("u1", result.getUserId());
            assertEquals("real_name_verified", result.getTagCode());
            assertEquals("已实名", result.getTagName());
            assertEquals("system", result.getTagSource());
            assertEquals(TagStatus.VALID.getCode(), result.getTagStatus());
            assertNotNull(result.getEffectiveTime());
            verify(tagRepository).insert(any(UserTag.class));
        }

        @Test
        void addTag_throwsWhenActiveTagExists() {
            when(tagRepository.findByUserIdAndTagCode("u1", "owner_verified"))
                    .thenReturn(Optional.of(stubTag("u1", "owner_verified", TagStatus.VALID.getCode())));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.addTag("u1", "owner_verified", "已车主认证", "callback"));
            assertEquals(CiamErrorCode.TAG_ALREADY_EXISTS, ex.getErrorCode());
            verify(tagRepository, never()).insert(any());
        }

        @Test
        void addTag_allowsWhenExistingTagIsInactive() {
            when(tagRepository.findByUserIdAndTagCode("u1", "real_name_verified"))
                    .thenReturn(Optional.of(stubTag("u1", "real_name_verified", TagStatus.INVALID.getCode())));

            UserTag result = service.addTag("u1", "real_name_verified", "已实名", "system");

            assertNotNull(result);
            verify(tagRepository).insert(any(UserTag.class));
        }

        @Test
        void addTag_generatesUniqueTagIds() {
            when(tagRepository.findByUserIdAndTagCode(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            UserTag tag1 = service.addTag("u1", "real_name_verified", "已实名", "system");
            UserTag tag2 = service.addTag("u2", "owner_verified", "已车主认证", "callback");

            assertNotEquals(tag1.getTagId(), tag2.getTagId());
        }
    }

    // ---- removeTag ----

    @Nested
    class RemoveTagTests {

        @Test
        void removeTag_setsStatusToInvalid() {
            UserTag existing = stubTag("u1", "real_name_verified", TagStatus.VALID.getCode());
            when(tagRepository.findByUserIdAndTagCode("u1", "real_name_verified"))
                    .thenReturn(Optional.of(existing));

            service.removeTag("u1", "real_name_verified");

            ArgumentCaptor<UserTag> captor = ArgumentCaptor.forClass(UserTag.class);
            verify(tagRepository).updateByTagId(captor.capture());
            assertEquals(TagStatus.INVALID.getCode(), captor.getValue().getTagStatus());
            assertNotNull(captor.getValue().getExpireTime());
        }

        @Test
        void removeTag_throwsWhenTagNotFound() {
            when(tagRepository.findByUserIdAndTagCode("u1", "nonexistent"))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.removeTag("u1", "nonexistent"));
            assertEquals(CiamErrorCode.TAG_NOT_FOUND, ex.getErrorCode());
            verify(tagRepository, never()).updateByTagId(any());
        }
    }

    // ---- getActiveTags ----

    @Nested
    class GetActiveTagsTests {

        @Test
        void getActiveTags_returnsOnlyActiveTags() {
            UserTag active = stubTag("u1", "real_name_verified", TagStatus.VALID.getCode());
            UserTag inactive = stubTag("u1", "owner_verified", TagStatus.INVALID.getCode());
            when(tagRepository.findByUserId("u1")).thenReturn(List.of(active, inactive));

            List<UserTag> result = service.getActiveTags("u1");

            assertEquals(1, result.size());
            assertEquals("real_name_verified", result.get(0).getTagCode());
        }

        @Test
        void getActiveTags_returnsEmptyListWhenNoTags() {
            when(tagRepository.findByUserId("u1")).thenReturn(Collections.emptyList());

            List<UserTag> result = service.getActiveTags("u1");

            assertTrue(result.isEmpty());
        }
    }

    // ---- hasTag ----

    @Nested
    class HasTagTests {

        @Test
        void hasTag_returnsTrueForActiveTag() {
            when(tagRepository.findByUserIdAndTagCode("u1", "real_name_verified"))
                    .thenReturn(Optional.of(stubTag("u1", "real_name_verified", TagStatus.VALID.getCode())));

            assertTrue(service.hasTag("u1", "real_name_verified"));
        }

        @Test
        void hasTag_returnsFalseForInactiveTag() {
            when(tagRepository.findByUserIdAndTagCode("u1", "real_name_verified"))
                    .thenReturn(Optional.of(stubTag("u1", "real_name_verified", TagStatus.INVALID.getCode())));

            assertFalse(service.hasTag("u1", "real_name_verified"));
        }

        @Test
        void hasTag_returnsFalseWhenTagNotFound() {
            when(tagRepository.findByUserIdAndTagCode("u1", "nonexistent"))
                    .thenReturn(Optional.empty());

            assertFalse(service.hasTag("u1", "nonexistent"));
        }
    }

    // ---- updateTagStatus ----

    @Nested
    class UpdateTagStatusTests {

        @Test
        void updateTagStatus_updatesStatus() {
            UserTag existing = stubTag("u1", "owner_verified", TagStatus.VALID.getCode());
            when(tagRepository.findByUserIdAndTagCode("u1", "owner_verified"))
                    .thenReturn(Optional.of(existing));

            service.updateTagStatus("u1", "owner_verified", TagStatus.INVALID.getCode());

            ArgumentCaptor<UserTag> captor = ArgumentCaptor.forClass(UserTag.class);
            verify(tagRepository).updateByTagId(captor.capture());
            assertEquals(TagStatus.INVALID.getCode(), captor.getValue().getTagStatus());
            assertNotNull(captor.getValue().getExpireTime());
        }

        @Test
        void updateTagStatus_setsExpireTimeOnlyWhenInvalid() {
            UserTag existing = stubTag("u1", "owner_verified", TagStatus.INVALID.getCode());
            existing.setExpireTime(null);
            when(tagRepository.findByUserIdAndTagCode("u1", "owner_verified"))
                    .thenReturn(Optional.of(existing));

            service.updateTagStatus("u1", "owner_verified", TagStatus.VALID.getCode());

            ArgumentCaptor<UserTag> captor = ArgumentCaptor.forClass(UserTag.class);
            verify(tagRepository).updateByTagId(captor.capture());
            assertEquals(TagStatus.VALID.getCode(), captor.getValue().getTagStatus());
            assertNull(captor.getValue().getExpireTime());
        }

        @Test
        void updateTagStatus_throwsWhenTagNotFound() {
            when(tagRepository.findByUserIdAndTagCode("u1", "nonexistent"))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.updateTagStatus("u1", "nonexistent", TagStatus.INVALID.getCode()));
            assertEquals(CiamErrorCode.TAG_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void updateTagStatus_throwsForInvalidStatusCode() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.updateTagStatus("u1", "owner_verified", 99));
        }
    }
}
