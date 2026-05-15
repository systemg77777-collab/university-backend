import com.resend.*;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${app.resend.api-key}")
    private String apiKey;

    @Value("${app.mail.from}")
    private String fromEmail;

    public void sendResetCode(String toEmail, String code) {
        Resend resend = new Resend(apiKey);

        CreateEmailOptions params = CreateEmailOptions.builder()
            .from(fromEmail)
            .to(toEmail)
            .subject("Your OJT System Reset Code")
            .html("<strong>Your password reset code is: " + code + "</strong>")
            .build();

        try {
            CreateEmailResponse data = resend.emails().send(params);
            System.out.println("Email sent successfully! ID: " + data.getId());
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
}
