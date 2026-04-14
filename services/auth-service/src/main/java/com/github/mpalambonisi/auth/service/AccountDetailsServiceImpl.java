package com.github.mpalambonisi.auth.service;

import com.github.mpalambonisi.auth.repository.AccountRepository;
import java.util.ArrayList;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Implementation of UserDetailsService to load user-specific data. */
@Service
@RequiredArgsConstructor
public class AccountDetailsServiceImpl implements UserDetailsService {

  private final AccountRepository accountRepository;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    final String sanitisedEmail = email.toLowerCase(Locale.ROOT);
    var account =
        accountRepository
            .findByEmail(sanitisedEmail.trim())
            .orElseThrow(() -> new UsernameNotFoundException("Email does not exist: " + email));
    return new User(account.getEmail(), account.getPasswordHash(), new ArrayList<>());
  }
}
