package dtu.fusion;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Factory for building a Fusion-ready {@link RestClient}.
 *
 * <p>
 * All Fusion REST endpoints require the same set of default headers (Basic
 * auth, JSON accept, REST Framework version, gzip encoding) and benefit from a
 * connection-pooled Apache HTTP client. This factory centralises that setup so
 * each service's client config only has to supply the base URL and pool-size
 * tuning appropriate for its workload.
 */
public final class FusionRestClientFactory
{
  private FusionRestClientFactory()
  {
  }

  /**
   * Creates an {@link OAuth2AuthorizedClientManager} configured for
   * client-credentials flow. Centralises the boilerplate that is otherwise
   * duplicated in each service's client-config class.
   *
   * @param registrationId unique registration identifier (e.g.
   *                       {@code "fusion-hcm"})
   * @param clientId       OAuth2 client ID
   * @param clientSecret   OAuth2 client secret
   * @param scope          requested scope
   * @param tokenUri       token endpoint URI
   */
  public static OAuth2AuthorizedClientManager buildClientCredentialsManager(String registrationId, String clientId,
      String clientSecret, String scope, String tokenUri)
  {
    var registration = ClientRegistration.withRegistrationId(registrationId).clientId(clientId)
        .clientSecret(clientSecret).authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).scope(scope)
        .tokenUri(tokenUri).build();
    var repo = new InMemoryClientRegistrationRepository(registration);
    var service = new InMemoryOAuth2AuthorizedClientService(repo);
    var provider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build();
    var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(repo, service);
    manager.setAuthorizedClientProvider(provider);
    return manager;
  }

  /**
   * Creates a pooled, pre-configured {@link RestClient} for a Fusion endpoint.
   *
   * @param baseUrl               Fusion REST base URL (e.g.
   *                              {@code https://host/hcmRestApi/resources/latest})
   * @param username              Fusion username (injected from
   *                              {@code fusion.username})
   * @param password              Fusion password (injected from
   *                              {@code fusion.password})
   * @param maxConnPerRoute       maximum connections per route for the HTTP pool
   * @param maxConnTotal          maximum total connections for the HTTP pool
   * @param connectTimeoutSeconds TCP connect (and pool-wait) timeout in seconds
   * @param readTimeoutSeconds    response (read) timeout in seconds
   */
  public static RestClient build(String baseUrl, String username, String password, int maxConnPerRoute,
      int maxConnTotal, int connectTimeoutSeconds, int readTimeoutSeconds)
  {
    String credentials = Base64.getEncoder()
        .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

    var connectionConfig = ConnectionConfig.custom().setConnectTimeout(Timeout.ofSeconds(connectTimeoutSeconds))
        .build();

    var requestConfig = RequestConfig.custom().setConnectionRequestTimeout(Timeout.ofSeconds(connectTimeoutSeconds))
        .setResponseTimeout(Timeout.ofSeconds(readTimeoutSeconds)).build();

    var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
        .setDefaultConnectionConfig(connectionConfig).setMaxConnPerRoute(maxConnPerRoute).setMaxConnTotal(maxConnTotal)
        .build();

    var httpClient = HttpClients.custom().setConnectionManager(connectionManager).setDefaultRequestConfig(requestConfig)
        .build();

    return RestClient.builder().baseUrl(baseUrl).requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
        .defaultHeader(HttpHeaders.ACCEPT, "application/json").defaultHeader("REST-Framework-Version", "9")
        .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip").build();
  }

  /**
   * Creates a pooled, pre-configured {@link RestClient} for a Fusion endpoint
   * that authenticates using OAuth2 client-credentials.
   *
   * @param baseUrl                 Fusion REST base URL
   * @param authorizedClientManager OAuth2 authorized-client manager
   * @param registrationId          OAuth2 client registration ID (must match a
   *                                {@code ClientRegistration} in the manager)
   * @param maxConnPerRoute         maximum connections per route for the HTTP
   *                                pool
   * @param maxConnTotal            maximum total connections for the HTTP pool
   * @param connectTimeoutSeconds   TCP connect (and pool-wait) timeout in seconds
   * @param readTimeoutSeconds      response (read) timeout in seconds
   */
  public static RestClient buildOAuth2(String baseUrl, OAuth2AuthorizedClientManager authorizedClientManager,
      String registrationId, int maxConnPerRoute, int maxConnTotal, int connectTimeoutSeconds, int readTimeoutSeconds)
  {
    var connectionConfig = ConnectionConfig.custom().setConnectTimeout(Timeout.ofSeconds(connectTimeoutSeconds))
        .build();

    var requestConfig = RequestConfig.custom().setConnectionRequestTimeout(Timeout.ofSeconds(connectTimeoutSeconds))
        .setResponseTimeout(Timeout.ofSeconds(readTimeoutSeconds)).build();

    var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
        .setDefaultConnectionConfig(connectionConfig).setMaxConnPerRoute(maxConnPerRoute).setMaxConnTotal(maxConnTotal)
        .build();

    var httpClient = HttpClients.custom().setConnectionManager(connectionManager).setDefaultRequestConfig(requestConfig)
        .build();

    var interceptor = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
    interceptor.setClientRegistrationIdResolver(request -> registrationId);

    return RestClient.builder().baseUrl(baseUrl).requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
        .requestInterceptor(interceptor).defaultHeader(HttpHeaders.ACCEPT, "application/json")
        .defaultHeader("REST-Framework-Version", "9").defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip").build();
  }

  /**
   * Creates an {@link OAuth2AuthorizedClientManager} for client-credentials flow
   * with a fixed service-identity principal substituted for
   * anonymous/unauthenticated callers.
   *
   * <p>
   * {@code OAuth2ClientHttpRequestInterceptor} resolves the outbound principal
   * from the inbound security context. For machine-to-machine (M2M) flows the
   * caller is often anonymous, which {@code InMemoryOAuth2AuthorizedClientService}
   * rejects because it uses the principal name as a cache key. This method wraps
   * the delegate so that any request whose principal name is blank or empty is
   * re-issued with a fixed service identity derived from the registration ID.
   *
   * @param registrationId unique registration identifier (e.g.
   *                       {@code "fusion-hcm"})
   * @param clientId       OAuth2 client ID
   * @param clientSecret   OAuth2 client secret
   * @param scope          requested scope
   * @param tokenUri       token-endpoint URI
   */
  public static OAuth2AuthorizedClientManager buildClientManager(String registrationId, String clientId,
      String clientSecret, String scope, String tokenUri)
  {
    OAuth2AuthorizedClientManager delegate = buildClientCredentialsManager(registrationId, clientId, clientSecret,
        scope, tokenUri);
    var servicePrincipal = new AnonymousAuthenticationToken(registrationId, registrationId,
        AuthorityUtils.createAuthorityList("ROLE_SERVICE"));
    return authorizeRequest ->
    {
      OAuth2AuthorizeRequest request = authorizeRequest;
      if (!StringUtils.hasText(request.getPrincipal().getName()))
      {
        request = OAuth2AuthorizeRequest.withClientRegistrationId(request.getClientRegistrationId())
            .principal(servicePrincipal).attributes(attrs -> attrs.putAll(authorizeRequest.getAttributes())).build();
      }
      return delegate.authorize(request);
    };
  }

  /**
   * Wraps a {@link RestClient} in a Spring HTTP Interface
   * {@link HttpServiceProxyFactory}.
   *
   * @param restClient the RestClient to adapt
   * @return a proxy factory ready to create HTTP interface client proxies
   */
  public static HttpServiceProxyFactory buildProxyFactory(RestClient restClient)
  {
    return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build();
  }
}
