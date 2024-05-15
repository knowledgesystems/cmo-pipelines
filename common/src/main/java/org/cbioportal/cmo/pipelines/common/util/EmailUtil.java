/*
 * Copyright (c) 2016, 2018, 2023, 2024 Memorial Sloan Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan Kettering Cancer
 * Center has been advised of the possibility of such damage.
 */

/*
 * This file is part of cBioPortal CMO-Pipelines.
 *
 * cBioPortal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.cbioportal.cmo.pipelines.common.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author heinsz
 */
@Configuration
public class EmailUtil {
    @Value("${email.server}")
    private  String server;
    @Value("${email.sender}")
    private  String sender;
    @Value("${email.subject}")
    private  String subject;
    @Value("${email.recipient}")
    private  String recipient;

    private Session session;
    private Properties properties;

    @Bean
    public EmailUtil EmailUtil() {
        return new EmailUtil();
    }

    public EmailUtil() {
        properties = System.getProperties();
        session = Session.getDefaultInstance(properties);
    }

    public void sendEmailToDefaultRecipient(String subject, String body) {
        String[] recipients = new String[1];
        recipients[0] = this.recipient;
        sendEmail(this.sender, recipients, subject, body);
    }

    public void sendEmail(String sender, String[] recipients, String subject, String body) {
        try {
            if (recipients == null || recipients.length < 1) {
                return;
            }
            properties.setProperty("mail.smtp.host", server);
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(sender));
            InternetAddress[] recipientEmails = new InternetAddress[recipients.length];
            for (int i = 0; i < recipients.length; i++) {
                recipientEmails[i] = new InternetAddress(recipients[i]);
            }
            message.addRecipients(Message.RecipientType.TO, recipientEmails);
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
        }
        catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }

    public void sendErrorEmail(String pipelineName, List<Throwable> exceptions, String parameters) {
        String body = "An error occured while running the " + pipelineName + " with job parameters " + parameters + ".\n\n";
        List<String> messages = new ArrayList<>();
        for (Throwable exception : exceptions) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            messages.add(pw.toString());
        }
        body += String.join("\n\n", messages);
        sendEmailToDefaultRecipient(subject, body);
    }
}
