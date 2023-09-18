package com.SampleProject.Imapnio;

import com.yahoo.imapnio.async.client.ImapAsyncClient;
import com.yahoo.imapnio.async.client.ImapAsyncCreateSessionResponse;
import com.yahoo.imapnio.async.client.ImapAsyncSession;
import com.yahoo.imapnio.async.client.ImapAsyncSessionConfig;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Session {
    final int numOfThreads ;
    final ImapAsyncClient imapClient;

    public Session() throws SSLException {
         this.numOfThreads = 1;
       this.imapClient = new ImapAsyncClient(numOfThreads);
    }

    public ImapAsyncCreateSessionResponse getSession() throws URISyntaxException, ExecutionException, InterruptedException {
        final URI serverUri = new URI("imaps://imap.mail.yahoo.com:993");
        final ImapAsyncSessionConfig config = new ImapAsyncSessionConfig();
        config.setConnectionTimeoutMillis(5000);
        config.setReadTimeoutMillis(6000);
        final List<String> sniNames = null;

        final InetSocketAddress localAddress = null;
        final Future<ImapAsyncCreateSessionResponse> future = imapClient.createSession(serverUri, config, localAddress, sniNames,
                ImapAsyncSession.DebugMode.DEBUG_OFF);
        return future.get();
    }
}
