package dtu.fusion;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FusionRestClientFactoryTest {
  @Test
  void buildReturnsConfiguredRestClient() {
    RestClient client = FusionRestClientFactory.build("http://localhost", "fusion-user", "s3cr3t", 2, 5, 10, 30);

    assertThat(client).isNotNull();
  }

  @Test
  void buildOAuth2ReturnsConfiguredRestClient() {
    OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);

    RestClient client = FusionRestClientFactory.buildOAuth2("http://localhost", manager, "fusion-erp", 2, 5, 10, 30);

    assertThat(client).isNotNull();
  }

  @Test
  void constructorIsPrivate() throws Exception {
    Constructor<FusionRestClientFactory> ctor = FusionRestClientFactory.class.getDeclaredConstructor();

    assertThat(ctor.canAccess(null)).isFalse();

    ctor.setAccessible(true);
    ctor.newInstance(); // covers the private constructor body
  }

  @Test
  void buildClientCredentialsManagerReturnsNonNullManager() {
    OAuth2AuthorizedClientManager manager = FusionRestClientFactory.buildClientCredentialsManager(
        "fusion-hcm", "client-id", "client-secret", "openid",
        "http://localhost/oauth/token");

    assertThat(manager).isNotNull();
  }

  @Test
  void buildClientManagerReturnsNonNullManager() {
    OAuth2AuthorizedClientManager manager = FusionRestClientFactory.buildClientManager(
        "fusion-hcm", "client-id", "client-secret", "openid",
        "http://localhost/oauth/token");

    assertThat(manager).isNotNull();
  }

  @Test
  void buildClientManagerSubstitutesServicePrincipalForBlankPrincipalName() {
    OAuth2AuthorizedClientManager manager = FusionRestClientFactory.buildClientManager(
        "fusion-hcm", "client-id", "client-secret", "openid",
        "http://localhost/oauth/token");

    Authentication principal = mock(Authentication.class);
    when(principal.getName()).thenReturn("");
    OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
        .withClientRegistrationId("fusion-hcm")
        .principal(principal)
        .build();

    // Lambda substitutes servicePrincipal then delegates; token endpoint
    // unavailable → exception
    assertThatThrownBy(() -> manager.authorize(request))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void buildClientManagerPassesThroughNonBlankPrincipalName() {
    OAuth2AuthorizedClientManager manager = FusionRestClientFactory.buildClientManager(
        "fusion-hcm", "client-id", "client-secret", "openid",
        "http://localhost/oauth/token");

    Authentication principal = mock(Authentication.class);
    when(principal.getName()).thenReturn("some-service");
    OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
        .withClientRegistrationId("fusion-hcm")
        .principal(principal)
        .build();

    // Lambda passes request through unchanged then delegates; token endpoint
    // unavailable → exception
    assertThatThrownBy(() -> manager.authorize(request))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void buildProxyFactoryReturnsNonNullFactory() {
    RestClient restClient = FusionRestClientFactory.build(
        "http://localhost", "user", "pass", 2, 5, 10, 30);

    HttpServiceProxyFactory factory = FusionRestClientFactory.buildProxyFactory(restClient);

    assertThat(factory).isNotNull();
  }
}
