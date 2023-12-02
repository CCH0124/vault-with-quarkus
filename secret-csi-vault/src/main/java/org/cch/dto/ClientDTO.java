package org.cch.dto;

import org.jboss.resteasy.reactive.multipart.FileUpload;

public record ClientDTO(FileUpload clientCertificate) {
    
}
