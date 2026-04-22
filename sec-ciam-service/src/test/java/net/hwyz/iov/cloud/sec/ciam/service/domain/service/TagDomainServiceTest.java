package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.TagStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserTagRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserTagPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TagDomainServiceTest {

    private CiamUserTagRepository tagRepository;
    private TagDomainService service;

    @BeforeEach
    void setUp() {
        tagRepository = mock(CiamUserTagRepository.class);
        when(tagRepository.insert(any())).thenReturn(1);
        when(tagRepository.updateByTagId(any())).thenReturn(1);
        service = new TagDomainService(tagRepository);
    }

    private UserTagPo stubTag(String userId, String tagCode, int status) {
        UserTagPo tag = new UserTagPo();
        tag.setTagId("tag-001");
        tag.setUserId(userId);
        tag.setTagCode(tagCode);
        tag.setTagName("测试标签");
        tag.setTagStatus(status);
        tag.setTagSource("system");
        tag.setRowValid(1);
        return tag;
    }

    // ---- addTag ----

    @Nested
    class AddTagTests {

        @Test
        void addTag_createsNewTag() {
            when(tagRepository.findByUserIdAndTagCode("u1", "real_name_verified"))
                    .thenReturn(Optional.empty());

            UserTagPo result = service.addTag("u1", "real_name_verified", "已实名", "system");

            assertNotNull(result.getTagId());
            assertEquals(32, result.getTagId().length());
            assertEquals("u1", result.getUserId());
            assertEquals("real_name_verified", result.getTagCode());
            assertEquals("已实名", result.getTagName());
            assertEquals("system", result.getTagSource());
            assertEquals(TagStatus.VALID.getCode(), result.getTagStatus());
            assertEquals(1, result.getRowValid());
            assertEquals(1, result.getRowVersion());
            assertNotNull(result.getCreateTime());
            assertNotNull(result.getModifyTime());
            assertNotNull(result.getEffectiveTime());
            verify(tagRepository).insert(any(UserTagPo.class));
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

            UserTagPo result = service.addTag("u1", "real_name_verified", "已实名", "system");

            assertNotNull(result);
            verify(tagRepository).insert(any(UserTagPo.class));
        }

        @Test
        void addTag_generatesUniqueTagIds() {
            when(tagRepository.findByUserIdAndTagCode(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            UserTagPo tag1 = service.addTag("u1", "real_name_verified", "已实名", "system");
            UserTagPo tag2 = service.addTag("u2", "owner_verified", "已车主认证", "callback");

            assertNotEquals(tag1.getTagId(), tag2.getTagId());
        }
    }

    // ---- removeTag ----

    @Nested
    class RemoveTagTests {

        @Test
        void removeTag_setsStatusToInvalid() {
            UserTagPo existing = stubTag("u1", "real_name_verified", TagStatus.VALID.getCode());
            when(tagRepository.findByUserIdAndTagCode("u1", "real_name_verified"))
                    .thenReturn(Optional.of(existing));

            service.removeTag("u1", "real_name_verified");

            ArgumentCaptor<UserTagPo> captor = ArgumentCaptor.forClass(UserTagPo.class);
            verify(tagRepository).updateByTagId(captor.capture());
            assertEquals(TagStatus.INVALID.getCode(), captor.getValue().getTagStatus());
            assertNotNull(captor.getValue().getExpireTime());
            assertNotNull(captor.getValue().getModifyTime());
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
            UserTagPo active = stubTag("u1", "real_name_verified", TagStatus.VALID.getCode());
            UserTagPo inactive = stubTag("u1", "owner_verified", TagStatus.INVALID.getCode());
            when(tagRepository.findByUserId("u1")).thenReturn(List.of(active, inactive));

            List<UserTagPo> result = service.getActiveTags("u1");

            assertEquals(1, result.size());
            assertEquals("real_name_verified", result.get(0).getTagCode());
        }

        @Test
        void getActiveTags_returnsEmptyListWhenNoTags() {
            when(tagRepository.findByUserId("u1")).thenReturn(Collections.emptyList());

            List<UserTagPo> result = service.getActiveTags("u1");

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
            UserTagPo existing = stubTag("u1", "owner_verified", TagStatus.VALID.getCode());
            when(tagRepository.findByUserIdAndTagCode("u1", "owner_verified"))
                    .thenReturn(Optional.of(existing));

            service.updateTagStatus("u1", "owner_verified", TagStatus.INVALID.getCode());

            ArgumentCaptor<UserTagPo> captor = ArgumentCaptor.forClass(UserTagPo.class);
            verify(tagRepository).updateByTagId(captor.capture());
            assertEquals(TagStatus.INVALID.getCode(), captor.getValue().getTagStatus());
            assertNotNull(captor.getValue().getExpireTime());
        }

        @Test
        void updateTagStatus_setsExpireTimeOnlyWhenInvalid() {
            UserTagPo existing = stubTag("u1", "owner_verified", TagStatus.INVALID.getCode());
            existing.setExpireTime(null);
            when(tagRepository.findByUserIdAndTagCode("u1", "owner_verified"))
                    .thenReturn(Optional.of(existing));

            service.updateTagStatus("u1", "owner_verified", TagStatus.VALID.getCode());

            ArgumentCaptor<UserTagPo> captor = ArgumentCaptor.forClass(UserTagPo.class);
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
