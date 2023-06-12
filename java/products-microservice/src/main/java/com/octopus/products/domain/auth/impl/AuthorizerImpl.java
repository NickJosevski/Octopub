package com.octopus.products.domain.auth.impl;

import com.octopus.features.AdminJwtClaimFeature;
import com.octopus.features.AdminJwtGroupFeature;
import com.octopus.jwt.JwtInspector;
import com.octopus.jwt.JwtUtils;
import com.octopus.products.domain.auth.Authorizer;
import com.octopus.products.domain.features.CognitoClientIdFeature;
import com.octopus.products.domain.features.impl.DisableSecurityFeatureImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * An implementation of the Authorizer service.
 */
@ApplicationScoped
public class AuthorizerImpl implements Authorizer {

  @Inject
  DisableSecurityFeatureImpl cognitoDisableAuth;

  @Inject
  AdminJwtClaimFeature adminJwtClaimFeature;

  @Inject
  JwtInspector jwtInspector;

  @Inject
  AdminJwtGroupFeature adminJwtGroupFeature;

  @Inject
  JwtUtils jwtUtils;

  @Inject
  CognitoClientIdFeature cognitoClientIdFeature;


  @Override
  public boolean isAuthorized(final String authorizationHeader, final String serviceAuthorizationHeader) {
    /*
    * This method implements the following logic:
    * If auth is disabled, return true.
    * If the Service-Authorization header contains an access token with the correct scope,
      generated by a known app client, return true.
    * If the Authorization header contains a known group, return true.
    * Otherwise, return false.
    */

    if (cognitoDisableAuth.getCognitoAuthDisabled()) {
      return true;
    }

    /*
      An admin scope granted to an access token generated by a known client credentials
      app client is accepted as machine-to-machine communication.
     */
    if (adminJwtClaimFeature.getAdminClaim().isPresent() && jwtUtils.getJwtFromAuthorizationHeader(serviceAuthorizationHeader)
        .map(jwt -> jwtInspector.jwtContainsScope(jwt, adminJwtClaimFeature.getAdminClaim().get(), cognitoClientIdFeature.getCognitoClientId()))
        .orElse(false)) {
      return true;
    }

    /*
      Anyone assigned to the appropriate group is also granted access.
     */
    return adminJwtGroupFeature.getAdminGroup().isPresent() && jwtUtils.getJwtFromAuthorizationHeader(authorizationHeader)
        .map(jwt -> jwtInspector.jwtContainsCognitoGroup(jwt, adminJwtGroupFeature.getAdminGroup().get()))
        .orElse(false);
  }
}
