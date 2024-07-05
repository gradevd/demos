package com.account.interceptor;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import com.account.constants.Constants;

/**
 * An interceptor that sets the Authorization headers for API called.
 */
// TODO: Extract the API keys as env variables
@Component
public class ApiAuthInterceptor implements ClientHttpRequestInterceptor {
   private final String _githubApiKey;
   private final String _freshdeskApiKey;

   public ApiAuthInterceptor(
         @Value("${github.api.key}") final String githubApiKey,
         @Value("${freshdesk.api.key}") final String freshdeskApiKey) {
      _githubApiKey = githubApiKey;
      _freshdeskApiKey = freshdeskApiKey;
   }

   @Override
   public ClientHttpResponse intercept(HttpRequest request, byte[] body,
         ClientHttpRequestExecution execution) throws IOException {
      final String host = request.getURI().getHost();
      if (Constants.GITHUB_API_HOST.equals(host)) {
         final HttpHeaders headers = request.getHeaders();
         headers.add("Accept", Constants.GITHUB_API_V3_HEADER);
         headers.add("Authorization", "token " + _githubApiKey);
      } else if (host.endsWith(Constants.FRESHDESK_DOMAIN)) {
         final HttpHeaders headers = request.getHeaders();
         headers.setBasicAuth(_freshdeskApiKey, "X");
      }
      return execution.execute(request, body);
   }
}
