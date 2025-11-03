package com.example.springbootjava.service;

import com.example.springbootjava.entity.Document;
import com.example.springbootjava.entity.User;
import com.example.springbootjava.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private AIService aiService;

    @Mock
    private LocalFileStorageService fileStorageService;

    @Mock
    private DocumentContentExtractor contentExtractor;

    @InjectMocks
    private DocumentService documentService;

    private User testUser;
    private Document testDocument;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        testDocument = new Document();
        testDocument.setId(1L);
        testDocument.setTitle("Test Document");
        testDocument.setFileName("test.pdf");
        testDocument.setFileType("PDF");
        testDocument.setFilePath("uploads/user_1/test.pdf");
        testDocument.setFileSize(1024L);
        testDocument.setUser(testUser);
        testDocument.setProcessingStatus(Document.ProcessingStatus.PENDING);
    }

    @Test
    void testGetUserDocuments() {
        List<Document> documents = Arrays.asList(testDocument);
        when(documentRepository.findByUserOrderByCreatedAtDesc(testUser)).thenReturn(documents);

        List<Document> result = documentService.getUserDocuments(testUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testDocument.getId(), result.get(0).getId());
        verify(documentRepository, times(1)).findByUserOrderByCreatedAtDesc(testUser);
    }

    @Test
    void testGetUserDocumentsWithPagination() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Document> documentPage = new PageImpl<>(Arrays.asList(testDocument), pageable, 1);
        when(documentRepository.findByUserOrderByCreatedAtDesc(testUser, pageable)).thenReturn(documentPage);

        Page<Document> result = documentService.getUserDocuments(testUser, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        verify(documentRepository, times(1)).findByUserOrderByCreatedAtDesc(testUser, pageable);
    }

    @Test
    void testGetDocumentById_Found() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

        Optional<Document> result = documentService.getDocumentById(1L);

        assertTrue(result.isPresent());
        assertEquals(testDocument.getId(), result.get().getId());
        assertEquals(testDocument.getTitle(), result.get().getTitle());
        verify(documentRepository, times(1)).findById(1L);
    }

    @Test
    void testGetDocumentById_NotFound() {
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Document> result = documentService.getDocumentById(999L);

        assertFalse(result.isPresent());
        verify(documentRepository, times(1)).findById(999L);
    }

    @Test
    void testSearchDocuments() {
        String searchTerm = "test";
        List<Document> documents = Arrays.asList(testDocument);
        when(documentRepository.findByUserAndSearchTerm(testUser, searchTerm)).thenReturn(documents);

        List<Document> result = documentService.searchDocuments(testUser, searchTerm);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(documentRepository, times(1)).findByUserAndSearchTerm(testUser, searchTerm);
    }

    @Test
    void testGetDocumentsByFileType() {
        String fileType = "PDF";
        List<Document> documents = Arrays.asList(testDocument);
        when(documentRepository.findByUserAndFileType(testUser, fileType)).thenReturn(documents);

        List<Document> result = documentService.getDocumentsByFileType(testUser, fileType);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(fileType, result.get(0).getFileType());
        verify(documentRepository, times(1)).findByUserAndFileType(testUser, fileType);
    }

    @Test
    void testGetDocumentCount() {
        when(documentRepository.countByUser(testUser)).thenReturn(5L);

        long count = documentService.getDocumentCount(testUser);

        assertEquals(5L, count);
        verify(documentRepository, times(1)).countByUser(testUser);
    }

    @Test
    void testGetProcessedDocumentCount() {
        when(documentRepository.countByUserAndProcessingStatus(testUser, Document.ProcessingStatus.COMPLETED))
                .thenReturn(3L);

        long count = documentService.getProcessedDocumentCount(testUser);

        assertEquals(3L, count);
        verify(documentRepository, times(1))
                .countByUserAndProcessingStatus(testUser, Document.ProcessingStatus.COMPLETED);
    }

    @Test
    void testDeleteDocument_Success() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(fileStorageService.fileExists(anyString())).thenReturn(true);
        when(fileStorageService.deleteFile(anyString())).thenReturn(true);
        doNothing().when(documentRepository).delete(any(Document.class));

        documentService.deleteDocument(1L, testUser);

        verify(documentRepository, times(1)).findById(1L);
        verify(fileStorageService, times(1)).deleteFile(anyString());
        verify(documentRepository, times(1)).delete(testDocument);
    }

    @Test
    void testDeleteDocument_NotFound() {
        when(documentRepository.findById(1L)).thenReturn(Optional.empty());

        documentService.deleteDocument(1L, testUser);

        verify(documentRepository, times(1)).findById(1L);
        verify(fileStorageService, never()).deleteFile(anyString());
        verify(documentRepository, never()).delete(any(Document.class));
    }

    @Test
    void testDeleteDocument_WrongUser() {
        User differentUser = new User();
        differentUser.setId(2L);
        testDocument.setUser(differentUser);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

        documentService.deleteDocument(1L, testUser);

        verify(documentRepository, times(1)).findById(1L);
        verify(fileStorageService, never()).deleteFile(anyString());
        verify(documentRepository, never()).delete(any(Document.class));
    }
}

