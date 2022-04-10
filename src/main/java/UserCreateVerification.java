import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.lang.reflect.Type;
import java.util.Map;

public class UserCreateVerification implements RequestHandler<SNSEvent, String> {

    final private static String OK = "200 OK";

    final private static String BAD_REQUEST = "400 BAD REQUEST";

    final private static String SUBJECT = "CSYE6225 Webapp Account Register Confirmation";

    final private static String SEND_NAME = "noreply";

    final private static String confirmationURI =  "/v1/verifyUserEmail";

    final private static String EMAIL_TEMP = "<p>Hello %s,<br>" + // first name
            "You're receiving this email because your email address was used to register a webapp account.<br>" +
            "Please click this link to verify your account register:<br>" +
            "<a href=\"%s\">%s</a><br>" + // token
            "the link will be expired in 5 minutes<br>" +
            "Kind Regards<br>" +
            "CSYE6225 Spring 2022<br>" +
            "%s</p>"; // domain

    public String handleRequest(SNSEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("start processing");

        String jsonMessage = event.getRecords().get(0).getSNS().getMessage();
        Gson gson = new Gson();
        Type messageType = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> message = gson.fromJson(jsonMessage, messageType);
        logger.log(message.toString());

        Region region = Region.of(System.getenv("AWS_REGION"));
        SesClient sesClient = SesClient.builder().region(region).build();
        String domain = System.getenv("DOMAIN");
        String sender = SEND_NAME + "@" + domain;
        String tokenLink = domain + confirmationURI + "?email=" + message.get("email") + "&token=" + message.get("token");
        String content = String.format(EMAIL_TEMP,
                message.get("first_name"),
                tokenLink,
                tokenLink,
                domain);
        logger.log("sender: " + sender);
        send(sesClient, sender, message.get("email"), SUBJECT, content, logger);

        return OK;
    }

    public static void send(SesClient sesClient,
                            String sender,
                            String recipient,
                            String subject,
                            String bodyHtml,
                            LambdaLogger logger) {
        Destination destination = Destination.builder()
                .toAddresses(recipient)
                .build();
        Content content = Content.builder()
                .data(bodyHtml)
                .build();
        Content sub = Content.builder()
                .data(subject)
                .build();
        Body body = Body.builder()
                .html(content)
                .build();
        Message message = Message.builder()
                .subject(sub)
                .body(body).build();

        SendEmailRequest emailRequest = SendEmailRequest.builder()
                .destination(destination)
                .message(message)
                .source(sender)
                .build();
        logger.log("emailRequest: " + emailRequest.toString());
        try {
            logger.log("try send: ");
            sesClient.sendEmail(emailRequest);
        } catch (SesException e) {
            logger.log("failed");
            e.printStackTrace();
            logger.log("error: " + e.awsErrorDetails().errorMessage());
        }

    }
}
