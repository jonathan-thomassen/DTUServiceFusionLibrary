package dtu.fusion;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FusionRestClientFactoryTest
{
  @Test
  void buildReturnsConfiguredRestClient()
  {
    RestClient client = FusionRestClientFactory.build("http://localhost", "fusion-user", "s3cr3t", 2, 5, 10, 30);

    assertThat(client).isNotNull();
  }

  @Test
  void buildOAuth2ReturnsConfiguredRestClient()
  {
    OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);

    RestClient client = FusionRestClientFactory.buildOAuth2("http://localhost", manager, "fusion-erp", 2, 5, 10, 30);

    assertThat(client).isNotNull();
  }

  @Test
  void constructorIsPrivate() throws Exception
  {
    Constructor<FusionRestClientFactory> ctor = FusionRestClientFactory.class.getDeclaredConstructor();

    assertThat(ctor.canAccess(null)).isFalse();

    ctor.setAccessible(true);
    ctor.newInstance(); // covers the private constructor body
  }
}
