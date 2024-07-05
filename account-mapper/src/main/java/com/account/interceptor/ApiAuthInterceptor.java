package com.account.interceptor;

import java.io.IOException;

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
   @Override
   public ClientHttpResponse intercept(HttpRequest request, byte[] body,
         ClientHttpRequestExecution execution) throws IOException {
      final String host = request.getURI().getHost();
      if (Constants.GITHUB_API_HOST.equals(host)) {
         final HttpHeaders headers = request.getHeaders();
         headers.add("Accept", Constants.GITHUB_API_V3_HEADER);
         headers.add("Authorization",
               "token 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");
      } else if (Constants.FRESHDESK_API_HOST.equals(host)) {
         final HttpHeaders headers = request.getHeaders();
         headers.setBasicAuth("1mIXRTQnuiqYzpdQZE6Y", "X");
      }
      return execution.execute(request, body);
   }
}
