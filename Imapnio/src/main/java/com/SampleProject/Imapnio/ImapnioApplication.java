package com.SampleProject.Imapnio;

import com.sun.mail.iap.ByteArray;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.protocol.BODY;
import com.sun.mail.imap.protocol.FetchItem;
import com.sun.mail.imap.protocol.FetchResponse;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.ListInfo;
import com.sun.mail.util.ASCIIUtility;
import com.yahoo.imapnio.async.client.ImapAsyncClient;
import com.yahoo.imapnio.async.client.ImapAsyncCreateSessionResponse;
import com.yahoo.imapnio.async.client.ImapAsyncSession;
import com.yahoo.imapnio.async.client.ImapAsyncSessionConfig;
import com.yahoo.imapnio.async.data.Capability;
import com.yahoo.imapnio.async.data.ListInfoList;
import com.yahoo.imapnio.async.data.MessageNumberSet;
import com.yahoo.imapnio.async.request.CapaCommand;
import com.yahoo.imapnio.async.request.FetchCommand;
import com.yahoo.imapnio.async.request.ListCommand;
import com.yahoo.imapnio.async.request.LoginCommand;
import com.yahoo.imapnio.async.request.SelectFolderCommand;
import com.yahoo.imapnio.async.response.ImapAsyncResponse;
import com.yahoo.imapnio.async.response.ImapResponseMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Future;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class ImapnioApplication {

	public static String getBody(final BODY body) {
		final ByteArray textArray = body.getByteArray();
		final int start = textArray.getStart();
		final int end = textArray.getCount() + start;
		String msgBodyText = ASCIIUtility.toString(textArray.getBytes(), start, end);
		// Line terminations in local file use \n. Replace \r\n with \n in
		// received message body
		msgBodyText = msgBodyText.replaceAll("\r\n", "\n");
		return msgBodyText;
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(ImapnioApplication.class, args);

		try {
			// Create a ImapAsyncClient instance with number of threads to handle the server requests
			final int numOfThreads = 1;
			final ImapAsyncClient imapClient = new ImapAsyncClient(numOfThreads);

			// SETUP SESSION
			final URI serverUri = new URI("imaps://imap.mail.yahoo.com:993");
			final ImapAsyncSessionConfig config = new ImapAsyncSessionConfig();
			config.setConnectionTimeoutMillis(5000);
			config.setReadTimeoutMillis(6000);
			final List<String> sniNames = null;

			final InetSocketAddress localAddress = null;
			final Future<ImapAsyncCreateSessionResponse> future = imapClient.createSession(serverUri, config, localAddress, sniNames,
					ImapAsyncSession.DebugMode.DEBUG_OFF);


			ImapAsyncCreateSessionResponse sessionResponse = future.get();

			//this version is a future-based nio client.  Check whether future is done by following code.
			if (future.isDone()) {
				System.out.println("Future is done.");
			}
			// Get the session from the response
			ImapAsyncSession session = sessionResponse.getSession();

			// Executes the capability command
			final Future<ImapAsyncResponse> capaCmdFuture = session.execute(new CapaCommand());

			final ImapAsyncResponse resp = capaCmdFuture.get(5000, TimeUnit.MILLISECONDS);

			if (capaCmdFuture.isDone()) {
				System.out.println("Capability command is done.");
				final ImapResponseMapper mapper = new ImapResponseMapper();
				final Capability capa = mapper.readValue(resp.getResponseLines().toArray(new IMAPResponse[0]), Capability.class);
				final List<String> values = capa.getCapability("AUTH");
				for( String a : values) {
					System.out.println(a);
				}
			}

			// Executes the Login command
			final String userName = "deepakpatel813003@yahoo.com";
			final String password = "comlbxttldzhoaog";
			/** Template for IMAPResponse array. */
			final IMAPResponse[] IMAP_RESPONSE_ARRAY_TYPE = new IMAPResponse[0];
			final Future<ImapAsyncResponse> loginCmdFuture = session.execute(new LoginCommand(userName, password));

			final ImapAsyncResponse loginResp = loginCmdFuture.get(5000, TimeUnit.MILLISECONDS);

			if(loginCmdFuture.isDone()) {
				System.out.println("Login command is done.");
				final IMAPResponse[] r = loginResp.getResponseLines().toArray(IMAP_RESPONSE_ARRAY_TYPE);
				final Response lastResponse = r[r.length - 1];
				System.out.println(lastResponse);
			}



			// Executes the List command
			final String patter ="*";
			final String ref = "";
			/** Template for ListInfo array. */
			final ListInfo[] LIST_INFO_ARRAY_TYPE = new ListInfo[0];

			final Future<ImapAsyncResponse> folderCmdResp = session.execute(new ListCommand(ref, patter));
			final ImapAsyncResponse folderResp = folderCmdResp.get(5000, TimeUnit.MILLISECONDS);

			if(folderCmdResp.isDone()) {
				System.out.println("List Folder command is done.");
				final IMAPResponse[] r = folderResp.getResponseLines().toArray(IMAP_RESPONSE_ARRAY_TYPE);
				final Response lastResponse = r[r.length - 1];
				System.out.println(lastResponse);

				final ListInfoList listInfoList;
				final ImapResponseMapper parser = new ImapResponseMapper();
				listInfoList = parser.readValue(r, ListInfoList.class);

				if(listInfoList != null) {
					final ListInfo[] result = listInfoList.getListInfo().toArray(LIST_INFO_ARRAY_TYPE);
					for(ListInfo folder : result) {
						System.out.println("#   " + folder.name);
					}
				} else {
					throw new Exception("No folder exist");
				}
			}

			// Executes the fetch Message command
			final String selectedFolder = "INBOX"; // Replace with the desired folder name

			final Future<ImapAsyncResponse> selectCmdResps = session.execute(new SelectFolderCommand(selectedFolder));
			final ImapAsyncResponse selectResp = selectCmdResps.get(5000, TimeUnit.MILLISECONDS);

			MessageNumberSet[] messageSets = MessageNumberSet.createMessageNumberSets(new int[] { 1,2,3,4,5 });

			//final String dataItems = "BODY[HEADER.FIELDS (FROM)]";
			final String dataItems = "BODY[TEXT]";
			// Executes the fetch Message command
			final FetchCommand fetchCmd = new FetchCommand(messageSets, dataItems);
			final Future<ImapAsyncResponse> fetchCmdResp = session.execute(fetchCmd);
			final ImapAsyncResponse fetchResp = fetchCmdResp.get(5000, TimeUnit.MILLISECONDS);

			if (fetchCmdResp.isDone()) {
				System.out.println("Fetch Message command is done.");

				System.out.println(fetchResp.toString());

				final IMAPResponse[] r = fetchResp.getResponseLines().toArray(IMAP_RESPONSE_ARRAY_TYPE);
				//final ImapAsyncResponse[] r = fetchResp.getResponseLines().toArray(new ImapAsyncResponse[0]);

				for (IMAPResponse response : r) {
					//new FetchItem[0]
					final FetchResponse fetchResponse = new FetchResponse(response, new FetchItem[0]);
					System.out.println("++" + r.toString());
					final BODY body = fetchResponse.getItem(BODY.class);
					String b = getBody(body);
					System.out.println("-" + b);
				}
			}

		} catch (final Exception e ) {
			throw new Exception(e.getMessage());
		}

	}
}
