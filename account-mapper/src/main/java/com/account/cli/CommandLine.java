package com.account.cli;

import java.util.Arrays;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.account.constants.Constants;
import jakarta.annotation.PreDestroy;

@Component
@ConditionalOnProperty(name = "command.line.interface.enabled", havingValue = "true")
public class CommandLine implements CommandLineRunner {
   private final Logger _logger = LoggerFactory.getLogger(getClass());
   private final Scanner _scanner;

   public CommandLine() {
      _scanner = new Scanner(System.in);
   }

   @Override
   public void run(String... args) {
      do {
         //@formatter:off
         System.out.println("""
            ===== Choose an option: ====
            1. Create a Freshdesk contact from an existing external account.
            0. Exit."""
         );
         //@formatter:on
         switch (_scanner.nextLine()) {
         case "1":
            createContact();
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
      System.out.printf("Account origin (%s)): %n", Arrays.asList(
            Constants.FreshdeskContactExternalAccountOrigin.values()));
      final String accountOriginString = _scanner.nextLine();
      if (accountOriginString.isEmpty()) {
         System.out.println("Empty account origin!");
         return;
      }
      final Constants.FreshdeskContactExternalAccountOrigin accountOrigin;
      try {
         accountOrigin = Constants.FreshdeskContactExternalAccountOrigin.valueOf(
               accountOriginString.toUpperCase());
      } catch (final IllegalArgumentException e) {
         System.out.println("Unsupported account origin!");
         return;
      }
      System.out.println("Account name: ");
      final String account = _scanner.nextLine();
      if (account.isEmpty()) {
         System.out.println("Missing 'account' argument.");
         return;
      }
      _logger.debug(
            "Received a request to create a Freshdesk contact for account {}@{}.",
            account, accountOrigin);
   }
}
