package com.account.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.account.constants.Constants;

/**
 * A service to retrieve information about a GitHub account.
 */
@Service
public class GitHubAccountService {
   private final Logger _logger = LoggerFactory.getLogger(getClass());
   private final RestTemplate restTemplate;

   @Autowired
   public GitHubAccountService(final RestTemplate restTemplate) {
      this.restTemplate = restTemplate;
   }

   /**
    * Retrieves the {@link GithubAccountInfo} of a given account using the
    * configured REST API.
    *
    * @param account The account to retrieve information for.
    * @return The retrieved {@GithubAccountInfo} data.
    */
   public GithubAccountInfo get(final String account) {
      _logger.info(
            String.format("Retrieving information about GitHub user '%s'.",
                  account));

      final String url = UriComponentsBuilder.fromHttpUrl(
            Constants.GITHUB_API_URL + Constants.GITHUB_USERS_API_PATH_TEMPLATE
                  + account).toUriString();
      final HttpEntity<Void> requestEntity = new HttpEntity<>(
            new HttpHeaders());

      final GithubAccountInfo userAccount = restTemplate.exchange(url,
            HttpMethod.GET, requestEntity, GithubAccountInfo.class).getBody();
      _logger.info(String.format(
            "Successfully retrieved information about GitHub user '%s'.",
            account));
      _logger.debug(String.format("GitHub user: %s", userAccount));
      return userAccount;
   }
}
