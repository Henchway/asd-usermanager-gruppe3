package at.ac.fhcampuswien.asd.rest.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

@Data
public class InboundUserChangePasswordDto {

    @NotBlank
    @NotEmpty
    private String oldPassword;
    @NotBlank
    @NotEmpty
    private String newPassword;
    @NotBlank
    @NotEmpty
    private String controlNewPassword;
}
