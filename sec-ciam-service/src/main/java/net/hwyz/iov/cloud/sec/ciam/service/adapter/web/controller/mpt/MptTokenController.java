package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mpt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.RefreshTokenVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.TokenQueryAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.assembler.RefreshTokenMapper;
import net.hwyz.iov.cloud.sec.ciam.service.domain.query.TokenQuery;
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
@Slf4j
@RestController
@RequestMapping("/api/mpt/token/v1")
@RequiredArgsConstructor
public class MptTokenController extends BaseController {

    private final TokenQueryAppService tokenQueryAppService;
    private final RefreshTokenMapper refreshTokenMapper = RefreshTokenMapper.INSTANCE;

    /**
     * 检索令牌列表
     */
    @GetMapping("/tokens")
    public ApiResponse<PageResult<TokenQueryAppService.TokenSearchResult>> searchTokens(
            @RequestParam(required = false) String refreshTokenId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) Integer tokenStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        
        TokenQuery query = TokenQuery.builder().refreshTokenId(refreshTokenId).userId(userId).sessionId(sessionId).clientId(clientId).tokenStatus(tokenStatus).startTime(startTime).endTime(endTime).build();
        startPage();
        List<TokenQueryAppService.TokenSearchResult> list = tokenQueryAppService.queryTokenList(query);
        return ApiResponse.ok(getPageResult(list));
    }

    @GetMapping("/tokens/detail")
    public ApiResponse<RefreshTokenVo> getTokenDetail(@RequestParam String refreshTokenId) {
        return ApiResponse.ok(refreshTokenMapper.toVo(tokenQueryAppService.queryToken(refreshTokenId)));
    }

    @GetMapping("/tokens/user")
    public ApiResponse<List<RefreshTokenVo>> getUserTokens(@RequestParam String userId) {
        List<RefreshTokenVo> voList = tokenQueryAppService.queryUserTokens(userId).stream()
            .map(refreshTokenMapper::toVo)
            .collect(Collectors.toList());
        return ApiResponse.ok(voList);
    }

    @GetMapping("/tokens/session")
    public ApiResponse<List<RefreshTokenVo>> getSessionTokens(@RequestParam String sessionId) {
        List<RefreshTokenVo> voList = tokenQueryAppService.querySessionTokens(sessionId).stream()
            .map(refreshTokenMapper::toVo)
            .collect(Collectors.toList());
        return ApiResponse.ok(voList);
    }
}
