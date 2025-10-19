# Local File Storage System

This document describes the local file storage system implemented to simulate S3 bucket functionality for document storage.

## Overview

The system provides local file storage capabilities for uploaded documents, replacing the previous mock storage approach. Files are stored in a configurable local directory with user-specific subdirectories.

## Configuration

### Application Properties

```properties
# Enable/disable local file storage
document.storage.local.enabled=true

# Base path for file storage (relative to application root)
document.storage.local.base-path=./uploads

# Maximum file size allowed
document.storage.local.max-file-size=10MB

# Allowed file extensions
document.storage.local.allowed-extensions=pdf,doc,docx,txt,ppt,pptx

# File cleanup configuration
file.cleanup.enabled=true
file.cleanup.retention-days=30
```

## Components

### 1. LocalFileStorageService

**Purpose**: Core service for file operations (store, retrieve, delete, validate)

**Key Features**:
- File validation (size, extension, content type)
- User-specific directory structure (`uploads/user_{userId}/`)
- Unique filename generation with timestamps
- Content type detection
- File existence checking

**Directory Structure**:
```
uploads/
├── .gitkeep
└── user_1/
    ├── 20241201_143022_a1b2c3d4.pdf
    ├── 20241201_143045_e5f6g7h8.docx
    └── ...
```

### 2. DocumentContentExtractor

**Purpose**: Extract text content from uploaded files

**Current Implementation**:
- Basic text file extraction
- Placeholder content generation for binary files (PDF, DOCX, etc.)
- Ready for integration with Apache Tika for production use

**Supported File Types**:
- `.txt` - Direct text extraction
- `.pdf`, `.doc`, `.docx`, `.ppt`, `.pptx` - Placeholder content (ready for Tika integration)

### 3. FileCleanupService

**Purpose**: Automated file cleanup and storage management

**Features**:
- Scheduled cleanup of old files (configurable retention period)
- Empty directory removal
- Storage statistics
- Memory-efficient file processing

**Scheduled Tasks**:
- File cleanup: Daily at 2:00 AM
- Directory cleanup: Daily at 2:30 AM

### 4. DocumentService Updates

**Changes**:
- Integrated with LocalFileStorageService
- Real file storage instead of mock paths
- Content extraction from actual files
- File deletion with storage cleanup

## API Endpoints

### New Endpoints

#### Download Document
```
GET /api/documents/{id}/download
```
Downloads the original file as an attachment.

#### View Document
```
GET /api/documents/{id}/view
```
Serves the file for inline viewing in the browser.

#### Storage Statistics
```
GET /api/documents/storage/stats
```
Returns storage usage statistics and document counts.

### Updated Endpoints

#### Upload Document
- Now stores files in local storage
- Generates unique filenames
- Validates file types and sizes

#### Delete Document
- Removes file from local storage
- Cleans up empty directories

## File Management

### File Naming Convention
```
{timestamp}_{uuid}.{extension}
```
Example: `20241201_143022_a1b2c3d4.pdf`

### User Isolation
Each user's files are stored in separate directories (`user_{userId}/`) to ensure proper access control and organization.

### Content Processing
1. File is uploaded and stored locally
2. Content is extracted (real or simulated)
3. AI processes the content for summary generation
4. Document metadata is saved to database

## Security Considerations

### Access Control
- Files are only accessible to their owners
- Authentication required for all file operations
- User-specific directory isolation

### File Validation
- File size limits enforced
- Allowed extensions whitelist
- Content type validation

### Path Security
- No directory traversal attacks possible
- User-specific paths prevent cross-user access

## Monitoring and Maintenance

### Storage Statistics
The system provides detailed statistics including:
- Total file count
- Directory count
- Total storage usage
- Formatted size display

### Automated Cleanup
- Old files are automatically removed after the retention period
- Empty directories are cleaned up
- Configurable retention policies

### Logging
Comprehensive logging for:
- File operations (upload, download, delete)
- Error conditions
- Cleanup operations
- Storage statistics

## Production Considerations

### Apache Tika Integration
For production use, integrate Apache Tika for proper content extraction:

```xml
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
    <version>2.9.1</version>
</dependency>
```

### File Size Limits
Adjust `spring.servlet.multipart.max-file-size` and `document.storage.local.max-file-size` based on requirements.

### Storage Monitoring
Consider implementing:
- Disk space monitoring
- File access logging
- Performance metrics
- Backup strategies

## Migration from Mock Storage

The system is designed to be backward compatible:
- Existing mock files continue to work
- New uploads use local storage
- Gradual migration as files are re-uploaded

## Testing

### Local Development
1. Start the application
2. Upload a test document
3. Verify file is stored in `uploads/user_{userId}/`
4. Test download and view endpoints
5. Check storage statistics

### File Operations
- Upload various file types
- Test file size limits
- Verify content extraction
- Test deletion and cleanup

## Troubleshooting

### Common Issues

1. **File Upload Fails**
   - Check file size limits
   - Verify file extension is allowed
   - Ensure uploads directory exists

2. **Download Not Working**
   - Verify file exists in storage
   - Check user permissions
   - Review file path in database

3. **Content Extraction Issues**
   - Check file format support
   - Review content extractor logs
   - Verify file is not corrupted

### Debug Information
Enable debug logging for detailed file operation information:
```properties
logging.level.com.example.springbootjava.service=DEBUG
```
