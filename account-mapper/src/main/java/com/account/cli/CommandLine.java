package com.account.cli;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.account.constants.Constants;
import com.account.entity.CreateContactTaskEntity;
import com.account.service.CreateContactTaskService;
import jakarta.annotation.PreDestroy;

@Component
@ConditionalOnProperty(name = "command.line.interface.enabled", havingValue = "true")
public class CommandLine implements CommandLineRunner {
   private final Logger _logger = LoggerFactory.getLogger(getClass());
   private final Scanner _scanner;
   private final CreateContactTaskService _createContactTaskService;

   public CommandLine(final CreateContactTaskService createContactTaskService) {
      _createContactTaskService = createContactTaskService;
      _scanner = new Scanner(System.in);
   }

   @Override
   public void run(String... args) {
      do {
         //@formatter:off
         System.out.println("""
            ===== Choose an option: ====
            1. Create a Freshdesk contact from an existing external account.
            2. List all available Freshdesk contacts.
            0. Exit."""
         );
         //@formatter:on
         switch (_scanner.nextLine()) {
         case "1":
            createContact();
            break;
         case "2":
            System.out.println("All available contacts: \n" + listContacts());
            break;
         case "0":
            System.out.println("Goodbye!");
            System.exit(0);
         default:
            System.out.println("Unknown option selected!");
            break;
         }
      } while (true);
   }

   @PreDestroy
   public void preDestroy() {
      _scanner.close();
   }

   private void createContact() {
      System.out.printf("Account origin (%s)): %n",
            Arrays.asList(Constants.AccountOrigin.values()));
      final String accountOriginString = _scanner.nextLine();
      System.out.println("Account name: ");
      final String account = _scanner.nextLine();
      System.out.println("Freshdesk domain: ");
      final String domain = _scanner.nextLine();
      try {
         _logger.debug(
               "Received a request to create a Freshdesk contact on domain {} for account {}@{}.",
               domain, account, accountOriginString);
         _createContactTaskService.create(account, accountOriginString, domain);
      } catch (final IllegalArgumentException e) {
         _logger.error(e.getMessage());
      } catch (final RuntimeException e) {
         _logger.error("An unexpected error occurred.", e);
      }
   }

   public List<CreateContactTaskEntity> listContacts() {
      return _createContactTaskService.list();
   }
}
