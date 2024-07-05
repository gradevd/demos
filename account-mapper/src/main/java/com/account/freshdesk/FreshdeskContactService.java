package com.account.freshdesk;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.account.constants.Constants;

/**
 * A service used to retrieve  and update existing {@link FreshdeskContactInfo},
 * or create new ones.
 */
@Component
public class FreshdeskContactService {

   private final Logger _logger = LoggerFactory.getLogger(getClass());
   private final RestTemplate _restTemplate;

   @Autowired
   public FreshdeskContactService(final RestTemplate restTemplate) {
      _restTemplate = restTemplate;
   }

   /**
    * Retrieves a {@link FreshdeskContactInfo} by its unique external ID.
    *
    * @param externalId The unique_external_id of the contact.
    * @return An optional object containing the {@link FreshdeskContactInfo} if found.
    */
   public Optional<FreshdeskContactInfo> findByExternalId(
         final String freshdeskDomain, final String externalId) {
      _logger.info("Looking up a Freshdesk contact by external ID {}.",
            externalId);
      final HttpEntity<Void> requestEntity = new HttpEntity<>(
            new HttpHeaders());

      // Unfortunately there is no Freshdesk Query API by external ID, so we
      // need to retrieve all contacts and filter them ourselves.
      final ResponseEntity<List<FreshdeskContactInfo>> response = _restTemplate.exchange(
            getFreshdeskApiUrl(freshdeskDomain), HttpMethod.GET, requestEntity,
            new ParameterizedTypeReference<>() {
            });

      final List<FreshdeskContactInfo> contactInfos = response.getBody();
      _logger.debug("Successfully retrieved all available contact infos:\n{}",
            contactInfos);

      if (contactInfos == null) {
         return Optional.empty();
      }
      return contactInfos.stream().filter(
                  contactInfo -> externalId.equals(contactInfo.uniqueExternalId))
            .findFirst();
   }

   /**
    * Updates an existing {@link FreshdeskContactInfo}.
    *
    * @param contactId  The ID of the contact to update.
    * @param updateSpec The data to update.
    * @return The updated {@link FreshdeskContactInfo}
    */
   public FreshdeskContactInfo update(final String freshdeskDomain,
         final Long contactId, final FreshdeskContactSpec updateSpec) {
      _logger.info("Updating a Freshdesk Contact with ID {}.", contactId);

      final HttpEntity<FreshdeskContactSpec> requestEntity = new HttpEntity<>(
            updateSpec, new HttpHeaders());

      final ResponseEntity<FreshdeskContactInfo> response = _restTemplate.exchange(
            getFreshdeskApiUrl(freshdeskDomain, contactId.toString()),
            HttpMethod.PUT, requestEntity, FreshdeskContactInfo.class);

      _logger.info("Updated contact info {}.", response.getBody());
      return response.getBody();
   }

   /**
    * Creates a new {@link FreshdeskContactInfo}.
    *
    * @param createSpec The data used to create the contact.
    * @return The newly created {@link FreshdeskContactInfo}.
    */
   public FreshdeskContactInfo create(final String freshdeskDomain,
         final FreshdeskContactSpec createSpec) {
      _logger.info("Creating a Freshdesk Contact '{}'.", createSpec);
      final HttpEntity<FreshdeskContactSpec> requestEntity = new HttpEntity<>(
            createSpec, new HttpHeaders());

      final ResponseEntity<FreshdeskContactInfo> response = _restTemplate.exchange(
            getFreshdeskApiUrl(freshdeskDomain), HttpMethod.POST, requestEntity,
            FreshdeskContactInfo.class);

      _logger.info("Successfully created a Freshdesk contact {}.",
            response.getBody());
      return response.getBody();
   }

   private String getFreshdeskApiUrl(final String freshdeskDomain) {
      return getFreshdeskApiUrl(freshdeskDomain, "");
   }

   private String getFreshdeskApiUrl(final String freshdeskDomain,
         final String contactId) {
      return UriComponentsBuilder.fromHttpUrl(
                  String.format(Constants.FRESHDESK_API_URL_TEMPLATE, freshdeskDomain)
                        + Constants.FRESHDESK_CONTACTS_PATH + contactId)
            .toUriString();
   }
}
