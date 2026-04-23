package net.hwyz.iov.cloud.sec.ciam.service.domain.gateway;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 适配器通用操作结果。
 */
@Getter
@AllArgsConstructor
public class AdapterResult {

    private final boolean success;
    private final String message;

    public static AdapterResult ok() {
        return new AdapterResult(true, "ok");
    }

    public static AdapterResult ok(String message) {
        return new AdapterResult(true, message);
    }

    public static AdapterResult fail(String message) {
        return new AdapterResult(false, message);
    }
}
