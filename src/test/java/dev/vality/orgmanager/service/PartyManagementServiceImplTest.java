package dev.vality.orgmanager.service;

import dev.vality.damsel.payment_processing.PartyExists;
import dev.vality.damsel.payment_processing.PartyManagementSrv;
import dev.vality.damsel.payment_processing.PartyParams;
import dev.vality.orgmanager.exception.PartyManagementException;
import org.apache.thrift.TException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static dev.vality.orgmanager.TestObjectFactory.randomString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartyManagementServiceImplTest {

    private PartyManagementService partyManagementService;

    @Mock
    private PartyManagementSrv.Iface partyManagementClient;

    @BeforeEach
    void setUp() {
        partyManagementService = new PartyManagementServiceImpl(partyManagementClient);
    }

    @Test
    void shouldThrowPartyManagementExceptionOnCreateParty() throws TException {
        doThrow(new TException())
                .when(partyManagementClient).create(anyString(), any(PartyParams.class));
        String partyId = randomString();
        String userId = randomString();
        String email = randomString();

        var exception =
                assertThrows(
                        PartyManagementException.class,
                        () -> partyManagementService.createParty(partyId, userId, email)
                );

        assertTrue(exception.getMessage().contains(String.format(
                "Exception during party creation. (partyId: %s, userId: %s, email: %s)", partyId, userId, email)));
    }

    @Test
    void shouldCreatePartyIfPartyExistThrown() throws TException {
        doThrow(new PartyExists())
                .when(partyManagementClient).create(anyString(), any(PartyParams.class));
        String partyId = randomString();
        String userId = randomString();
        String email = randomString();

        partyManagementService.createParty(partyId, userId, email);

        verify(partyManagementClient, times(1))
                .create(anyString(), any(PartyParams.class));
    }

    @Test
    void shouldCreateParty() throws TException {
        String partyId = randomString();
        String userId = randomString();
        String email = randomString();

        partyManagementService.createParty(partyId, userId, email);

        ArgumentCaptor<String> partyIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PartyParams> partyParamsCaptor = ArgumentCaptor.forClass(PartyParams.class);

        verify(partyManagementClient, times(1))
                .create(partyIdCaptor.capture(), partyParamsCaptor.capture());

        assertEquals(1, partyIdCaptor.getAllValues().size());
        assertEquals(partyId, partyIdCaptor.getValue());

        assertEquals(1, partyParamsCaptor.getAllValues().size());
        assertEquals(email, partyParamsCaptor.getValue().getContactInfo().getEmail());
    }
}
