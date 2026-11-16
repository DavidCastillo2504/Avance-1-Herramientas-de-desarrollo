package com.sabortradicion.app.storage;

import com.sabortradicion.app.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ImageStorageServiceTest {
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    @Test
    void uploadsValidImagesUnderTheMenuPrefixAndReturnsThePublicUrl() {
        S3Client s3 = mock(S3Client.class);
        var service = new ImageStorageService("sabor-imagenes", "us-east-1", "", s3);

        var response = service.upload(new MockMultipartFile("file", "foto.png", "image/png", PNG));

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo("sabor-imagenes");
        assertThat(captor.getValue().key()).startsWith("menu/").endsWith(".png");
        assertThat(captor.getValue().contentType()).isEqualTo("image/png");
        assertThat(response.url()).isEqualTo("https://sabor-imagenes.s3.us-east-1.amazonaws.com/" + response.key());
    }

    @Test
    void rejectsFilesThatAreNotImagesEvenIfTheyClaimToBe() {
        S3Client s3 = mock(S3Client.class);
        var service = new ImageStorageService("sabor-imagenes", "us-east-1", "", s3);
        var fake = new MockMultipartFile("file", "x.png", "image/png", "<script>alert(1)</script>".getBytes());

        assertThatThrownBy(() -> service.upload(fake))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Formato");
        verifyNoInteractions(s3);
    }

    @Test
    void explainsWhenUploadsAreNotConfigured() {
        var service = new ImageStorageService("", "us-east-1", "");

        assertThat(service.isEnabled()).isFalse();
        assertThatThrownBy(() -> service.upload(new MockMultipartFile("file", "foto.png", "image/png", PNG)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("S3_BUCKET");
    }

    @Test
    void usesTheConfiguredPublicBaseUrl() {
        S3Client s3 = mock(S3Client.class);
        var service = new ImageStorageService("sabor-imagenes", "us-east-1", "https://img.ejemplo.com/", s3);

        var response = service.upload(new MockMultipartFile("file", "foto.png", "image/png", PNG));

        assertThat(response.url()).startsWith("https://img.ejemplo.com/menu/");
    }
}
