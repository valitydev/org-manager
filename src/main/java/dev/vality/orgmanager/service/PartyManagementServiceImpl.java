package dev.vality.orgmanager.service;

import dev.vality.damsel.domain.PartyContactInfo;
import dev.vality.damsel.payment_processing.PartyExists;
import dev.vality.damsel.payment_processing.PartyManagementSrv;
import dev.vality.damsel.payment_processing.PartyParams;
import dev.vality.orgmanager.exception.PartyManagementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PartyManagementServiceImpl implements PartyManagementService {

    private final PartyManagementSrv.Iface partyManagementClient;

    @Override
    public void createParty(String partyId, String userId, String email) {
        PartyParams partyParams = new PartyParams(new PartyContactInfo(email));
        try {
            partyManagementClient.create(partyId, partyParams);
        } catch (PartyExists ex) {
            log.warn("Party already exists. (partyId: {}, userId: {}, email: {})", partyId, userId, email);
        } catch (TException ex) {
            throw new PartyManagementException(
                    String.format("Exception during party creation. (partyId: %s, userId: %s, email: %s)",
                            partyId, userId, email),
                    ex);
        }

        log.info("Created party. (partyId: {}, userId: {}, email: {})", partyId, userId, email);
    }
}
