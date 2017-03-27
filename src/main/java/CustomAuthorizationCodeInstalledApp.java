import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;

import java.io.IOException;

public class CustomAuthorizationCodeInstalledApp extends AuthorizationCodeInstalledApp {
  /**
   * @param flow     authorization code flow
   * @param receiver verification code receiver
   */
  public CustomAuthorizationCodeInstalledApp(AuthorizationCodeFlow flow, VerificationCodeReceiver receiver) {
    super(flow, receiver);
  }

  protected void onAuthorization(AuthorizationCodeRequestUrl authorizationUrl) throws IOException {
    authorizationUrl.setState("xyz");
//    super.onAuthorization(authorizationUrl);
    System.out.println(authorizationUrl);
  }
}
