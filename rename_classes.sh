#!/bin/bash
REPLACEMENTS=(
    "CiamRefreshTokenDo:RefreshTokenPo"
    "CiamAuditLogDo:AuditLogPo"
    "CiamMergeRequestDo:MergeRequestPo"
    "CiamUserCredentialDo:UserCredentialPo"
    "CiamUserProfileDo:UserProfilePo"
    "CiamMfaChallengeDo:MfaChallengePo"
    "CiamRiskEventDo:RiskEventPo"
    "CiamOwnerCertStateDo:OwnerCertStatePo"
    "CiamUserIdentityDo:UserIdentityPo"
    "CiamOAuthClientDo:OAuthClientPo"
    "CiamDeviceDo:DevicePo"
    "CiamJwkDo:JwkPo"
    "CiamSessionDo:SessionPo"
    "CiamAuthCodeDo:AuthCodePo"
    "CiamUserTagDo:UserTagPo"
    "CiamInvitationRelationDo:InvitationRelationPo"
    "CiamDeactivationRequestDo:DeactivationRequestPo"
    "CiamUserDo:UserPo"
    "CiamUserConsentDo:UserConsentPo"
)

for replacement in "${REPLACEMENTS[@]}"; do
    OLD="${replacement%%:*}"
    NEW="${replacement##*:}"
    echo "Replacing $OLD with $NEW..."
    find . -type f -name "*.java" -exec sed -i '' "s/$OLD/$NEW/g" {} +
done
