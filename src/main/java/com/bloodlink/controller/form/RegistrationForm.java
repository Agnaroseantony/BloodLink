package com.bloodlink.controller.form;

import com.bloodlink.model.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.*;

public class RegistrationForm extends DonorForm {
    @AssertTrue(message = "Password must fit within 72 UTF-8 bytes. Use fewer characters for non-ASCII passwords.")
    public boolean isPasswordByteLengthValid() { return password == null || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72; }
    @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,40}", message = "Use 3–40 letters, numbers, dots, underscores or hyphens.")
    private String username;
    @NotBlank @Size(min = 8, max = 72)
    private String password;
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
