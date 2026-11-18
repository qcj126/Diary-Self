package diary.file.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.auth.CurrentUser;
import diary.file.entity.dto.ImageUploadInitReqDto;
import diary.file.entity.vo.ImageUploadConfigVO;
import diary.file.entity.vo.ImageUploadInitVO;
import diary.file.service.uploadservice.ImageChunkUploadService;
import diary.file.service.uploadservice.ImageUploadCompleteService;
import diary.file.service.uploadservice.ImageUploadInitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ImageMultipartUploadControllerTest {

    private static final long USER_ID = 10001L;
    private static final String SHA256 = "a".repeat(64);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ImageUploadInitService uploadInitService;
    private ImageChunkUploadService chunkUploadService;
    private ImageUploadCompleteService uploadCompleteService;
    private CurrentUser currentUser;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        uploadInitService = org.mockito.Mockito.mock(ImageUploadInitService.class);
        chunkUploadService = org.mockito.Mockito.mock(ImageChunkUploadService.class);
        uploadCompleteService = org.mockito.Mockito.mock(ImageUploadCompleteService.class);
        currentUser = org.mockito.Mockito.mock(CurrentUser.class);
        mockMvc = standaloneSetup(new ImageMultipartUploadController(
                uploadInitService,
                currentUser,
                chunkUploadService,
                uploadCompleteService
        )).build();
    }

    @Test
    void initializeUploadUsesTrustedCurrentUserAndReturnsServiceResult() throws Exception {
        ImageUploadInitVO serviceResult = ImageUploadInitVO.builder()
                .imageId(9001L)
                .uploadId("business-upload-id")
                .instantUpload(false)
                .chunkSize(262_144)
                .totalChunks(2)
                .uploadedChunkIndexes(List.of(0))
                .build();
        when(currentUser.getUser()).thenReturn(USER_ID);
        when(uploadInitService.initialize(org.mockito.ArgumentMatchers.any(ImageUploadInitReqDto.class),
                org.mockito.ArgumentMatchers.eq(USER_ID))).thenReturn(serviceResult);

        mockMvc.perform(post("/file/upload/images/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson(99999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.imageId").value(9001L))
                .andExpect(jsonPath("$.data.uploadId").value("business-upload-id"))
                .andExpect(jsonPath("$.data.instantUpload").value(false))
                .andExpect(jsonPath("$.data.uploadedChunkIndexes[0]").value(0))
                .andExpect(jsonPath("$.data.ossUploadId").doesNotExist());

        ArgumentCaptor<ImageUploadInitReqDto> requestCaptor = ArgumentCaptor.forClass(ImageUploadInitReqDto.class);
        verify(uploadInitService).initialize(requestCaptor.capture(), org.mockito.ArgumentMatchers.eq(USER_ID));
        ImageUploadInitReqDto request = requestCaptor.getValue();
        assertEquals("photo.jpg", request.getOriginalName());
        assertEquals(300_000L, request.getFileSize());
        assertEquals("image/jpeg", request.getMimeType());
        assertEquals(SHA256, request.getSha256());
        assertEquals(262_144, request.getChunkSize());
        assertEquals(2, request.getTotalChunks());
        assertEquals(2_000, request.getImageTypeCode());
    }

    @Test
    void initializeUploadDoesNotCallServiceWhenCurrentUserIsMissing() throws Exception {
        when(currentUser.getUser()).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/file/upload/images/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson(null)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(uploadInitService);
    }

    @Test
    void initializeUploadRejectsMissingBodyBeforeCallingDependencies() throws Exception {
        mockMvc.perform(post("/file/upload/images/init")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(uploadInitService, currentUser);
    }

    @Test
    void getUploadConfigReturnsServerConfigurationWithoutRequiringCurrentUser() throws Exception {
        when(uploadInitService.getUploadConfig()).thenReturn(ImageUploadConfigVO.builder()
                .chunkSize(262_144)
                .maxImageSize(104_857_600)
                .allowedMimeTypes(List.of("image/png", "image/jpeg"))
                .allowedExtensions(List.of("png", "jpg", "jpeg"))
                .build());

        mockMvc.perform(get("/file/upload/images/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chunkSize").value(262_144))
                .andExpect(jsonPath("$.data.maxImageSize").value(104_857_600))
                .andExpect(jsonPath("$.data.allowedMimeTypes[0]").value("image/png"))
                .andExpect(jsonPath("$.data.allowedExtensions[2]").value("jpeg"));

        verify(uploadInitService).getUploadConfig();
        verify(currentUser, never()).getUser();
    }

    @Test
    void legacyWholeFileUploadEndpointIsRemoved() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "files", "legacy.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/file/upload/images")
                        .file(image)
                        .param("code", "1000"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(uploadInitService, chunkUploadService, uploadCompleteService, currentUser);
    }

    private String validRequestJson(Long untrustedUserId) throws Exception {
        java.util.Map<String, Object> request = new java.util.LinkedHashMap<>();
        request.put("originalName", "photo.jpg");
        request.put("fileSize", 300_000L);
        request.put("mimeType", "image/jpeg");
        request.put("sha256", SHA256);
        request.put("chunkSize", 262_144);
        request.put("totalChunks", 2);
        request.put("imageTypeCode", 2_000);
        if (untrustedUserId != null) {
            request.put("userId", untrustedUserId);
        }
        return objectMapper.writeValueAsString(request);
    }
}
