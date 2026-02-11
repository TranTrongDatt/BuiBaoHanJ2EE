package fit.hutech.BuiBaoHan.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when file upload fails
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class FileUploadException extends RuntimeException {
    
    private final String fileName;
    private final FileUploadError errorType;

    public enum FileUploadError {
        FILE_TOO_LARGE,
        INVALID_FILE_TYPE,
        EMPTY_FILE,
        STORAGE_ERROR,
        VIRUS_DETECTED,
        FILENAME_INVALID
    }

    public FileUploadException(String fileName, FileUploadError errorType) {
        super(String.format("File upload failed for '%s': %s", fileName, getErrorMessage(errorType)));
        this.fileName = fileName;
        this.errorType = errorType;
    }

    public FileUploadException(String fileName, FileUploadError errorType, String details) {
        super(String.format("File upload failed for '%s': %s - %s", 
                fileName, getErrorMessage(errorType), details));
        this.fileName = fileName;
        this.errorType = errorType;
    }

    public FileUploadException(String message) {
        super(message);
        this.fileName = null;
        this.errorType = null;
    }

    private static String getErrorMessage(FileUploadError errorType) {
        return switch (errorType) {
            case FILE_TOO_LARGE -> "File exceeds maximum allowed size";
            case INVALID_FILE_TYPE -> "File type is not allowed";
            case EMPTY_FILE -> "File is empty";
            case STORAGE_ERROR -> "Failed to store file";
            case VIRUS_DETECTED -> "Malware detected in file";
            case FILENAME_INVALID -> "Invalid file name";
        };
    }

    public String getFileName() {
        return fileName;
    }

    public FileUploadError getErrorType() {
        return errorType;
    }
}
