package net.hwyz.iov.cloud.sec.ciam.service.domain.service;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.*;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class UserStatusMachineTest {

    // ---- 合法流转 ----

    static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(UserStatus.PENDING, UserStatus.ACTIVE),
                Arguments.of(UserStatus.ACTIVE, UserStatus.LOCKED),
                Arguments.of(UserStatus.ACTIVE, UserStatus.DISABLED),
                Arguments.of(UserStatus.ACTIVE, UserStatus.DEACTIVATING),
                Arguments.of(UserStatus.LOCKED, UserStatus.ACTIVE),
                Arguments.of(UserStatus.DISABLED, UserStatus.ACTIVE),
                Arguments.of(UserStatus.DEACTIVATING, UserStatus.DEACTIVATED),
                Arguments.of(UserStatus.DEACTIVATING, UserStatus.ACTIVE)
        );
    }

    @ParameterizedTest
    @MethodSource("validTransitions")
    void canTransit_returnsTrue_forValidTransitions(UserStatus from, UserStatus to) {
        assertTrue(UserStatusMachine.canTransit(from, to));
    }

    @ParameterizedTest
    @MethodSource("validTransitions")
    void validateTransition_doesNotThrow_forValidTransitions(UserStatus from, UserStatus to) {
        assertDoesNotThrow(() -> UserStatusMachine.validateTransition(from, to));
    }

    // ---- 非法流转 ----

    static Stream<Arguments> illegalTransitions() {
        return Stream.of(
                Arguments.of(UserStatus.PENDING, UserStatus.LOCKED),
                Arguments.of(UserStatus.PENDING, UserStatus.DISABLED),
                Arguments.of(UserStatus.PENDING, UserStatus.DEACTIVATING),
                Arguments.of(UserStatus.PENDING, UserStatus.DEACTIVATED),
                Arguments.of(UserStatus.ACTIVE, UserStatus.PENDING),
                Arguments.of(UserStatus.ACTIVE, UserStatus.DEACTIVATED),
                Arguments.of(UserStatus.LOCKED, UserStatus.DISABLED),
                Arguments.of(UserStatus.LOCKED, UserStatus.DEACTIVATING),
                Arguments.of(UserStatus.LOCKED, UserStatus.DEACTIVATED),
                Arguments.of(UserStatus.DISABLED, UserStatus.LOCKED),
                Arguments.of(UserStatus.DISABLED, UserStatus.DEACTIVATING),
                Arguments.of(UserStatus.DISABLED, UserStatus.DEACTIVATED),
                Arguments.of(UserStatus.DEACTIVATING, UserStatus.LOCKED),
                Arguments.of(UserStatus.DEACTIVATING, UserStatus.DISABLED),
                Arguments.of(UserStatus.DEACTIVATED, UserStatus.ACTIVE),
                Arguments.of(UserStatus.DEACTIVATED, UserStatus.PENDING)
        );
    }

    @ParameterizedTest
    @MethodSource("illegalTransitions")
    void canTransit_returnsFalse_forIllegalTransitions(UserStatus from, UserStatus to) {
        assertFalse(UserStatusMachine.canTransit(from, to));
    }

    @ParameterizedTest
    @MethodSource("illegalTransitions")
    void validateTransition_throwsBusinessException_forIllegalTransitions(UserStatus from, UserStatus to) {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> UserStatusMachine.validateTransition(from, to));
        assertEquals(CiamErrorCode.ILLEGAL_STATUS_TRANSITION, ex.getErrorCode());
    }

    // ---- 自身流转 ----

    @Test
    void canTransit_returnsFalse_forSameStatus() {
        for (UserStatus status : UserStatus.values()) {
            assertFalse(UserStatusMachine.canTransit(status, status),
                    "自身流转应被拒绝: " + status);
        }
    }
}
