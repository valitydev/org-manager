package dev.vality.orgmanager.service;

import dev.vality.damsel.message_sender.MailBody;
import dev.vality.damsel.message_sender.Message;
import dev.vality.damsel.message_sender.MessageMail;
import dev.vality.damsel.message_sender.MessageSenderSrv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailMessageSender {

    @Value("${dashboard.url}")
    private String dashboardUrl;
    @Value("${dudoser.mail-from}")
    private String mailFrom;

    private final MessageSenderSrv.Iface dudoserClient;

    public void send(String body, String email) {
        try {
            MessageMail messageMail = new MessageMail();
            messageMail.setMailBody(new MailBody(dashboardUrl + body));
            messageMail.setToEmails(List.of(email));
            messageMail.setSubject("Подтверждение вступления в организацию");
            messageMail.setFromEmail(mailFrom);
            log.info("Try to send message to email: {}", email);
            dudoserClient.send(Message.message_mail(messageMail));
        } catch (Exception ex) {
            log.warn("dudoserClient error", ex);
        }
    }
}
