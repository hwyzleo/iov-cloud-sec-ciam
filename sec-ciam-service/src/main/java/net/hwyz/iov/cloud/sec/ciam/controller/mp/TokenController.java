package net.hwyz.iov.cloud.sec.ciam.controller.mp;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.sec.ciam.application.TokenQueryAppService;
import net.hwyz.iov.cloud.sec.ciam.domain.search.SearchResult;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamRefreshTokenDo;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 运营后台管理控制器 — 令牌查询。
 */
@RestController
@RequestMapping("/api/mp/token/v1")
@RequiredArgsConstructor
public class TokenController {

    private final TokenQueryAppService tokenQueryAppService;

    /**
     * 检索令牌列表
     */
    @GetMapping("/tokens")
    public ApiResponse<SearchResult<TokenQueryAppService.TokenSearchResult>> searchTokens(
            @RequestParam(required = false) String refreshTokenId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) Integer tokenStatus,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") OffsetDateTime endTime,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ApiResponse.ok(tokenQueryAppService.queryTokenList(
                refreshTokenId, userId, sessionId, clientId, tokenStatus, startTime, endTime, page, size));
    }

    /**
     * 查询令牌详情
     */
    @GetMapping("/tokens/detail")
    public ApiResponse<CiamRefreshTokenDo> getTokenDetail(@RequestParam String refreshTokenId) {
        return ApiResponse.ok(tokenQueryAppService.queryToken(refreshTokenId));
    }

    /**
     * 查询用户的令牌列表
     */
    @GetMapping("/tokens/user")
    public ApiResponse<List<CiamRefreshTokenDo>> getUserTokens(@RequestParam String userId) {
        return ApiResponse.ok(tokenQueryAppService.queryUserTokens(userId));
    }

    /**
     * 查询会话的令牌列表
     */
    @GetMapping("/tokens/session")
    public ApiResponse<List<CiamRefreshTokenDo>> getSessionTokens(@RequestParam String sessionId) {
        return ApiResponse.ok(tokenQueryAppService.querySessionTokens(sessionId));
    }
}
