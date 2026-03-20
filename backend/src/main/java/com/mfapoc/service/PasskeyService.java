package com.mfapoc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mfapoc.entity.PasskeyCredential;
import com.mfapoc.entity.User;
import com.mfapoc.repository.PasskeyCredentialRepository;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasskeyService {

    private static final Logger log = LoggerFactory.getLogger(PasskeyService.class);

    private final PasskeyCredentialRepository credentialRepository;
    private final RelyingParty relyingParty;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Temporary storage for challenges (in production, use Redis or similar)
    private final Map<String, String> registrationChallenges = new ConcurrentHashMap<>();
    private final Map<String, String> assertionChallenges = new ConcurrentHashMap<>();

    public PasskeyService(PasskeyCredentialRepository credentialRepository,
                         @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.credentialRepository = credentialRepository;

        CredentialRepositoryAdapter credentialRepo = new CredentialRepositoryAdapter(credentialRepository);

        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
                .id("localhost")
                .name("MFA POC")
                .build();

        this.relyingParty = RelyingParty.builder()
                .identity(rpIdentity)
                .credentialRepository(credentialRepo)
                .origins(Set.of(allowedOrigins))
                .build();
    }

    public Map<String, Object> generateRegistrationOptions(User user) {
        UserIdentity userIdentity = UserIdentity.builder()
                .name(user.getUsername())
                .displayName(user.getUsername())
                .id(new ByteArray(longToBytes(user.getId())))
                .build();

        StartRegistrationOptions options = StartRegistrationOptions.builder()
                .user(userIdentity)
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                        .authenticatorAttachment(AuthenticatorAttachment.PLATFORM)
                        .residentKey(ResidentKeyRequirement.PREFERRED)
                        .userVerification(UserVerificationRequirement.PREFERRED)
                        .build())
                .build();

        PublicKeyCredentialCreationOptions creationOptions = relyingParty.startRegistration(options);

        try {
            String optionsJson = creationOptions.toCredentialsCreateJson();
            registrationChallenges.put(user.getUsername(), creationOptions.toJson());

            return objectMapper.readValue(optionsJson, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize registration options", e);
        }
    }

    public boolean verifyRegistration(User user, String credentialJson) {
        try {
            String storedOptions = registrationChallenges.remove(user.getUsername());
            if (storedOptions == null) {
                log.error("No registration challenge found for user: {}", user.getUsername());
                return false;
            }

            PublicKeyCredentialCreationOptions creationOptions =
                    PublicKeyCredentialCreationOptions.fromJson(storedOptions);
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> credential =
                    PublicKeyCredential.parseRegistrationResponseJson(credentialJson);

            RegistrationResult result = relyingParty.finishRegistration(
                    FinishRegistrationOptions.builder()
                            .request(creationOptions)
                            .response(credential)
                            .build()
            );

            // Save credential
            PasskeyCredential passkeyCredential = new PasskeyCredential();
            passkeyCredential.setUserId(user.getId());
            passkeyCredential.setCredentialId(result.getKeyId().getId().getBase64Url());
            passkeyCredential.setPublicKey(result.getPublicKeyCose().getBytes());
            passkeyCredential.setSignCount(result.getSignatureCount());
            passkeyCredential.setCredentialName("Passkey " + (credentialRepository.findByUserId(user.getId()).size() + 1));
            passkeyCredential.setAaguid(result.getAaguid().getHex());
            credentialRepository.save(passkeyCredential);

            return true;
        } catch (RegistrationFailedException | IOException e) {
            log.error("Passkey registration failed: {}", e.getMessage());
            return false;
        }
    }

    public Map<String, Object> generateAssertionOptions(User user) {
        StartAssertionOptions options = StartAssertionOptions.builder()
                .username(user.getUsername())
                .userVerification(UserVerificationRequirement.PREFERRED)
                .build();

        AssertionRequest assertionRequest = relyingParty.startAssertion(options);

        try {
            String optionsJson = assertionRequest.toCredentialsGetJson();
            assertionChallenges.put(user.getUsername(), assertionRequest.toJson());

            return objectMapper.readValue(optionsJson, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize assertion options", e);
        }
    }

    public boolean verifyAssertion(User user, String assertionJson) {
        try {
            String storedRequest = assertionChallenges.remove(user.getUsername());
            if (storedRequest == null) {
                log.error("No assertion challenge found for user: {}", user.getUsername());
                return false;
            }

            AssertionRequest assertionRequest = AssertionRequest.fromJson(storedRequest);
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> credential =
                    PublicKeyCredential.parseAssertionResponseJson(assertionJson);

            AssertionResult result = relyingParty.finishAssertion(
                    FinishAssertionOptions.builder()
                            .request(assertionRequest)
                            .response(credential)
                            .build()
            );

            if (result.isSuccess()) {
                // Update sign count
                String credId = result.getCredential().getCredentialId().getBase64Url();
                credentialRepository.findByCredentialId(credId).ifPresent(c -> {
                    c.setSignCount(result.getSignatureCount());
                    credentialRepository.save(c);
                });
                return true;
            }
            return false;
        } catch (AssertionFailedException | IOException e) {
            log.error("Passkey assertion failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean hasPasskeys(Long userId) {
        return credentialRepository.existsByUserId(userId);
    }

    private byte[] longToBytes(long value) {
        byte[] bytes = new byte[8];
        for (int i = 7; i >= 0; i--) {
            bytes[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return bytes;
    }

    // Inner adapter for WebAuthn credential repository
    private static class CredentialRepositoryAdapter implements com.yubico.webauthn.CredentialRepository {

        private final PasskeyCredentialRepository credentialRepository;

        public CredentialRepositoryAdapter(PasskeyCredentialRepository credentialRepository) {
            this.credentialRepository = credentialRepository;
        }

        @Override
        public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
            // This would need user lookup - simplified for POC
            return Set.of();
        }

        @Override
        public Optional<ByteArray> getUserHandleForUsername(String username) {
            return Optional.empty();
        }

        @Override
        public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
            return Optional.empty();
        }

        @Override
        public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
            return credentialRepository.findByCredentialId(credentialId.getBase64Url())
                    .map(c -> RegisteredCredential.builder()
                            .credentialId(credentialId)
                            .userHandle(userHandle)
                            .publicKeyCose(new ByteArray(c.getPublicKey()))
                            .signatureCount(c.getSignCount())
                            .build());
        }

        @Override
        public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
            return credentialRepository.findByCredentialId(credentialId.getBase64Url())
                    .map(c -> Set.of(RegisteredCredential.builder()
                            .credentialId(credentialId)
                            .userHandle(new ByteArray(new byte[]{0}))
                            .publicKeyCose(new ByteArray(c.getPublicKey()))
                            .signatureCount(c.getSignCount())
                            .build()))
                    .orElse(Set.of());
        }
    }
}
