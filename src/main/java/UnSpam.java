import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import com.google.api.services.gmail.Gmail;

import java.io.*;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UnSpam {
    private static final String APPLICATION_NAME = "UnSpam";

    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    private static HttpTransport HTTP_TRANSPORT;

    private static final List<String> SCOPES =
            Arrays.asList(GmailScopes.GMAIL_MODIFY, GmailScopes.GMAIL_LABELS);

    private static String[] unSpamList;
    private static List<EmailAccount> accountList;

    private static String DATA_PATH = "/Users/hiepns/verve-workspace/UnSpam/data/";

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    public static Credential authorize(String email) throws IOException {
        File dataStoreDir = new File(System.getProperty("user.home"), ".unspam/" + email);
        FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(dataStoreDir);

        // Load client secrets.
        InputStream in =
                UnSpam.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(dataStoreFactory)
                        .setAccessType("offline")
                        .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + dataStoreDir.getAbsolutePath());
        return credential;
    }

    public static Gmail getGmailService(String email) throws IOException {
        Credential credential = authorize(email);
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        String queryString = "";

        unSpamList = readLines(DATA_PATH + "unspam_list.txt");
        for (String email : unSpamList) {
            System.out.println(email);
            queryString = queryString + "from:" + email + " OR ";
        }
        queryString = queryString.substring(0, queryString.length() - 4);
        System.out.println("queryString: " + queryString);

        accountList = new ArrayList<EmailAccount>();
        String[] lines = readLines(DATA_PATH + "mail_list.txt");
        for (String line : lines) {
            String[] splitedLine = line.split("\\t");
            accountList.add(new EmailAccount(splitedLine[0], splitedLine[1]));
        }

        for (EmailAccount account : accountList) {
            System.out.println("Current email: " + account.email);

            // Build a new authorized API client service.
            Gmail service = getGmailService(account.email);
            String user = "me";

            List<Message> messageList = listSpamEmailMatchingQuery(service, "me", queryString);
            for (Message message : messageList) {
                System.out.println(message.getId());
                modifyMessage(service, "me", message.getId(), Arrays.asList("INBOX"), Arrays.asList("SPAM"));
            }
            Thread.sleep(4000);

        }

    }

    public static void modifyMessage(Gmail service, String userId, String messageId,
                                     List<String> labelsToAdd, List<String> labelsToRemove) throws IOException {
        ModifyMessageRequest mods = new ModifyMessageRequest().setAddLabelIds(labelsToAdd).setRemoveLabelIds(labelsToRemove);
        Message message = service.users().messages().modify(userId, messageId, mods).execute();

        System.out.println("Message id: " + message.getId());
        System.out.println(message.toPrettyString());
    }

    public static String[] readLines(String filePath) throws IOException {
        FileReader fileReader = new FileReader(filePath);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        List<String> lines = new ArrayList<String>();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            lines.add(line.trim());
        }
        bufferedReader.close();
        return lines.toArray(new String[lines.size()]);
    }

    public static List<Message> listSpamEmailMatchingQuery(Gmail service, String userId,
                                                          String query) throws IOException {
        Gmail.Users.Messages.List queryMessageList = service.users().messages().list(userId).setIncludeSpamTrash(true).setLabelIds(Arrays.asList("SPAM")).setQ(query);

        ListMessagesResponse response = queryMessageList.execute();

        List<Message> messages = new ArrayList<Message>();
        while (response.getMessages() != null) {
            messages.addAll(response.getMessages());
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken();
                response = queryMessageList.setPageToken(pageToken).execute();
            } else {
                break;
            }
        }

        return messages;
    }

}