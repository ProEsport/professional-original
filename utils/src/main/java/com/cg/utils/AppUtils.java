package com.cg.utils;

import com.cg.exception.DataInputException;
import com.cg.utils.driver.GoogleDriveConfig;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.multipart.MultipartFile;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AppUtils {

    static final String stringAB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    @Autowired
    private GoogleDriveConfig googleDriveConfig;

    private static final String DATE_PATTERN = "yyyy-MM-dd";

    public ResponseEntity<?> mapErrorToResponse(BindingResult result) {
        List<FieldError> fieldErrors = result.getFieldErrors();
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : fieldErrors) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    public static LocalDate stringToLocalDate(String str) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        return LocalDate.parse(str, formatter);
    }

    public static String localDateToString(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        return formatter.format(date);
    }

    public String getUserName() {
        String userName;
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            userName = ((UserDetails) principal).getUsername();
        } else {
            userName = principal.toString();
        }

        return userName;
    }

    public String randomPassword(int length) {
        StringBuilder stringBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            stringBuilder.append(stringAB.charAt(new SecureRandom().nextInt(stringAB.length())));
        return stringBuilder.toString();
    }

    public String randomOtp(int length) {
        StringBuilder stringBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            stringBuilder.append(stringAB.charAt(new SecureRandom().nextInt(stringAB.length())));
        return stringBuilder.toString();
    }

    public String checkPrice(int price) {
        if (price < 10000)
            return "Giá sản phẩm ít nhất là 10.000 VNĐ.";

        if (price > 500000)
            return "Giá sản phẩm lớn nhất là 500.000 VNĐ.";

        if (price % 1000 != 0)
            return "Giá sản phẩm phải chia hết cho 1.000.";

        return "";
    }
    public String uploadAvartar(MultipartFile file, String name){
        try {
            if (file != null) {
                File fileMetadata = new File();
                fileMetadata.setName(name);
                File uploadFile = googleDriveConfig.getInstance()
                        .files()
                        .create(fileMetadata, new InputStreamContent(
                                file.getContentType(),
                                new ByteArrayInputStream(file.getBytes()))
                        )
                        .setFields("id").execute();
                googleDriveConfig.getInstance().permissions().create(uploadFile.getId(), setPermission("anyone", "reader")).execute();
                return "https://drive.google.com/uc?export=view&id="+uploadFile.getId();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataInputException("FAIL");
        }
        return "";
    }
    public void deleteFile(String fileId) throws GeneralSecurityException, IOException {
        googleDriveConfig.getInstance().files().delete(fileId).execute();
    }

    private Permission setPermission(String type, String role){
        Permission permission = new Permission();
        permission.setType(type);
        permission.setRole(role);
        return permission;
    }

}

