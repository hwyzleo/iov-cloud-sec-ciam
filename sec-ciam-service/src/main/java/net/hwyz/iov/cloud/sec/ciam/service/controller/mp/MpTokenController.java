package net.hwyz.iov.cloud.sec.ciam.service.controller.mp;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.sec.ciam.api.vo.RefreshTokenVO;
import net.hwyz.iov.cloud.sec.ciam.service.application.TokenQueryAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.RefreshTokenMapper;
import net.hwyz.iov.cloud.sec.ciam.service.domain.search.SearchResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 运营后台管理控制器 — 令牌查询。
 */
@RestController
@RequestMapping("/api/mp/token/v1")
@RequiredArgsConstructor
public class MpTokenController {

    private final TokenQueryAppService tokenQueryAppService;
    
    private final RefreshTokenMapper refreshTokenMapper = RefreshTokenMapper.INSTANCE;

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
    public ApiResponse<RefreshTokenVO> getTokenDetail(@RequestParam String refreshTokenId) {
        var token = tokenQueryAppService.queryToken(refreshTokenId);
        var domainModel = refreshTokenMapper.toDomain(token);
        RefreshTokenVO vo = refreshTokenMapper.toVo(domainModel);
        return ApiResponse.ok(vo);
    }

    /**
     * 查询用户的令牌列表
     */
    @GetMapping("/tokens/user")
    public ApiResponse<List<RefreshTokenVO>> getUserTokens(@RequestParam String userId) {
        var tokens = tokenQueryAppService.queryUserTokens(userId);
        List<RefreshTokenVO> voList = tokens.stream()
            .map(t -> {
                var domainModel = refreshTokenMapper.toDomain(t);
                return refreshTokenMapper.toVo(domainModel);
            })
            .collect(Collectors.toList());
        return ApiResponse.ok(voList);
    }

    /**
     * 查询会话的令牌列表
     */
    @GetMapping("/tokens/session")
    public ApiResponse<List<RefreshTokenVO>> getSessionTokens(@RequestParam String sessionId) {
        var tokens = tokenQueryAppService.querySessionTokens(sessionId);
        List<RefreshTokenVO> voList = tokens.stream()
            .map(t -> {
                var domainModel = refreshTokenMapper.toDomain(t);
                return refreshTokenMapper.toVo(domainModel);
            })
            .collect(Collectors.toList());
        return ApiResponse.ok(voList);
    }
}
