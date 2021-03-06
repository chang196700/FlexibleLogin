/*
 * This file is part of FlexibleLogin
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2018 contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.games647.flexiblelogin.tasks;

import com.github.games647.flexiblelogin.FlexibleLogin;
import com.github.games647.flexiblelogin.config.node.MailConfig;

import java.util.Arrays;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;

import org.spongepowered.api.entity.living.player.Player;

public class SendMailTask implements Runnable {

    private final FlexibleLogin plugin;
    private final Session session;
    private final Message mail;

    private final Player player;

    public SendMailTask(FlexibleLogin plugin, Player player, Session session, Message mail) {
        this.plugin = plugin;
        this.session = session;
        this.mail = mail;
        this.player = player;
    }

    @Override
    public void run() {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try (Transport transport = session.getTransport()) {
            // Prevent UnsupportedDataTypeException: no object DCH for MIME type multipart/alternative
            // cf. https://stackoverflow.com/questions/21856211/unsupporteddatatypeexception-no-object-dch-for-mime-type
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
            mc.addMailcap("text/html;; x-java-content-handler=flexiblelogin.mail.handlers.text_html");
            mc.addMailcap("text/xml;; x-java-content-handler=flexiblelogin.mail.handlers.text_xml");
            mc.addMailcap("text/plain;; x-java-content-handler=flexiblelogin.mail.handlers.text_plain");
            mc.addMailcap("multipart/*;; x-java-content-handler=flexiblelogin.mail.handlers.multipart_mixed");
            mc.addMailcap("message/rfc822;; x-java-content- handler=flexiblelogin.mail.handlers.message_rfc822");

            MailConfig mailConfig = plugin.getConfigManager().getGeneral().getMail();

            //connect to host and send message
            if (!transport.isConnected()) {
                String password = mailConfig.getPassword();
                transport.connect(mailConfig.getHost(), mailConfig.getAccount(), password);
            }

            transport.sendMessage(mail, mail.getAllRecipients());
            player.sendMessage(plugin.getConfigManager().getText().getMailSent());
        } catch (NoSuchProviderException providerEx) {
            plugin.getLogger().error("Transport provider not found", providerEx);
            plugin.getLogger().error("Registered providers: {}", Arrays.asList(session.getProviders()));

            player.sendMessage(plugin.getConfigManager().getText().getErrorExecutingCommand());
        } catch (MessagingException messagingEx) {
            plugin.getLogger().error("Error sending mail", messagingEx);
            player.sendMessage(plugin.getConfigManager().getText().getErrorExecutingCommand());
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }
}
